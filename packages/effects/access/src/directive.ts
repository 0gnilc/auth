/**
 * 挂载时按角色或菜单访问码移除不可见元素，仅控制界面入口，不执行 Server 的 API 授权。
 * @example v-access:role="[ROLE_CODE]" 或 v-access:code="[ACCESS_CODE]"
 */
import type { App, Directive, DirectiveBinding } from 'vue';

import { useAccess } from './use-access';

function isAccessible(
  el: Element,
  binding: DirectiveBinding<string | string[]>,
) {
  const { hasAccessByCodes, hasAccessByRoles } = useAccess();

  const value = binding.value;

  if (!value) return;
  const authMethod =
    binding.arg === 'role' ? hasAccessByRoles : hasAccessByCodes;

  const values = Array.isArray(value) ? value : [value];

  if (!authMethod(values)) {
    el?.remove();
  }
}

const mounted = (el: Element, binding: DirectiveBinding<string | string[]>) => {
  isAccessible(el, binding);
};

const authDirective: Directive = {
  mounted,
};

export function registerAccessDirective(app: App) {
  app.directive('access', authDirective);
}
