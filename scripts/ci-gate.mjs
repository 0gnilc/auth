import { appendFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ACCEPTED_RESULTS = new Set(['skipped', 'success']);
const REQUIRED_JOBS = [
  'changes',
  'docs',
  'admin',
  'workspace',
  'server',
  'e2e',
];
const SELECTED_JOB_OUTPUTS = {
  admin: 'admin',
  docs: 'docs_only',
  e2e: 'e2e',
  server: 'server',
  workspace: 'tooling',
};
const REQUIRED_BOOLEAN_OUTPUTS = [
  'admin',
  'conservative',
  'docs_only',
  'e2e',
  'server',
  'server_full',
  'tooling',
];

function validateClassificationOutputs(outputs = {}) {
  const failures = [];
  for (const name of REQUIRED_BOOLEAN_OUTPUTS) {
    if (outputs[name] === undefined) {
      failures.push({ job: 'changes.outputs', result: `missing ${name}` });
    } else if (!['false', 'true'].includes(outputs[name])) {
      failures.push({
        job: 'changes.outputs',
        result: `invalid ${name}: ${outputs[name]}`,
      });
    }
  }
  if (!outputs.reason) {
    failures.push({ job: 'changes.outputs', result: 'missing reason' });
  }
  if (outputs.server_full === 'true' && outputs.server !== 'true') {
    failures.push({
      job: 'changes.outputs',
      result: 'server_full requires server',
    });
  }
  const conservativeScopes = [
    'admin',
    'e2e',
    'server',
    'server_full',
    'tooling',
  ];
  if (
    outputs.conservative === 'true' &&
    (outputs.docs_only !== 'false' ||
      conservativeScopes.some((name) => outputs[name] !== 'true'))
  ) {
    failures.push({
      job: 'changes.outputs',
      result: 'conservative fallback requires all application scopes',
    });
  }
  return failures;
}

function evaluateCiGate(needs) {
  const missingDependencies = REQUIRED_JOBS.filter(
    (job) => needs[job] === undefined,
  ).map((job) => ({ job, result: 'missing dependency' }));
  const jobFailures = Object.entries(needs)
    .map(([job, value]) => ({ job, result: value?.result ?? 'missing' }))
    .filter(({ result }) => !ACCEPTED_RESULTS.has(result));
  const outputFailures =
    needs.changes?.result === 'success'
      ? validateClassificationOutputs(needs.changes.outputs)
      : [];
  const selectedJobFailures = Object.entries(SELECTED_JOB_OUTPUTS)
    .filter(
      ([job, output]) =>
        needs.changes?.outputs?.[output] === 'true' &&
        needs[job]?.result === 'skipped',
    )
    .map(([job]) => ({ job, result: 'selected but skipped' }));
  const changesFailure =
    needs.changes?.result === 'skipped'
      ? [{ job: 'changes', result: 'required but skipped' }]
      : [];
  const failures = [
    ...missingDependencies,
    ...jobFailures,
    ...outputFailures,
    ...selectedJobFailures,
    ...changesFailure,
  ];
  return { failures, ok: failures.length === 0 };
}

function renderGateSummary(needs, evaluation) {
  const classifications = Object.entries(needs.changes?.outputs ?? {});
  return [
    '## CI gate',
    '',
    ...(classifications.length > 0
      ? [
          '| Scope | Selected |',
          '| --- | --- |',
          ...classifications.map(
            ([scope, selected]) => `| ${scope} | ${selected} |`,
          ),
          '',
        ]
      : []),
    '| Job | Result |',
    '| --- | --- |',
    ...Object.entries(needs).map(
      ([job, value]) => `| ${job} | ${value?.result ?? 'missing'} |`,
    ),
    '',
    `**Gate:** ${evaluation.ok ? 'PASS' : 'FAIL'}`,
    '',
  ].join('\n');
}

export { evaluateCiGate, renderGateSummary };

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  let needs;
  try {
    needs = JSON.parse(process.env.CI_NEEDS_JSON ?? '');
  } catch (error) {
    console.error(`Could not parse CI_NEEDS_JSON: ${error.message}`);
    process.exitCode = 1;
  }

  if (needs) {
    const evaluation = evaluateCiGate(needs);
    const summary = renderGateSummary(needs, evaluation);
    process.stdout.write(summary);
    if (process.env.GITHUB_STEP_SUMMARY) {
      appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary, 'utf8');
    }
    if (!evaluation.ok) {
      for (const failure of evaluation.failures) {
        console.error(`${failure.job}: ${failure.result}`);
      }
      process.exitCode = 1;
    }
  }
}
