import os
import json
import asyncio
import logging
# pyrefly: ignore [missing-import]
import httpx
# pyrefly: ignore [missing-import]
import redis.asyncio as aioredis
from app.models import EmailPayload, ExtractionResult
from app.llm_extractor import LLMExtractor

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("processing-worker")

REDIS_URL = os.getenv("SPRING_DATA_REDIS_URL") or os.getenv("REDIS_URL", "redis://localhost:6379")
STREAM_KEY = "placementos:events:stream"
CONSUMER_GROUP = "placementos-python-group"
CONSUMER_NAME = "python-worker-1"
SPRING_BOOT_URL = os.getenv("SPRING_BOOT_URL", "http://localhost:8080")

extractor = LLMExtractor()

async def init_stream_group(r: aioredis.Redis):
    try:
        await r.xgroup_create(STREAM_KEY, CONSUMER_GROUP, id="0", mkstream=True)
        log.info("Created consumer group %s on stream %s", CONSUMER_GROUP, STREAM_KEY)
    except aioredis.ResponseError as e:
        if "BUSYGROUP" in str(e):
            log.info("Consumer group %s already exists", CONSUMER_GROUP)
        else:
            raise

async def process_record(r: aioredis.Redis, record_id: str, data: dict, http_client: httpx.AsyncClient):
    event_type = data.get("eventType") or data.get(b"eventType", b"").decode("utf-8", errors="ignore")
    if event_type != "GMAIL_MESSAGE_RETRIEVED":
        # Ignore other event types and acknowledge
        await r.xack(STREAM_KEY, CONSUMER_GROUP, record_id)
        return

    raw_payload = data.get("payload") or data.get(b"payload", b"").decode("utf-8", errors="ignore")
    try:
        payload_dict = json.loads(raw_payload) if isinstance(raw_payload, str) else raw_payload
    except Exception:
        log.warning("Invalid JSON payload in record %s", record_id)
        await r.xack(STREAM_KEY, CONSUMER_GROUP, record_id)
        return

    message_id = payload_dict.get("messageId")
    source_email = payload_dict.get("sourceEmail")
    log.info("Processing message %s from source %s", message_id, source_email)

    # Fetch full message details from Spring Boot if needed
    plain_text_body = payload_dict.get("plainTextBody")
    subject = payload_dict.get("subject", "")
    sender = payload_dict.get("sender", "")

    if not plain_text_body:
        try:
            resp = await http_client.get(f"{SPRING_BOOT_URL}/api/v1/internal/messages/{message_id}")
            if resp.status_code == 200:
                msg_data = resp.json()
                plain_text_body = msg_data.get("plainTextBody", "")
                subject = msg_data.get("subject", subject)
                sender = msg_data.get("sender", sender)
        except Exception as e:
            log.error("Failed to fetch message %s details from Spring Boot: %s", message_id, e)

    email_payload = EmailPayload(
        message_id=message_id,
        source_email=source_email,
        subject=subject,
        sender=sender,
        plain_text_body=plain_text_body
    )

    # Perform classification and extraction
    result: ExtractionResult = extractor.extract_with_fallback(email_payload)

    # Post result back to Spring Boot
    try:
        post_resp = await http_client.post(
            f"{SPRING_BOOT_URL}/api/v1/internal/extraction/result",
            json=result.model_dump(by_alias=True)
        )
        if post_resp.status_code == 200:
            log.info("Successfully submitted extraction result for message %s to Spring Boot", message_id)
            await r.xack(STREAM_KEY, CONSUMER_GROUP, record_id)
        else:
            log.error("Spring Boot rejected extraction result for %s: HTTP %s", message_id, post_resp.status_code)
    except Exception as e:
        log.error("Failed to post extraction result for %s to Spring Boot: %s", message_id, e)

async def run_worker():
    log.info("Starting PlacementOS Python Processing Worker...")
    r = aioredis.from_url(REDIS_URL, decode_responses=True)
    await init_stream_group(r)

    async with httpx.AsyncClient(timeout=10.0) as http_client:
        while True:
            try:
                records = await r.xreadgroup(
                    CONSUMER_GROUP,
                    CONSUMER_NAME,
                    {STREAM_KEY: ">"},
                    count=10,
                    block=2000
                )
                if records:
                    for stream, messages in records:
                        for record_id, data in messages:
                            await process_record(r, record_id, data, http_client)
            except Exception as e:
                log.error("Error reading from Redis Stream: %s", e)
                await asyncio.sleep(1)

if __name__ == "__main__":
    asyncio.run(run_worker())
