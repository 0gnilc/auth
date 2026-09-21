import { describe, expect, it, vi } from 'vitest';

import { createToolbarGroups } from './toolbar';

describe('tiptap toolbar image contract', () => {
  it('offers only Managed Image upload when an uploader is configured', () => {
    const imageAction = createToolbarGroups({
      upload: vi.fn(async () => ({ objectKey: 'content/image.png', url: '' })),
    })
      .flat()
      .find((action) =>
        action.menu?.items.some((item) => item.shortLabel === 'UPL'),
      );

    expect(imageAction?.menu?.items.map((item) => item.shortLabel)).toEqual([
      'UPL',
    ]);
  });
});
