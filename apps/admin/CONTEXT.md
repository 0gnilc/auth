# Admin System

Admin manages administrator identities, role assignments, navigation and dynamic internationalization.

## Identity And Access

**Admin User**: An administrator identity with its own session and authorization subject.

**Current Admin User**: The administrator represented by the active session; self-service targets this identity rather than a selected account.

**Admin User Administration**: Management of another administrator's profile and role assignments, distinct from self-service; the current operator cannot disable or delete their own account.

**Default Admin Baseline**: The recoverable bootstrap administrator and mandatory access bindings. Reinitialization preserves existing credentials and profile data.

**Admin Access Baseline Role**: The mandatory `admin` role for shell and self-service access, without ordinary business management.

**Administration Module**: A complete resource or workflow that forms a useful unit of operator responsibility.

**Management Role**: An independently assignable set of access for a module or an established separation of duties; it describes a capability rather than a job or person.

**Custom Role**: An operator-owned access combination for an environment-specific need.

**Built-in RBAC Resource**: A system-maintained Role, Permission, or Menu whose protected identity is independent of its assignments. The [role catalog](../../docs/role/role-design.md) explains the current access model.

## Navigation And Messages

**Current Admin Navigation Route Tree**: The enabled, reachable menus available to the current administrator, including ancestors needed to reach usable pages.

**Menu Authorization Closure**: Granted menus plus their required ancestors; disabled menus can remain granted while absent from navigation.

**Button Menu**: A frontend action whose access code controls visibility independently of backend API enforcement.

**Message Key**: The globally unique identity of a dynamic message across locales and categories.

**I18n Message Category**: The mutable grouping used for message administration and runtime bundles, independent of message identity. Dynamic messages provide optional display text to business resources; [ADR-0010](../../docs/adr/0010-keep-dynamic-i18n-auxiliary-to-business-resources.md) explains their separate lifecycle.
