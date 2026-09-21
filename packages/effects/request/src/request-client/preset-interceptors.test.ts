import axios from 'axios';
import { describe, expect, it, vi } from 'vitest';

import {
  authenticateResponseInterceptor,
  errorMessageResponseInterceptor,
} from './preset-interceptors';
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
  it('并发 401 共用一次刷新并使用新令牌重放', async () => {
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

  it('旧业务重放未完成时允许新的 401 启动下一轮刷新', async () => {
    // 将令牌刷新与业务重放设为两个独立 Promise，防止把刷新互斥范围延伸到整段业务请求。
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

  it('刷新失败拒绝全部等待请求并统一重新认证', async () => {
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

describe('errorMessageResponseInterceptor', () => {
  it('lets the application resolve and display the current localized message', async () => {
    const onError = vi.fn();
    const resolveMessage = vi.fn(() => 'Request forbidden');
    const interceptor = errorMessageResponseInterceptor({
      onError,
      resolveMessage,
    });
    const error = { response: { status: 403 } };
    if (!interceptor.rejected)
      throw new Error('Rejected interceptor is missing');

    await expect(interceptor.rejected(error)).rejects.toBe(error);

    expect(resolveMessage).toHaveBeenCalledWith('forbidden', error);
    expect(onError).toHaveBeenCalledWith('Request forbidden', error);
  });

  it('classifies network and timeout failures independently of translations', async () => {
    const onError = vi.fn();
    const resolveMessage = vi.fn((type: string) => type);
    const interceptor = errorMessageResponseInterceptor({
      onError,
      resolveMessage,
    });
    if (!interceptor.rejected)
      throw new Error('Rejected interceptor is missing');

    const networkError = new Error('Network Error');
    const timeoutError = new Error('timeout of 10000ms exceeded');
    await expect(interceptor.rejected(networkError)).rejects.toBe(networkError);
    await expect(interceptor.rejected(timeoutError)).rejects.toBe(timeoutError);

    expect(resolveMessage.mock.calls.map(([type]) => type)).toEqual([
      'network-error',
      'request-timeout',
    ]);
  });

  it('取消请求不显示错误提示', async () => {
    const onError = vi.fn();
    const resolveMessage = vi.fn();
    const interceptor = errorMessageResponseInterceptor({
      onError,
      resolveMessage,
    });
    const error = new axios.CanceledError();
    if (!interceptor.rejected)
      throw new Error('Rejected interceptor is missing');

    await expect(interceptor.rejected(error)).rejects.toBe(error);

    expect(resolveMessage).not.toHaveBeenCalled();
    expect(onError).not.toHaveBeenCalled();
  });

  it('关闭单次错误提示仍保留请求失败', async () => {
    const onError = vi.fn();
    const resolveMessage = vi.fn();
    const interceptor = errorMessageResponseInterceptor({
      onError,
      resolveMessage,
    });
    const error = {
      config: { showErrorMessage: false },
      response: { status: 500 },
    };
    if (!interceptor.rejected)
      throw new Error('Rejected interceptor is missing');

    await expect(interceptor.rejected(error)).rejects.toBe(error);

    expect(resolveMessage).not.toHaveBeenCalled();
    expect(onError).not.toHaveBeenCalled();
  });
});
