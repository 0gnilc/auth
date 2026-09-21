import { describe, expect, it } from 'vitest';

import { evaluateCiGate, renderGateSummary } from './ci-gate.mjs';

function classificationOutputs(overrides = {}) {
  return {
    admin: 'false',
    conservative: 'false',
    docs_only: 'true',
    e2e: 'false',
    reason: 'classified',
    server: 'false',
    server_full: 'false',
    tooling: 'false',
    ...overrides,
  };
}

function dependenciesFor(outputs = classificationOutputs(), overrides = {}) {
  return {
    admin: { result: outputs.admin === 'true' ? 'success' : 'skipped' },
    changes: { outputs, result: 'success' },
    docs: { result: outputs.docs_only === 'true' ? 'success' : 'skipped' },
    e2e: { result: outputs.e2e === 'true' ? 'success' : 'skipped' },
    server: { result: outputs.server === 'true' ? 'success' : 'skipped' },
    workspace: {
      result: outputs.tooling === 'true' ? 'success' : 'skipped',
    },
    ...overrides,
  };
}

describe('ci gate', () => {
  it('accepts successful and intentionally skipped jobs', () => {
    expect(evaluateCiGate(dependenciesFor())).toEqual({
      failures: [],
      ok: true,
    });
  });

  it.each(['failure', 'cancelled', 'timed_out', undefined])(
    'rejects a dependency result of %s',
    (result) => {
      expect(
        evaluateCiGate(
          dependenciesFor(classificationOutputs(), {
            server: result === undefined ? {} : { result },
          }),
        ),
      ).toEqual({
        failures: [{ job: 'server', result: result ?? 'missing' }],
        ok: false,
      });
    },
  );

  it('reports selected scopes beside dependency results', () => {
    const needs = dependenciesFor(
      classificationOutputs({ docs_only: 'false', server: 'true' }),
    );

    const summary = renderGateSummary(needs, evaluateCiGate(needs));

    expect(summary).toContain('| server | true |');
    expect(summary).toContain('| server_full | false |');
    expect(summary).toContain('| server | success |');
    expect(summary).toContain('**Gate:** PASS');
  });

  it('rejects missing classification outputs before skipped jobs can look successful', () => {
    const outputs = classificationOutputs();
    delete outputs.server;

    expect(evaluateCiGate(dependenciesFor(outputs))).toEqual({
      failures: [{ job: 'changes.outputs', result: 'missing server' }],
      ok: false,
    });
  });

  it('rejects a full Server mode that does not select the Server job', () => {
    expect(
      evaluateCiGate(
        dependenciesFor(
          classificationOutputs({
            docs_only: 'false',
            server_full: 'true',
          }),
        ),
      ),
    ).toEqual({
      failures: [
        {
          job: 'changes.outputs',
          result: 'server_full requires server',
        },
      ],
      ok: false,
    });
  });

  it('rejects a conservative fallback that does not select every application scope', () => {
    expect(
      evaluateCiGate(
        dependenciesFor(
          classificationOutputs({
            conservative: 'true',
            docs_only: 'false',
          }),
        ),
      ),
    ).toEqual({
      failures: [
        {
          job: 'changes.outputs',
          result: 'conservative fallback requires all application scopes',
        },
      ],
      ok: false,
    });
  });

  it('rejects a selected job that was skipped', () => {
    const outputs = classificationOutputs({
      docs_only: 'false',
      server: 'true',
    });

    expect(
      evaluateCiGate(
        dependenciesFor(outputs, { server: { result: 'skipped' } }),
      ),
    ).toEqual({
      failures: [{ job: 'server', result: 'selected but skipped' }],
      ok: false,
    });
  });

  it('rejects a missing declared dependency', () => {
    const needs = dependenciesFor();
    delete needs.workspace;

    expect(evaluateCiGate(needs)).toEqual({
      failures: [{ job: 'workspace', result: 'missing dependency' }],
      ok: false,
    });
  });
});
