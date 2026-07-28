# Resolve client-facing backend errors at source

Internationalize client-correctable request and business errors at the owning backend source, using that module's classpath bundles; keep programmer contracts and internal invariant diagnostics developer-facing. Preserve transport status and response shape, never expose caught internal exception messages, and use `en-US` when no supported locale is requested.
