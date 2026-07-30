import type { Page } from 'playwright/test';

import { expect, test } from 'playwright/test';

interface ApiEnvelope<T = unknown> {
  code: number;
  data: T;
}

async function readApiResponse<T = unknown>(response: {
  json: () => Promise<unknown>;
}) {
  const body = await response.json();
  return body as ApiEnvelope<T>;
}

async function passSliderCaptcha(page: Page) {
  const action = page.locator('[name="captcha-action"]');
  const wrapper = action.locator('..');
  await expect(action).toBeVisible();
  await expect
    .poll(async () => {
      const currentActionBox = await action.boundingBox();
      const currentWrapperBox = await wrapper.boundingBox();
      if (!currentActionBox || !currentWrapperBox) {
        return Number.POSITIVE_INFINITY;
      }
      return Math.abs(currentActionBox.x - currentWrapperBox.x);
    })
    .toBeLessThanOrEqual(2);
  const actionBox = await action.boundingBox();
  const wrapperBox = await wrapper.boundingBox();
  if (!actionBox || !wrapperBox) throw new Error('Captcha is not measurable');

  await page.mouse.move(
    actionBox.x + actionBox.width / 2,
    actionBox.y + actionBox.height / 2,
  );
  await page.mouse.down();
  await page.mouse.move(
    wrapperBox.x + wrapperBox.width - actionBox.width / 2 - 2,
    actionBox.y + actionBox.height / 2,
    { steps: 8 },
  );
  await page.mouse.up();
  await expect(wrapper).toContainText(/Passed|验证通过/);
}

async function submitLogin(page: Page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().endsWith('/api/sys/admin/login'),
  );
  await page.getByRole('button', { name: 'login' }).click();
  return responsePromise;
}

async function loginSuccessfully(
  page: Page,
  usernameValue: string,
  passwordValue: string,
) {
  await page.goto('/auth/login');
  await page
    .locator('input:not([type="checkbox"])')
    .first()
    .fill(usernameValue);
  await page.locator('input[type="password"]').first().fill(passwordValue);
  await passSliderCaptcha(page);
  const response = await submitLogin(page);
  const body = await readApiResponse<{
    accessToken: string;
    refreshToken: string;
  }>(response);
  expect(body.code).toBe(0);
  await expect(page).toHaveURL(/\/dashboard/);
  return body.data;
}

function rowContaining(page: Page, text: string) {
  return page.locator('.vxe-body--row').filter({ hasText: text });
}

test('failed login can be retried and the resulting session can log out', async ({
  page,
}) => {
  await page.goto('/auth/login');
  const username = page.locator('input:not([type="checkbox"])').first();
  const password = page.locator('input[type="password"]').first();
  await username.fill('admin');
  await password.fill('incorrect-password');
  await passSliderCaptcha(page);

  const failedResponse = await submitLogin(page);
  const failedBody = await readApiResponse(failedResponse);
  expect(failedBody.code).not.toBe(0);
  await expect(page).toHaveURL(/\/auth\/login/);

  await password.fill('123456');
  await passSliderCaptcha(page);
  const successResponse = await submitLogin(page);
  const successBody = await readApiResponse(successResponse);
  expect(successBody.code).toBe(0);
  await expect(page).toHaveURL(/\/dashboard/);

  await page.getByRole('banner').getByRole('button').last().click();
  await page.getByRole('menuitem', { name: /Logout|退出登录/ }).click();
  await page.getByRole('button', { name: /^(Confirm|确认)$/ }).click();
  await expect(page).toHaveURL(/\/auth\/login/);
});

