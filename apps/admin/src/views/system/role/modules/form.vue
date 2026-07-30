<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { RoleApi } from '#/api';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { isEqual, trimToNull } from '@vben/utils';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createRole, updateRole } from '#/api';
import { $t } from '#/locales';

import { confirmDrawerClose } from '../../components/dirty';

const emit = defineEmits<{ success: [] }>();

type RoleForm = Partial<Pick<RoleApi.Role, 'code' | 'id' | 'name' | 'remark'>>;

const initialValues = ref<RoleForm>();
const saved = ref(false);

const schema: VbenFormSchema[] = [
  {
    component: 'Input',
    fieldName: 'id',
    hide: true,
  },
  {
    component: 'Input',
    fieldName: 'code',
    label: $t('page.systemRole.form.code'),
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('page.systemRole.form.name'),
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: { rows: 4, type: 'textarea' },
    fieldName: 'remark',
    label: $t('page.systemRole.form.remark'),
  },
];

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  schema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1',
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onBeforeClose() {
    if (saved.value) return true;
    return confirmDrawerClose(
      !isEqual(await formApi.getValues(), initialValues.value),
    );
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues<RoleForm>();
    trimToNull(values);
    drawerApi.lock();
    try {
      const data = {
        code: values.code ?? '',
        name: values.name ?? '',
        remark: values.remark,
      };
      await (values.id
        ? updateRole({ id: values.id, ...data })
        : createRole(data));
      saved.value = true;
      ElMessage.success($t('page.systemRole.messages.saveSuccess'));
      emit('success');
      await drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
  async onOpenChange(open) {
    if (!open) return;
    saved.value = false;
    const row = drawerApi.getData<Partial<RoleApi.Role>>();
    const values: RoleForm = {
      code: row.code ?? '',
      id: row.id,
      name: row.name ?? '',
      remark: row.remark ?? '',
    };
    drawerApi.setState({
      title: row.id
        ? $t('page.systemRole.drawer.editTitle')
        : $t('page.systemRole.drawer.createTitle'),
    });
    await formApi.reset();
    await nextTick();
    await formApi.setValues(values, false);
    initialValues.value = await formApi.getValues<RoleForm>();
  },
});
</script>

<template>
  <Drawer class="w-full sm:max-w-xl">
    <Form />
  </Drawer>
</template>
