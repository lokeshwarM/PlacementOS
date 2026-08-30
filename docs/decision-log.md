# Engineering Decision Log

## D-001

### Decision

Use a shared CDC email source.

### Why

Every student receives the same placement email.

Parsing once is dramatically cheaper.

### Trade-off

Requires trusted inboxes.

---

## D-002

### Decision

Use deterministic parsing first.

### Why

CDC emails are structured.

Regex is cheaper than AI.

### Trade-off

AI fallback is needed for unusual emails.