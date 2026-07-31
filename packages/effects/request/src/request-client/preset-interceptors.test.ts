import { describe, expect, it, vi } from 'vitest';

import { authenticateResponseInterceptor } from './preset-interceptors';
import { RequestClient } from './request-client';

function deferred<T>() {
  let reject!: (reason?: unknown) => void;
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, reject, resolve };
}

function unauthorized(url: string) {
  return {
    config: { headers: {}, url },
    response: { status: 401 },
  };
}

describe('authenticateResponseInterceptor', () => {
  it('refreshes once and retries every concurrent 401 with the new token', async () => {
    const client = new RequestClient();
    const refresh = deferred<string>();
    const request = vi.spyOn(client, 'request').mockResolvedValue({ ok: true });
    const doRefreshToken = vi.fn(() => refresh.promise);
    const doReAuthenticate = vi.fn();
    const interceptor = authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: true,
      formatToken: (token) => (token ? `Bearer ${token}` : null),
    });
    if (!interceptor.rejected)
      throw new Error('Rejected interceptor is missing');

    const first = interceptor.rejected(unauthorized('/first'));
    const second = interceptor.rejected(unauthorized('/second'));
    expect(doRefreshToken).toHaveBeenCalledTimes(1);

    refresh.resolve('refreshed-token');
    await expect(Promise.all([first, second])).resolves.toEqual([
      { ok: true },
      { ok: true },
    ]);

    expect(request).toHaveBeenCalledTimes(2);
    expect(request.mock.calls.map(([url]) => url)).toEqual([
      '/first',
      '/second',
    ]);
    expect(request.mock.calls).toEqual(
      expect.arrayContaining([
        [
          '/first',
          expect.objectContaining({
            __isRetryRequest: true,
            headers: expect.objectContaining({
              Authorization: 'Bearer refreshed-token',
            }),
          }),
        ],
        [
          '/second',
          expect.objectContaining({
            __isRetryRequest: true,
            headers: expect.objectContaining({
              Authorization: 'Bearer refreshed-token',
            }),
          }),
        ],
      ]),
    );
    expect(doReAuthenticate).not.toHaveBeenCalled();
    expect(client.refreshTokenQueue).toHaveLength(0);
    expect(client.isRefreshing).toBe(false);
  });

  it('allows a later 401 to refresh while an earlier retry is still pending', async () => {
    const client = new RequestClient();
    const firstRefresh = deferred<string>();
    const firstRetry = deferred<{ ok: boolean }>();
    const request = vi
      .spyOn(client, 'request')
      .mockImplementation((url) =>
        url === '/first'
          ? firstRetry.promise
          : Promise.resolve({ retriedUrl: url }),
      );
    const doRefreshToken = vi
      .fn()
      .mockImplementationOnce(() => firstRefresh.promise)
      .mockResolvedValueOnce('second-token');
    const interceptor = authenticateResponseInterceptor({
      client,
      doReAuthenticate: vi.fn(),
      doRefreshToken,
      enableRefreshToken: true,
      formatToken: (token) => (token ? `Bearer ${token}` : null),
    });
    if (!interceptor.rejected)
      throw new Error('Rejected interceptor is missing');

    const first = interceptor.rejected(unauthorized('/first'));
    firstRefresh.resolve('first-token');
    await vi.waitFor(() => expect(request).toHaveBeenCalledTimes(1));
    expect(client.isRefreshing).toBe(false);

    await expect(interceptor.rejected(unauthorized('/later'))).resolves.toEqual(
      {
        retriedUrl: '/later',
      },
    );
    expect(doRefreshToken).toHaveBeenCalledTimes(2);

    firstRetry.resolve({ ok: true });
    await expect(first).resolves.toEqual({ ok: true });
  });

  it('rejects every queued request and re-authenticates once when refresh fails', async () => {
    const client = new RequestClient();
    const refresh = deferred<string>();
    const request = vi.spyOn(client, 'request').mockResolvedValue({ ok: true });
    const doReAuthenticate = vi.fn();
    const interceptor = authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken: () => refresh.promise,
      enableRefreshToken: true,
      formatToken: (token) => (token ? `Bearer ${token}` : null),
    });
    if (!interceptor.rejected)
      throw new Error('Rejected interceptor is missing');

    const first = interceptor.rejected(unauthorized('/first'));
    const second = interceptor.rejected(unauthorized('/second'));
    const refreshError = new Error('refresh unavailable');
    refresh.reject(refreshError);

    const results = await Promise.allSettled([first, second]);
    expect(results).toEqual([
      { reason: refreshError, status: 'rejected' },
      { reason: refreshError, status: 'rejected' },
    ]);
    expect(request).not.toHaveBeenCalled();
    expect(doReAuthenticate).toHaveBeenCalledTimes(1);
    expect(client.refreshTokenQueue).toHaveLength(0);
    expect(client.isRefreshing).toBe(false);
  });
});
