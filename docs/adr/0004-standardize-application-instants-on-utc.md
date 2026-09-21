# Standardize application instants on UTC

Represent timeline points as Java `Instant`, RFC 3339 UTC JSON values, and UTC literals in MySQL `DATETIME(6)` columns; enforce UTC on database connections and convert only for frontend display. Admin time-query inputs are interpreted in the currently selected display time zone, converted to UTC instants, and use inclusive start and end bounds. Calendar values and regional schedules retain their local semantics, and audit timestamps are not treated as unique ordering or concurrency tokens.
