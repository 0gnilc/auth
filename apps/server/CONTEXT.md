# Authentication And Authorization

The Server establishes administrator identities and enforces protected access.

**Access Identity**: The authenticated subject represented by the current request.

**Authentication Failure**: Credentials were supplied but could not establish a valid identity, including an expired or revoked session.

**Access Denial**: The current identity, including anonymous access, is not permitted to access the requested resource.

**Required Role**: A baseline role that an existing administrator identity must retain regardless of whether its account is enabled.

**Public Access Permission**: A permission available without a role binding, including anonymous access; it does not grant navigation visibility.

**Client Business String**: A submitted value whose exact representation matters, including null, empty, whitespace and case distinctions.
