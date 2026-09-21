# Admin Development

Admin uses Vben and Element Plus. Prefer the existing form, Drawer, Grid, and request capabilities when working in this UI; `#/adapter/form` is the form integration entry.

- Keep page-owned Vue components in the feature's `components/` directory; place shared components under their nearest common business directory. Keep query, validation, and type files named by responsibility outside component directories.
- Navigation comes from the Server and resolves components through the existing `views/**/*.vue` page map. Menu access codes control visibility; API Permissions enforce access.
- Organize APIs by business capability under `src/api`; callers import from the owning module entry, such as `#/api/core` or `#/api/system`. Keep DTOs and requests together, and contract tests beside their module.
- `src/api/core` owns runtime/session requests; `src/api/system` owns administrator, access, and dynamic-message administration. The shared transport and pagination contracts remain in `src/api/request.ts` and `src/api/types.ts`.
- The shared request client owns common error presentation and locale handling.
- [Role Design](../../docs/role/role-design.md) describes the current role catalog and where its grants are maintained.
