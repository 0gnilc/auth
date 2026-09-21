import type { RequestClient } from '../request-client';

import { describe, expect, it, vi } from 'vitest';

import { FileUploader } from './uploader';

describe('fileUploader', () => {
  it('maps upload fields and config to FormData and merged headers', async () => {
    const post = vi.fn();
    const uploader = new FileUploader({ post } as unknown as RequestClient);
    const file = new File(['statement'], 'statement.txt', {
      type: 'text/plain',
    });

    await uploader.upload(
      '/files',
      {
        file,
        label: 'statement',
        omitted: undefined,
        tags: ['finance', undefined, 'monthly'],
      },
      {
        headers: { 'X-Upload': 'ledger' },
        timeout: 5000,
      },
    );

    const [url, formData, config] = post.mock.calls[0] as [
      string,
      FormData,
      object,
    ];
    expect(url).toBe('/files');
    expect([...formData.entries()]).toEqual([
      ['file', file],
      ['label', 'statement'],
      ['tags[0]', 'finance'],
      ['tags[2]', 'monthly'],
    ]);
    expect(config).toEqual({
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-Upload': 'ledger',
      },
      timeout: 5000,
    });
  });
});
