import { shallowMount } from '@vue/test-utils';

import { afterEach, describe, expect, it, vi } from 'vitest';

import ThemeButton from '../theme-button.vue';

describe('theme button', () => {
  afterEach(() => {
    Reflect.deleteProperty(document, 'startViewTransition');
    document.documentElement.classList.remove('theme-switching');
    vi.unstubAllGlobals();
  });

  it('updates the theme without starting a staged page transition', async () => {
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: false })),
    );
    const startViewTransition = vi.fn(
      (updateTheme: () => Promise<void> | void) => {
        void updateTheme();
        return {
          ready: new Promise<void>(() => {}),
          skipTransition: vi.fn(),
        };
      },
    );
    Object.defineProperty(document, 'startViewTransition', {
      configurable: true,
      value: startViewTransition,
    });
    const animationFrames: FrameRequestCallback[] = [];
    vi.stubGlobal(
      'requestAnimationFrame',
      vi.fn((callback: FrameRequestCallback) => {
        animationFrames.push(callback);
        return animationFrames.length;
      }),
    );

    const wrapper = shallowMount(ThemeButton, {
      global: {
        stubs: {
          Button: {
            template: '<button><slot /></button>',
          },
        },
      },
      props: {
        modelValue: false,
      },
    });

    await wrapper.get('button').trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([[true]]);
    expect(startViewTransition).not.toHaveBeenCalled();
    expect(document.documentElement.classList.contains('theme-switching')).toBe(
      true,
    );

    animationFrames.shift()?.(0);
    expect(document.documentElement.classList.contains('theme-switching')).toBe(
      true,
    );

    animationFrames.shift()?.(16);
    expect(document.documentElement.classList.contains('theme-switching')).toBe(
      false,
    );
  });
});
