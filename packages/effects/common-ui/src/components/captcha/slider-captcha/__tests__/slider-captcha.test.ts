import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import SliderCaptcha from '../index.vue';

function dispatchMouseEvent(element: Element, type: string, pageX: number) {
  const event = new MouseEvent(type, { bubbles: true });
  Object.defineProperty(event, 'pageX', { value: pageX });
  element.dispatchEvent(event);
}

describe('slider-captcha', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockImplementation(
      function (this: HTMLElement) {
        return this.getAttribute('name') === 'captcha-action' ? 40 : 220;
      },
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('resets when the model value changes to false', async () => {
    const wrapper = mount(SliderCaptcha, {
      props: {
        modelValue: false,
        successText: 'Passed',
        text: 'Slide',
      },
    });
    const action = wrapper.find('[name="captcha-action"]');

    dispatchMouseEvent(action.element, 'mousedown', 0);
    dispatchMouseEvent(wrapper.element, 'mousemove', 200);
    await nextTick();

    expect(wrapper.text()).toContain('Passed');
    expect(action.attributes('style')).toContain('left: 180px');

    await wrapper.setProps({ modelValue: true });
    await wrapper.setProps({ modelValue: false });
    vi.runAllTimers();
    await nextTick();

    expect(wrapper.text()).toContain('Slide');
    expect(action.attributes('style')).toContain('left: 0px');
  });
});
