---
status: accepted
---

# Separate Authentication, Authorization, and Access Denied Handling

Authentication establishes a principal but grants no access, while Authorization determines whether an Access Identity may access an Access Target by adapting an Access Context, resolving Granted and Required Permissions through providers, and applying Permission Checking through `AccessDecision`. Access Denied Handling runs only after a denied result and keeps execution-environment response objects outside the authorization facts and decision. This separation adds explicit adapter, provider, and handler seams in exchange for keeping the reusable authorization core environment-independent and making context preparation, permission resolution, decision strategy, and denied responses independently replaceable and testable.
