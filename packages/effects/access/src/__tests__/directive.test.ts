import { createApp, defineComponent } from 'vue';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { registerAccessDirective } from '../directive';

const runtime = vi.hoisted(() => ({
  accessMode: { value: 'backend' },
  hasAccessByCodes: vi.fn(),
  hasAccessByRoles: vi.fn(),
}));

vi.mock('../use-access', () => ({
  useAccess: () => runtime,
}));

const AccessDirectiveSubject = defineComponent({
  template: `
    <div>
      <span data-testid="role" v-access:role="'statistics:viewer'">role</span>
      <span data-testid="code" v-access:code="'statistics:read'">code</span>
    </div>
  `,
});

function mountSubject() {
  const container = document.createElement('div');
  const app = createApp(AccessDirectiveSubject);
  registerAccessDirective(app);
  app.mount(container);
  return { app, container };
}

describe('v-access directive', () => {
  beforeEach(() => {
    runtime.accessMode.value = 'backend';
    runtime.hasAccessByCodes.mockReset().mockReturnValue(false);
    runtime.hasAccessByRoles.mockReset().mockReturnValue(true);
  });

  it('后端菜单模式下 role 指令仍按角色代码判断', () => {
    const { app, container } = mountSubject();

    expect(container.querySelector('[data-testid="role"]')).not.toBeNull();
    expect(runtime.hasAccessByRoles).toHaveBeenCalledWith([
      'statistics:viewer',
    ]);
    expect(runtime.hasAccessByCodes).toHaveBeenCalledTimes(1);
    app.unmount();
  });

  it('code 指令独立使用菜单访问码，不混用角色代码', () => {
    runtime.hasAccessByRoles.mockReturnValue(false);
    runtime.hasAccessByCodes.mockReturnValue(true);

    const { app, container } = mountSubject();

    expect(container.querySelector('[data-testid="role"]')).toBeNull();
    expect(container.querySelector('[data-testid="code"]')).not.toBeNull();
    expect(runtime.hasAccessByCodes).toHaveBeenCalledWith(['statistics:read']);
    app.unmount();
  });
});
