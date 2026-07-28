# Serialize pagination metadata as numbers

Continue serializing Java `Long` identifiers as JSON strings for JavaScript safety, but serialize the shared `PageResult` fields `currentPage`, `pageSize`, `totalCount`, and `totalPage` as JSON numbers. This gives every paginated API a numeric pagination contract without frontend rewriting or a broad primitive-type exception.
