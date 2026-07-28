/**
 * 判断语言包节点是否还能继续向下合并。
 *
 * 只有普通对象表示命名空间；字符串、数字、布尔值、null 和数组都视为
 * 最终消息值。数组不按下标递归合并，避免把一个完整配置值意外拼接成旧值。
 */
function isMessageObject(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

/**
 * 深度合并两棵 vue-i18n 消息树，第二个参数在叶子节点上拥有更高优先级。
 *
 * 调用方通过传参顺序表达消息来源优先级。当前应用先放入数据库动态消息，
 * 再覆盖公共静态语言包，最后覆盖应用静态语言包，因此数据库配置不能替换
 * 随代码发布的页面文案。函数每层都创建新对象，不直接修改任一输入对象。
 */
function mergeMessages(
  base: Record<string, unknown>,
  override: Record<string, unknown>,
): Record<string, unknown> {
  const result: Record<string, unknown> = { ...base };
  for (const [key, value] of Object.entries(override)) {
    const current = result[key];
    result[key] =
      isMessageObject(current) && isMessageObject(value)
        ? mergeMessages(current, value)
        : value;
  }
  return result;
}

export { mergeMessages };
