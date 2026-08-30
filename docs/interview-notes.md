# Interview Notes

## Example

Problem:
Duplicate Gmail notifications.

Solution:
Used Gmail Message-ID as an idempotency key.

Trade-off:
Requires storing processed message IDs.

---

Problem:
Why deterministic parsing first?

Solution:
Most CDC emails follow predictable formats.

Trade-off:
AI handles edge cases.