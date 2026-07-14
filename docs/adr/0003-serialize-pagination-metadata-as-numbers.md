---
status: accepted
---

# Serialize pagination metadata as numbers

Keep the global Jackson serialization of Java `Long` and `long` values as JSON strings so identifiers remain safe for JavaScript clients. Override that serializer on the shared `PageResult` pagination fields so `currentPage`, `pageSize`, `totalCount`, and `totalPage` are emitted as JSON numbers; this gives every paginated API a numeric pagination contract without frontend response rewriting or a broad exception based on primitive types.
