// cspell:ignore Dfailsafe Dsurefire
import { readFileSync } from 'node:fs';

import { defineConfig, devices } from 'playwright/test';

import { parsePort } from './scripts/port.mjs';

function readEnvPort(path: string, name: string) {
  const value = readFileSync(path, 'utf8')
    .split(/\r?\n/u)
    .find((line) => line.startsWith(`${name}=`))
    ?.slice(name.length + 1);
  if (!value || !/^\d+$/u.test(value)) {
    throw new Error(`${name} must be configured in ${path}.`);
  }
  return String(parsePort(value, `${name} in ${path}`));
}

function readBackendPort() {
  const application = readFileSync(
    'apps/server/gnilc-bootstrap/src/main/resources/application.yml',
    'utf8',
  );
  const value = /^server:\s*\n\s+port:\s*(\d+)\s*$/mu.exec(application)?.[1];
  if (!value) throw new Error('server.port must be configured.');
  return String(parsePort(value, 'server.port'));
}

function readRuntimePort(name: string, fallback: string) {
  const value = process.env[name];
  if (value === undefined) return fallback;
  return String(parsePort(value, name));
}

const backendPort = readRuntimePort('E2E_SERVER_PORT', readBackendPort());
const adminPort = readRuntimePort(
  'E2E_ADMIN_PORT',
  readEnvPort('apps/admin/.env.development', 'VITE_PORT'),
);
const backendUrl = `http://127.0.0.1:${backendPort}`;
const adminUrl = `http://127.0.0.1:${adminPort}`;

export default defineConfig({
  expect: { timeout: 10_000 },
  forbidOnly: !!process.env.CI,
  fullyParallel: false,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  retries: process.env.CI ? 1 : 0,
  testDir: './tests/e2e',
  timeout: 45_000,
  projects: [
    {
      name: 'admin',
      testMatch: 'admin-smoke.spec.ts',
      use: { ...devices['Desktop Chrome'], baseURL: adminUrl },
    },
  ],
  use: {
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },
  webServer: [
    {
      command: 'node scripts/e2e-service.mjs server',
      reuseExistingServer: false,
      timeout: 180_000,
      url: `${backendUrl}/api/sys/admin/user-info`,
    },
    {
      command: 'node scripts/e2e-service.mjs admin',
      reuseExistingServer: false,
      timeout: 120_000,
      url: adminUrl,
    },
  ],
  workers: 1,
});
