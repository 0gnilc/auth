import assert from 'node:assert/strict';
import test from 'node:test';

import {
  getTaskInvocations,
  resolveMavenExecutable,
  runTask,
} from '../../../scripts/maven.mjs';

test('uses the Maven wrapper for Unix-like platforms when present', () => {
  const executable = resolveMavenExecutable({
    fileExists: (path) => path.endsWith('/mvnw'),
    platform: 'linux',
    serverDir: '/repo',
  });

  assert.deepEqual(executable, {
    command: '/repo/mvnw',
    shell: false,
    source: 'wrapper',
  });
});

test('uses the Windows Maven wrapper through the command shell when present', () => {
  const executable = resolveMavenExecutable({
    fileExists: (path) => path.endsWith('/mvnw.cmd'),
    platform: 'win32',
    serverDir: '/repo',
  });

  assert.deepEqual(executable, {
    command: '/repo/mvnw.cmd',
    shell: true,
    source: 'wrapper',
  });
});

test('falls back to the platform Maven command when no wrapper exists', () => {
  assert.equal(
    resolveMavenExecutable({
      fileExists: () => false,
      platform: 'darwin',
      serverDir: '/repo',
    }).command,
    'mvn',
  );
  assert.equal(
    resolveMavenExecutable({
      fileExists: () => false,
      platform: 'win32',
      serverDir: '/repo',
    }).command,
    'mvn.cmd',
  );
});

test('maps server tasks to stable Maven invocations', () => {
  assert.deepEqual(getTaskInvocations('build'), [
    ['--batch-mode', '--no-transfer-progress', '-DskipTests', 'package'],
  ]);
  assert.deepEqual(getTaskInvocations('dev'), [
    ['--batch-mode', '--no-transfer-progress', '-DskipTests', 'package'],
    [
      '--batch-mode',
      '--no-transfer-progress',
      '-pl',
      'gnilc-bootstrap',
      'spring-boot:run',
    ],
  ]);
});

test('stops a multi-step task after the first failed Maven invocation', () => {
  const calls = [];
  const status = runTask('dev', {
    executable: { command: 'mvn', shell: false, source: 'system' },
    spawn: (command, args) => {
      calls.push([command, args]);
      return { status: 7 };
    },
  });

  assert.equal(status, 7);
  assert.equal(calls.length, 1);
});
