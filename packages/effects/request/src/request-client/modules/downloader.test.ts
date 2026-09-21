import type { RequestClient } from '../request-client';

import { describe, expect, it, vi } from 'vitest';

import { FileDownloader } from './downloader';

describe('fileDownloader', () => {
  it('adds the default blob download config', async () => {
    const request = vi.fn();
    const downloader = new FileDownloader({
      request,
    } as unknown as RequestClient);

    await downloader.download('/files/report');

    expect(request).toHaveBeenCalledWith('/files/report', {
      method: 'GET',
      responseReturn: 'body',
      responseType: 'blob',
    });
  });

  it('merges custom request facts while keeping the blob response type', async () => {
    const request = vi.fn();
    const downloader = new FileDownloader({
      request,
    } as unknown as RequestClient);

    await downloader.download('/files/report', {
      data: { format: 'csv' },
      headers: { 'X-Report': 'ledger' },
      method: 'POST',
      responseReturn: 'raw',
      responseType: 'text',
    });

    expect(request).toHaveBeenCalledWith('/files/report', {
      data: { format: 'csv' },
      headers: { 'X-Report': 'ledger' },
      method: 'POST',
      responseReturn: 'raw',
      responseType: 'blob',
    });
  });
});