test('administrator CRUD preserves unsaved edits until the operator decides', async ({
  page,
}) => {
  const usernameValue = `e2e-admin-${Date.now()}`;
  await loginSuccessfully(page, 'admin', '123456');
  await page.goto('/system/admin');
  await page
    .getByRole('button', { name: /Add administrator|新增后台管理员/ })
    .click();

  let drawer = page.getByRole('dialog').filter({
    hasText: /Add administrator|新增后台管理员/,
  });
  await drawer.getByLabel(/Username|用户名/).fill(usernameValue);
  await drawer.getByLabel(/Password|密码/).fill('Strong#123');
  await drawer.getByLabel(/Nickname|昵称/).fill('E2E Original');
  const createResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith('/api/sys/admin/create'),
  );
  await drawer.getByRole('button', { name: /Confirm|确认/ }).click();
  const createResponse = await createResponsePromise;
  const createBody = await readApiResponse(createResponse);
  expect(createBody.code).toBe(0);

  let row = rowContaining(page, usernameValue);
  await expect(row).toContainText('E2E Original');
  await row.getByRole('button', { name: /Edit|修改/ }).click();
  drawer = page.getByRole('dialog').filter({
    hasText: /Edit administrator|修改后台管理员/,
  });
  const nickname = drawer.getByLabel(/Nickname|昵称/);
  await nickname.fill('E2E Unsaved');
  await drawer.getByRole('button').first().click();
  const unsavedDialog = page.getByRole('dialog').filter({
    hasText: /Unsaved changes|未保存的修改/,
  });
  await unsavedDialog
    .getByRole('button', { name: /Keep editing|继续编辑/ })
    .click();
  await expect(nickname).toHaveValue('E2E Unsaved');

  await drawer.getByRole('button').first().click();
  await page
    .getByRole('dialog')
    .filter({ hasText: /Unsaved changes|未保存的修改/ })
    .getByRole('button', { name: /Discard changes|放弃修改/ })
    .click();
  await expect(drawer).toBeHidden();

  row = rowContaining(page, usernameValue);
  await row.getByRole('button', { name: /Edit|修改/ }).click();
  drawer = page.getByRole('dialog').filter({
    hasText: /Edit administrator|修改后台管理员/,
  });
  await drawer.getByLabel(/Nickname|昵称/).fill('E2E Saved');
  const updateResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith('/api/sys/admin/update'),
  );
  await drawer.getByRole('button', { name: /Confirm|确认/ }).click();
  const updateResponse = await updateResponsePromise;
  const updateBody = await readApiResponse(updateResponse);
  expect(updateBody.code).toBe(0);
  await expect(rowContaining(page, usernameValue)).toContainText('E2E Saved');

  row = rowContaining(page, usernameValue);
  await row.getByRole('button').last().click();
  await page.getByRole('menuitem', { name: /Remove|删除/ }).click();
  const removeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/sys/admin/remove/'),
  );
  await page
    .getByRole('button', { name: /Confirm|确认/ })
    .last()
    .click();
  const removeResponse = await removeResponsePromise;
  const removeBody = await readApiResponse(removeResponse);
  expect(removeBody.code).toBe(0);
  await expect(rowContaining(page, usernameValue)).toHaveCount(0);
});

test('baseline administrators cannot see or call management capabilities', async ({
  page,
}) => {
  const limitedUsername = `e2e-limited-${Date.now()}`;
  const managerSession = await loginSuccessfully(page, 'admin', '123456');
  const managerHeaders = {
    Authorization: `Bearer ${managerSession.accessToken}`,
  };
  const createResponse = await page.request.post('/api/sys/admin/create', {
    data: {
      homePath: '/dashboard',
      nickname: 'E2E Limited',
      password: 'Strong#123',
      status: true,
      username: limitedUsername,
    },
    headers: managerHeaders,
  });
  const createBody = await readApiResponse(createResponse);
  expect(createBody.code).toBe(0);

  const limitedLogin = await page.request.post('/api/sys/admin/login', {
    data: { password: 'Strong#123', username: limitedUsername },
  });
  const limitedLoginBody = await readApiResponse<{
    accessToken: string;
  }>(limitedLogin);
  const limitedSession = limitedLoginBody.data;
  const forbidden = await page.request.post('/api/sys/admin/page', {
    data: { currentPage: 1, pageSize: 10 },
    headers: { Authorization: `Bearer ${limitedSession.accessToken}` },
  });
  expect(forbidden.status()).toBe(403);

  await page.evaluate(() => localStorage.clear());
  await loginSuccessfully(page, limitedUsername, 'Strong#123');
  await expect(
    page.getByText(/System Management|系统管理/, { exact: true }),
  ).toHaveCount(0);
  await page.goto('/system/admin');
  await expect(
    page.getByRole('button', { name: /Add administrator|新增后台管理员/ }),
  ).toHaveCount(0);

  const pageResponse = await page.request.post('/api/sys/admin/page', {
    data: {
      currentPage: 1,
      pageSize: 10,
      username: limitedUsername,
    },
    headers: managerHeaders,
  });
  const pageBody = await readApiResponse<{ list: Array<{ id: string }> }>(
    pageResponse,
  );
  const limitedAdmin = pageBody.data.list[0];
  if (!limitedAdmin) throw new Error('Limited administrator was not found');
  const removeResponse = await page.request.post(
    `/api/sys/admin/remove/${limitedAdmin.id}`,
    { headers: managerHeaders },
  );
  const removeBody = await readApiResponse(removeResponse);
  expect(removeBody.code).toBe(0);
});

