# Roadmap

## V0 (Completed)
- Gmail Watch API integration & History API Synchronization
- Upstash Redis Streams asynchronous event foundation
- Placement email classification & structured extraction (deterministic + LLM fallback)
- Multi-role and common eligibility ingestion

## V1 (Completed)
- Student profiles & database persistence
- Explainable student-level eligibility evaluation engine (multi-role & common criteria)
- Attachment binary acquisition & storage abstraction (`AttachmentStorage`)
- Document processing & Excel/PDF/DOCX candidate parsing
- Deterministic candidate-to-student matching & shortlist persistence
- Application state reconciliation (non-regression lifecycle)
- Notification decision engine with deterministic idempotency keys
- Transactional Outbox pattern & asynchronous delivery worker
- WhatsApp provider abstraction & `MockWhatsAppNotificationProvider`
- Reminder lifecycle with recurring scheduling & stop-on-apply/done semantics
- Principal-derived student identity resolution (anti-impersonation boundary)

## V2 (Next)
- Real WhatsApp Cloud API / Webhook provider integration
- Student web dashboard & Next.js frontend
- Admin moderation console for ambiguous shortlist matches & criteria reviews

## V3
- Multi-campus support
- Historical placement analytics & trends
- Automated resume matching and OCR ingestion