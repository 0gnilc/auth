# Gnilc Auth

Gnilc Auth establishes administrator identities and controls their access to administration resources.

## Language

**Admin User**: An administrator identity with credentials, a profile and sessions. Its profile identity differs from its authorization subject.

**Authorization Subject**: The global user identity to which roles are assigned.

**Admin Session**: A signed-in administrator's access and refresh credentials with independent expiration and revocation.

**Authentication**: Establishing the identity represented by a request's credentials.

**Authorization**: Deciding whether an identity may access a protected resource.

**Admin Baseline Role**: The mandatory `admin` role providing navigation and self-service access without management privileges.

**Management Role**: An independently assignable set of administrator-management or internationalization capabilities.

**Built-in Resource**: A system-maintained role, permission or menu whose protected definition is distinct from its assignments.

**Permission**: A rule governing access to an API resource. A public permission can allow anonymous access.

**Menu**: A navigation resource or a UI action; visibility does not replace API authorization.

**Message Key**: The global identity of a localized dynamic message, independent of its category and language.
