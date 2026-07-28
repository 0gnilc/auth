const formControlOffset = '0.5rem + 1px';

export function getProfileFormActionStyle(labelWidth: number) {
  return {
    marginInlineStart: `calc(${labelWidth}px + ${formControlOffset})`,
  };
}
