---
status: accepted
---

# Validate client business strings without normalization

Admin business strings are validated and persisted as submitted, preserving null, empty, whitespace, and case distinctions. Each field defines its permitted format; a generic trim or case-fold step would silently change identity or user data.
