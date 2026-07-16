---
status: accepted
---

# Use admin as the admin access baseline role

Every admin user must retain the built-in `admin` role. Permissions for reading the current admin user's profile, roles, and button access codes and for updating that user's basic information and password are assigned through the normal RBAC role-permission relationship for `admin`, rather than through a system-specific granted-permissions provider; other roles add more specific business permissions on top of this baseline.
