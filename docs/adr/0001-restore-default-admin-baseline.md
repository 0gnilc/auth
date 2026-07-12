---
status: accepted
---

# Restore the default admin baseline

Repeated execution of the manual initialization SQL restores the default admin's active system identity, its RBAC subject, the built-in admin role, and the required role binding when any of them is missing or logically deleted. It preserves mutable administrator data such as the password, nickname, avatar, description, and home path, choosing a recoverable bootstrap identity over a strictly insert-only seed without overwriting operator-managed profile data.
