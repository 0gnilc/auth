<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { AdminApi } from '#/api';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { isEqual, trimToNull } from '@vben/utils';

import { ElMessage } from 'element-plus';

import { useVbenForm, z } from '#/adapter/form';
import { createAdmin, updateAdmin } from '#/api';
import { $t } from '#/locales';

import { confirmDrawerClose } from '../../components/dirty';

const emit = defineEmits<{ success: [] }>();

type AdminForm = Partial<AdminApi.Admin> & {
  currentAdmin: boolean;
  password: string;
};

const initialValues = ref<AdminForm>();
const saved = ref(false);

const passwordRule = z
  .string()
  .max(32, { message: $t('page.profile.form.passwordMaxLength') })
  .refine(
    (value) =>
      !value ||
      (value.length >= 8 &&
        !/\s/.test(value) &&
        /[a-z]/.test(value) &&
        /[A-Z]/.test(value) &&
        /\d/.test(value) &&
        /[^A-Za-z0-9]/.test(value)),
    { message: $t('page.profile.form.passwordComplexity') },
  );

const schema: VbenFormSchema<AdminForm>[] = [
  {
    component: 'Input',
    fieldName: 'id',
    hide: true,
  },
  {
    component: 'Input',
    fieldName: 'currentAdmin',
    hide: true,
  },
  {
    component: 'Input',
    fieldName: 'username',
    label: $t('page.systemAdmin.form.username'),
    rules: 'required',
  },
  {
    component: 'VbenInputPassword',
    componentProps: {
      passwordStrength: true,
      placeholder: $t('page.systemAdmin.form.passwordUnchanged'),
    },
    fieldName: 'password',
    label: $t('page.systemAdmin.form.password'),
    rules: passwordRule,
  },
  {
    component: 'Input',
    fieldName: 'nickname',
    label: $t('page.systemAdmin.form.nickname'),
    rules: 'required',
  },
  {
    component: 'Input',
    defaultValue: '/dashboard',
    fieldName: 'homePath',
    label: $t('page.systemAdmin.form.homePath'),
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'avatar',
    label: $t('page.systemAdmin.form.avatar'),
  },
  {
    component: 'Input',
    componentProps: { rows: 3, type: 'textarea' },
    fieldName: 'desc',
    formItemClass: 'col-span-full',
    label: $t('page.systemAdmin.form.description'),
  },
  {
    component: 'Switch',
    defaultValue: true,
    dependencies: {
      resolve: ({ values }) => ({
        componentProps: { disabled: Boolean(values.currentAdmin) },
      }),
      triggerFields: ['currentAdmin'],
    },
    fieldName: 'status',
    label: $t('page.systemAdmin.form.status'),
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
    const values = await formApi.getValues<AdminForm>();
    if (!values.id && !values.password) {
      ElMessage.error($t('page.systemAdmin.validation.password'));
      return;
    }

    drawerApi.lock();
    try {
      trimToNull(values, 'password');
      await (values.id
        ? updateAdmin({
            avatar: values.avatar,
            desc: values.desc,
            homePath: values.homePath,
            id: values.id,
            nickname: values.nickname,
            password: values.password || undefined,
            status: values.status,
            username: values.username,
          })
        : createAdmin({
            avatar: values.avatar,
            desc: values.desc,
            homePath: values.homePath ?? '/dashboard',
            nickname: values.nickname ?? '',
            password: values.password,
            status: values.status ?? true,
            username: values.username ?? '',
          }));
      saved.value = true;
      ElMessage.success($t('page.systemAdmin.messages.saveSuccess'));
      emit('success');
      await drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
  async onOpenChange(open) {
    if (!open) return;
    saved.value = false;
    const row = drawerApi.getData<Partial<AdminForm>>();
    const values: AdminForm = {
      avatar: row.avatar ?? '',
      currentAdmin: row.currentAdmin ?? false,
      desc: row.desc ?? '',
      homePath: row.homePath ?? '/dashboard',
      id: row.id,
      nickname: row.nickname ?? '',
      password: '',
      status: row.status ?? true,
      username: row.username ?? '',
    };
    drawerApi.setState({
      title: row.id
        ? $t('page.systemAdmin.drawer.editTitle')
        : $t('page.systemAdmin.drawer.createTitle'),
    });
    await formApi.reset();
    await nextTick();
    await formApi.setValues(values, false);
    initialValues.value = await formApi.getValues<AdminForm>();
  },
});
</script>

<template>
  <Drawer class="w-full sm:max-w-2xl">
    <Form />
  </Drawer>
</template>
