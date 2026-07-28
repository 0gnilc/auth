# Use the Vben monorepo for admin and server

Use Vue Vben Admin's pnpm and Turborepo structure as the repository shell, with `apps/admin` for the Element Plus administration application and `apps/server` for the Maven reactor. Expose server tasks through its package scripts so the two ecosystems remain independently usable while root tooling can schedule both.
