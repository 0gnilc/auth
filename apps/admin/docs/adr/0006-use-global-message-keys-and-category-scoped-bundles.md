# Use global Message Keys and category-scoped bundles

Identify each dynamic message globally by Message Key; use a required, mutable category only for organization and runtime bundle scope. Store one row per locale with uniqueness on `(message_key, locale)`, require all locale rows for a key to share a category, and reject ancestor or descendant conflicts between keys.
