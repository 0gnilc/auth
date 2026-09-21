# Release Admin and Server as one product

Admin, Server and internal workspace modules use one product version, owned by the root package manifest and synchronized by repository tooling. These modules are developed and verified together; independent package releases would require compatibility matrices and public package contracts that this application does not currently have.

A published version is an immutable tag and a verified set of artifacts from one commit. Product version, build identity and persistent storage schema are separate contracts: ordinary product upgrades must not reset compatible browser state, and a matching version string alone does not establish artifact provenance. Database migrations are ordered and immutable after publication; unknown historical installations require an explicit upgrade plan.
