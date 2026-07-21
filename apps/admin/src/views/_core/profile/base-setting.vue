<script setup lang="ts">
import type { Recordable } from '@vben/types';

import type { VbenFormSchema } from '#/adapter/form';

import { computed, onMounted, ref } from 'vue';

import { ProfileBaseSetting } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { z } from '#/adapter/form';
import { getAdminUserInfo, updateProfile } from '#/api';
import { $t } from '#/locales';
import { useAuthStore } from '#/store';

const profileBaseSettingRef = ref();
const authStore = useAuthStore();

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      fieldName: 'nickname',
      component: 'Input',
      componentProps: {
        maxlength: 255,
        placeholder: $t('page.profile.form.nicknamePlaceholder'),
      },
      label: $t('page.profile.form.nickname'),
      rules: z
        .string()
        .trim()
        .min(1, { message: $t('page.profile.form.nicknamePlaceholder') }),
    },
    {
      fieldName: 'avatar',
      component: 'Input',
      componentProps: {
        maxlength: 500,
        placeholder: $t('page.profile.form.avatarPlaceholder'),
      },
      label: $t('page.profile.form.avatarUrl'),
    },
    {
      fieldName: 'desc',
      component: 'Input',
      componentProps: {
        maxlength: 500,
        rows: 4,
        type: 'textarea',
      },
      label: $t('page.profile.form.description'),
    },
  ];
});

async function handleSubmit(values: Recordable<any>) {
  await updateProfile({
    avatar: values.avatar,
    desc: values.desc,
    nickname: values.nickname,
  });
  const userInfo = await authStore.getUserInfo();
  profileBaseSettingRef.value?.getFormApi().setValues(userInfo);
  ElMessage.success($t('page.profile.messages.basicUpdated'));
}

onMounted(async () => {
  const data = await getAdminUserInfo();
  profileBaseSettingRef.value.getFormApi().setValues(data);
});
</script>
<template>
  <ProfileBaseSetting
    class="w-full"
    ref="profileBaseSettingRef"
    :form-schema="formSchema"
    @submit="handleSubmit"
  />
</template>
