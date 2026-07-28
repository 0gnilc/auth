import type { TableActionProps } from '@vben/common-ui';
import type { VxeTableGridOptions } from '@vben/plugins/vxe-table';

import type { ComponentPropsMap, ComponentType } from './component';

import { defineComponent, h } from 'vue';

import { useAccess } from '@vben/access';
import { VbenTableAction as VbenTableActionCore } from '@vben/common-ui';
import {
  setupVbenVxeTable,
  useVbenVxeGrid as useGrid,
} from '@vben/plugins/vxe-table';

import { ElButton, ElImage, ElSwitch, ElTag } from 'element-plus';

import { $t } from '#/locales';

import { useVbenForm } from './form';

setupVbenVxeTable({
  configVxeTable: (vxeUI) => {
    vxeUI.setConfig({
      grid: {
        align: 'center',
        border: false,
        columnConfig: {
          resizable: true,
        },
        minHeight: 180,
        formConfig: {
          // 全局禁用vxe-table的表单配置，使用formOptions
          enabled: false,
        },
        proxyConfig: {
          autoLoad: true,
          response: {
            result: 'list',
            total: 'total',
            list: 'list',
          },
          showActiveMsg: true,
          showResponseMsg: false,
        },
        round: true,
        showOverflow: true,
        size: 'small',
      } as VxeTableGridOptions,
    });

    // 表格配置项可以用 cellRender: { name: 'CellImage' },
    vxeUI.renderer.add('CellImage', {
      renderTableDefault(renderOpts, params) {
        const { props } = renderOpts;
        const { column, row } = params;
        const src = row[column.field];
        return h(ElImage, { src, previewSrcList: [src], ...props });
      },
    });

    // 表格配置项可以用 cellRender: { name: 'CellLink' },
    vxeUI.renderer.add('CellLink', {
      renderTableDefault(renderOpts) {
        const { props } = renderOpts;
        return h(
          ElButton,
          { size: 'small', link: true },
          { default: () => props?.text },
        );
      },
    });

    // 单元格渲染：Tag。
    vxeUI.renderer.add('CellTag', {
      renderTableDefault({ options, props }, { column, row }) {
        const value = row[column.field];
        const tagOptions = options ?? [
          {
            effect: 'plain',
            label: $t('common.enabled'),
            type: 'success',
            value: true,
          },
          {
            effect: 'plain',
            label: $t('common.disabled'),
            type: 'info',
            value: false,
          },
        ];
        const tagItem = tagOptions.find((item) => item.value === value);
        const { label, value: _optionValue, ...tagProps } = tagItem ?? {};
        return h(
          ElTag,
          { ...props, ...tagProps },
          { default: () => label ?? value },
        );
      },
    });

    vxeUI.renderer.add('CellSwitch', {
      renderTableDefault({ attrs, props }, { column, row }) {
        const loadingKey = `__loading_${column.field}`;
        const finallyProps = {
          activeText: $t('common.enabled'),
          activeValue: true,
          inactiveText: $t('common.disabled'),
          inactiveValue: false,
          inlinePrompt: true,
          ...props,
          loading: row[loadingKey] ?? false,
          modelValue: row[column.field],
          'onUpdate:modelValue': onChange,
        };

        async function onChange(newValue: unknown) {
          row[loadingKey] = true;
          try {
            const result = await attrs?.beforeChange?.(newValue, row);
            if (result !== false) {
              row[column.field] = newValue;
            }
          } finally {
            row[loadingKey] = false;
          }
        }

        return h(ElSwitch, finallyProps);
      },
    });

    // 这里可以自行扩展 vxe-table 的全局配置，比如自定义格式化
    // vxeUI.formats.add
  },
  useVbenForm,
});

export const useVbenVxeGrid = <T extends Record<string, any>>(
  ...rest: Parameters<typeof useGrid<T, ComponentType, ComponentPropsMap>>
) => useGrid<T, ComponentType, ComponentPropsMap>(...rest);

/**
 * 表格操作按钮组件。
 *
 * 在应用适配层统一注入访问码校验，页面只需通过 action.auth 声明权限码。
 * 显式传入 hasPermission 时仍可覆盖默认校验逻辑。
 */
export const VbenTableAction = defineComponent(
  (props: TableActionProps, { attrs, slots }) => {
    const { hasAccessByCodes } = useAccess();

    function hasPermission(auth?: string | string[]) {
      if (!auth) return true;
      return hasAccessByCodes(Array.isArray(auth) ? auth : [auth]);
    }

    return () =>
      h(VbenTableActionCore, { hasPermission, ...props, ...attrs }, slots);
  },
  {
    name: 'VbenTableAction',
    inheritAttrs: false,
  },
);

export type * from '@vben/plugins/vxe-table';
