function parsePort(value, source) {
  let port = Number.NaN;
  if (typeof value === 'number') port = value;
  else if (/^\d+$/u.test(value ?? '')) port = Number(value);

  if (!Number.isInteger(port) || port < 1 || port > 65_535) {
    throw new Error(`Invalid port in ${source}: ${value ?? 'missing'}`);
  }
  return port;
}

export { parsePort };
