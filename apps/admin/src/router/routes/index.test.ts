import { describe, expect, it } from 'vitest';

import { accessRoutes, coreRouteNames, routes } from './index';

describe('admin route registration', () => {
  it('keeps the workspace redirect in core without exposing dashboard', () => {
    expect(routes).toContainEqual(
      expect.objectContaining({
        name: 'WorkspaceRedirect',
        path: '/workspace',
        redirect: '/dashboard',
      }),
    );
    expect(coreRouteNames).toContain('WorkspaceRedirect');
    expect(coreRouteNames).not.toContain('Dashboard');
    expect(accessRoutes).toContainEqual(
      expect.objectContaining({
        name: 'Dashboard',
        path: '/dashboard',
      }),
    );
    expect(accessRoutes.map((route) => route.name)).not.toContain(
      'WorkspaceRedirect',
    );
  });
});
