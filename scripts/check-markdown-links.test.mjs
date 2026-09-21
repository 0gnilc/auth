import { describe, expect, it, vi } from 'vitest';

import { findBrokenLocalLinks } from './check-markdown-links.mjs';

describe('markdown link checking', () => {
  it('reports missing local targets without treating external links or anchors as files', () => {
    const existing = new Set(
      [
        '/repo/docs/guide.md',
        '/repo/My File.md',
        '/repo/assets/image.png',
      ].toSorted(),
    );
    const exists = vi.fn((path) => existing.has(path));

    expect(
      findBrokenLocalLinks({
        exists,
        filePath: '/repo/docs/index.md',
        markdown: [
          '[Guide](guide.md#section)',
          '[File with spaces](<../My%20File.md>)',
          '![Image](../assets/image.png)',
          '[Section](#section)',
          '[Web](https://example.com/docs)',
          '[Template placeholder](UPSTREAM_COMMIT_URL)',
          '[Missing](missing.md)',
        ].join('\n'),
      }),
    ).toEqual([{ source: '/repo/docs/index.md', target: 'missing.md' }]);
    expect(exists).toHaveBeenCalledTimes(4);
  });
});
