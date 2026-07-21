function isMessageObject(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

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

function collectLeafKeys(
  messages: Record<string, unknown>,
  prefix: string,
  target: Set<string>,
) {
  for (const [key, value] of Object.entries(messages)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (isMessageObject(value)) {
      collectLeafKeys(value, path, target);
    } else {
      target.add(path);
    }
  }
}

export { collectLeafKeys, mergeMessages };