test('a dynamic Message Key is created without overwriting an existing resource', async ({
  page,
}) => {
  const messageKey = `e2e.message.key${Date.now()}`;
  const retryKey = `e2e.message.retry${Date.now()}`;
  const managerSession = await loginSuccessfully(page, 'admin', '123456');
  const headers = { Authorization: `Bearer ${managerSession.accessToken}` };

  await page.goto('/system/i18n-message');
  await page.getByRole('button', { name: /Create|新增/ }).click();
  const drawer = page.getByRole('dialog').filter({
    hasText: /Create internationalization message|新增国际化消息/,
  });
  await drawer.getByLabel(/Message Key/).fill(messageKey);
  await drawer.getByLabel('en-US').fill('E2E original message');
  await drawer.getByLabel('zh-CN').fill('E2E 原始消息');
  const createResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith('/api/sys/i18n-message/create'),
  );
  await drawer.getByRole('button', { name: /Confirm|确认/ }).click();
  const createResponse = await createResponsePromise;
  const createBody = await readApiResponse<{
    category: string;
    messageKey: string;
  }>(createResponse);
  expect(createBody.code).toBe(0);
  expect(createBody.data.messageKey).toBe(messageKey);
  await expect(drawer).toBeHidden();

  const pageResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith('/api/sys/i18n-message/page'),
  );
  await page
    .getByRole('textbox', { name: 'Message Key', exact: true })
    .fill(messageKey);
  await page.getByRole('button', { name: /^(Search|搜索)$/ }).click();
  await pageResponsePromise;
  await expect(rowContaining(page, messageKey)).toContainText(
    'E2E original message',
  );

  await page.getByRole('button', { name: /Create|新增/ }).click();
  await drawer.getByLabel(/Message Key/).fill(messageKey);
  await drawer.getByLabel('en-US').fill('E2E replacement message');
  await drawer.getByLabel('zh-CN').fill('E2E 替换消息');
  const duplicateResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith('/api/sys/i18n-message/create'),
  );
  await drawer.getByRole('button', { name: /Confirm|确认/ }).click();
  const duplicateResponse = await duplicateResponsePromise;
  const duplicateBody = await readApiResponse(duplicateResponse);
  expect(duplicateBody.code).toBe(10_001);
  await expect(
    page.getByText(/Message key already exists|Message key已存在/),
  ).toBeVisible();
  await expect(drawer).toBeVisible();
  await expect(drawer.getByLabel(/Message Key/)).toHaveValue(messageKey);
  await expect(drawer.getByLabel('en-US')).toHaveValue(
    'E2E replacement message',
  );

  await drawer.getByLabel(/Message Key/).fill(retryKey);
  const retryResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith('/api/sys/i18n-message/create'),
  );
  await drawer.getByRole('button', { name: /Confirm|确认/ }).click();
  const retryResponse = await retryResponsePromise;
  const retryBody = await readApiResponse(retryResponse);
  expect(retryBody.code).toBe(0);
  await expect(drawer).toBeHidden();

  const valuesResponse = await page.request.post(
    `/api/sys/i18n-message/values/${encodeURIComponent(messageKey)}`,
    { headers },
  );
  const valuesBody = await readApiResponse(valuesResponse);
  expect(valuesBody.data).toMatchObject({
    category: createBody.data.category,
    messageKey,
    values: expect.arrayContaining([
      { locale: 'en-US', value: 'E2E original message' },
      { locale: 'zh-CN', value: 'E2E 原始消息' },
    ]),
  });

  const removeResponse = await page.request.post(
    `/api/sys/i18n-message/remove/${encodeURIComponent(messageKey)}`,
    { headers },
  );
  const removeBody = await readApiResponse(removeResponse);
  expect(removeBody.code).toBe(0);

  const removeRetryResponse = await page.request.post(
    `/api/sys/i18n-message/remove/${encodeURIComponent(retryKey)}`,
    { headers },
  );
  const removeRetryBody = await readApiResponse(removeRetryResponse);
  expect(removeRetryBody.code).toBe(0);
});
