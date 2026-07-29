<script setup lang="ts">
import type { Recordable } from '@vben/types';

import type { VbenFormSchema } from '#/adapter/form';

import { computed } from 'vue';

import { ProfilePasswordSetting, z } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { updatePassword } from '#/api';
import { $t } from '#/locales';
import { useAuthStore } from '#/store';

const authStore = useAuthStore();

const formSchema = computed((): VbenFormSchema[] => {
  const strongPassword = z
    .string()
    .min(8, { message: $t('page.profile.form.passwordMinLength') })
    .max(32, { message: $t('page.profile.form.passwordMaxLength') })
    .refine(
      (value) =>
        !/\s/.test(value) &&
        /[a-z]/.test(value) &&
        /[A-Z]/.test(value) &&
        /\d/.test(value) &&
        /[^A-Za-z0-9]/.test(value),
      { message: $t('page.profile.form.passwordComplexity') },
    );

  return [
    {
      fieldName: 'oldPassword',
      label: $t('page.profile.form.oldPassword'),
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('page.profile.form.oldPasswordPlaceholder'),
      },
      rules: z
        .string()
        .min(1, { message: $t('page.profile.form.oldPasswordPlaceholder') }),
    },
    {
      fieldName: 'newPassword',
      label: $t('page.profile.form.newPassword'),
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: $t('page.profile.form.newPasswordPlaceholder'),
      },
      rules: strongPassword,
    },
    {
      fieldName: 'confirmPassword',
      label: $t('page.profile.form.confirmPassword'),
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: $t('page.profile.form.confirmPasswordPlaceholder'),
      },
      dependencies: {
        rules(values) {
          const { newPassword } = values;
          return z
            .string({
              error: $t('page.profile.form.confirmPasswordPlaceholder'),
            })
            .min(1, {
              message: $t('page.profile.form.confirmPasswordPlaceholder'),
            })
            .refine((value) => value === newPassword, {
              message: $t('page.profile.form.passwordMismatch'),
            });
        },
        triggerFields: ['newPassword'],
      },
    },
  ];
});

async function handleSubmit(values: Recordable<any>) {
  await updatePassword(values.oldPassword, values.newPassword);
  ElMessage.success($t('page.profile.messages.passwordUpdated'));
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
