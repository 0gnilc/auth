<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { PermissionApi } from '#/api';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { isEqual, trimToNull } from '@vben/utils';

import { ElMessage, ElMessageBox } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createPermission, updatePermission } from '#/api';
import { $t } from '#/locales';

import { confirmDrawerClose } from '../../components/dirty';

const emit = defineEmits<{ success: [] }>();

type PermissionForm = Partial<
  Pick<
    PermissionApi.Permission,
    | 'code'
    | 'id'
    | 'name'
    | 'publicAccess'
    | 'remark'
    | 'targetIdentifier'
    | 'targetQualifier'
  >
>;

const initialValues = ref<PermissionForm>();
const initialPublicAccess = ref(false);
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
    label: $t('page.systemPermission.form.code'),
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('page.systemPermission.form.name'),
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      placeholder: $t('page.systemPermission.form.qualifierPlaceholder'),
    },
    fieldName: 'targetQualifier',
    label: $t('page.systemPermission.form.qualifier'),
  },
  {
    component: 'Input',
    fieldName: 'targetIdentifier',
    label: $t('page.systemPermission.form.targetIdentifier'),
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: { rows: 3, type: 'textarea' },
    fieldName: 'remark',
    formItemClass: 'col-span-full',
    label: $t('page.systemPermission.form.remark'),
  },
  {
    component: 'Switch',
    defaultValue: false,
    description: $t('page.systemPermission.form.publicHint'),
    fieldName: 'publicAccess',
    label: $t('page.systemPermission.form.publicAccess'),
  },
];

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  schema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 sm:grid-cols-2',
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
    const values = await formApi.getValues<PermissionForm>();
    if (!initialPublicAccess.value && values.publicAccess) {
      try {
        await ElMessageBox.confirm(
          $t('page.systemPermission.messages.publicConfirm'),
          $t('page.systemPermission.messages.publicTitle'),
          { type: 'warning' },
        );
      } catch {
        return;
      }
    }
    trimToNull(values);
    drawerApi.lock();
    try {
      const data = {
        code: values.code ?? '',
        name: values.name ?? '',
        publicAccess: values.publicAccess ?? false,
        remark: values.remark,
        targetIdentifier: values.targetIdentifier ?? '',
        targetQualifier: values.targetQualifier,
      };
      await (values.id
        ? updatePermission({ id: values.id, ...data })
        : createPermission(data));
      saved.value = true;
      ElMessage.success($t('page.systemPermission.messages.saveSuccess'));
      emit('success');
      await drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
  async onOpenChange(open) {
    if (!open) return;
    saved.value = false;
    const row = drawerApi.getData<Partial<PermissionApi.Permission>>();
    initialPublicAccess.value = row.publicAccess ?? false;
    const values: PermissionForm = {
      code: row.code ?? '',
      id: row.id,
      name: row.name ?? '',
      publicAccess: row.publicAccess ?? false,
      remark: row.remark ?? '',
      targetIdentifier: row.targetIdentifier ?? '',
      targetQualifier: row.targetQualifier ?? '',
    };
    drawerApi.setState({
      title: row.id
        ? $t('page.systemPermission.drawer.editTitle')
        : $t('page.systemPermission.drawer.createTitle'),
    });
    await formApi.resetForm();
    await nextTick();
    await formApi.setValues(values, false);
    initialValues.value = await formApi.getValues<PermissionForm>();
  },
});
</script>

<template>
  <Drawer class="w-full sm:max-w-2xl">
    <Form />
  </Drawer>
</template>
