<script setup lang="ts">
import type { Recordable } from '@vben/types';

import type { VbenFormSchema } from '#/adapter/form';

import { computed } from 'vue';

import { ProfilePasswordSetting, z } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { updateAdminPassword } from '#/api';
import { useAuthStore } from '#/store';

const authStore = useAuthStore();

const strongPassword = z
  .string()
  .min(8, { message: '密码至少 8 个字符' })
  .max(32, { message: '密码最多 32 个字符' })
  .refine(
    (value) =>
      !/\s/.test(value) &&
      /[a-z]/.test(value) &&
      /[A-Z]/.test(value) &&
      /\d/.test(value) &&
      /[^A-Za-z0-9]/.test(value),
    { message: '密码需包含大小写字母、数字和特殊字符，且不能含空白' },
  );

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      fieldName: 'oldPassword',
      label: '旧密码',
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: '请输入旧密码',
      },
      rules: z.string().min(1, { message: '请输入旧密码' }),
    },
    {
      fieldName: 'newPassword',
      label: '新密码',
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: '请输入新密码',
      },
      rules: strongPassword,
    },
    {
      fieldName: 'confirmPassword',
      label: '确认密码',
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: '请再次输入新密码',
      },
      dependencies: {
        rules(values) {
          const { newPassword } = values;
          return z
            .string({ required_error: '请再次输入新密码' })
            .min(1, { message: '请再次输入新密码' })
            .refine((value) => value === newPassword, {
              message: '两次输入的密码不一致',
            });
        },
        triggerFields: ['newPassword'],
      },
    },
  ];
});

async function handleSubmit(values: Recordable<any>) {
  await updateAdminPassword({
    newPassword: values.newPassword,
    oldPassword: values.oldPassword,
  });
  ElMessage.success('密码已修改，请重新登录');
  await authStore.resetSessionToLogin();
}
</script>
<template>
  <ProfilePasswordSetting
    class="w-full max-w-xl"
    :form-schema="formSchema"
    @submit="handleSubmit"
  />
</template>
