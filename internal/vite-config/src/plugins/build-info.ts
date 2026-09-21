import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { join } from 'node:path';

/** 构建标识与产品版本分离；发布构建必须提供可追溯的完整提交。 */
function resolveBuildInfo(
  version: string,
  root: string,
  environment = process.env,
) {
  const channel = environment.GNILC_BUILD_CHANNEL ?? 'development';
  if (!['development', 'release'].includes(channel)) {
    throw new Error(`Unknown build channel: ${channel}`);
  }
  let revision = environment.GNILC_BUILD_REVISION;
  if (!revision && existsSync(join(root, '.git'))) {
    revision = execFileSync('git', ['rev-parse', 'HEAD'], {
      cwd: root,
      encoding: 'utf8',
    }).trim();
  }
  if (channel === 'release' && !/^[a-f\d]{40}$/u.test(revision ?? '')) {
    throw new Error(
      'Release builds require GNILC_BUILD_REVISION with a full commit SHA',
    );
  }
  const epoch = environment.SOURCE_DATE_EPOCH;
  if (epoch !== undefined && !/^\d+$/u.test(epoch))
    throw new Error('Invalid SOURCE_DATE_EPOCH');
  const buildTime = new Date(
    epoch === undefined ? Date.now() : Number(epoch) * 1000,
  ).toISOString();
  return { buildTime, channel, revision: revision ?? 'local', version };
}

export { resolveBuildInfo };
