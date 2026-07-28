# Explicitly compose shared infrastructure configuration

Keep reusable Spring infrastructure components in the inert `gnilc-common-config` module and require runtime modules to import only the components they need. Do not activate the module through aggregate configuration or Spring Boot auto-configuration; explicit composition keeps consumers independently runnable and replaceable.
