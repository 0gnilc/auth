import { describe, expect, it, vi } from 'vitest';

import { useColumns } from '../data';

vi.mock('#/locales', () => ({
  $t: (key: string) => key,
}));

function statusColumn(columns: ReturnType<typeof useColumns>) {
  const column = columns?.find((candidate) => candidate?.field === 'status');
  if (!column) throw new Error('Status column not found');
  return column;
}

describe('administrator management columns', () => {
  it('uses an editable status switch only when update access is available', () => {
    const beforeChange = vi.fn();
    const editableStatus = statusColumn(useColumns(beforeChange));
    const readOnlyStatus = statusColumn(useColumns());

    expect(editableStatus.cellRender).toMatchObject({
      attrs: { beforeChange },
      name: 'CellSwitch',
    });
    expect(readOnlyStatus.cellRender).toMatchObject({
      name: 'CellTag',
    });
  });
});
