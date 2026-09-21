# Separate baseline access from management roles

Every Admin User retains the built-in `admin` role for shell and self-service access. Independently assignable Management Roles add business capabilities through the existing Permission/Menu union; removing the baseline binding is rejected.

The catalog initializer maintains built-in grants, while ordinary role assignments and Custom Roles remain operator-owned. Fresh empty-database provisioning assigns the Default Admin the RBAC and internationalization management roles once. Historical bindings prevent later initialization from restoring a role an operator removed.

This keeps sign-in access separate from management authority and makes initialization repeatable without overriding operator choices.
