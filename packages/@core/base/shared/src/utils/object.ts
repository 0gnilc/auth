/**
 * Trim the own, enumerable string properties of an object in place.
 *
 * Whitespace-only strings become `null`. Non-string values, inherited
 * properties, nested object properties, and explicitly excluded properties
 * are left unchanged. The original object is returned so callers can
 * normalize a form payload while constructing an API request.
 *
 * @param target - Object whose top-level string properties will be normalized.
 * @param excludedProperties - Property names that must retain their original values.
 * @returns The same object instance after normalization.
 */
export function trimToNull<T extends object>(
  target: T,
  ...excludedProperties: (keyof T)[]
): T {
  const excluded = new Set<PropertyKey>(excludedProperties);
  const record = target as Record<PropertyKey, unknown>;

  for (const key of Object.keys(target)) {
    if (excluded.has(key) || typeof record[key] !== 'string') {
      continue;
    }

    const value = record[key].trim();
    record[key] = value === '' ? null : value;
  }

  return target;
}
