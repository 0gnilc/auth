<script setup lang="ts">
import type { Recordable } from '@vben/types';

import type { VbenFormSchema } from '#/adapter/form';

import { computed, onMounted, ref } from 'vue';

import { ProfileBaseSetting } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { getAdminUserInfo, updateAdminUserInfo } from '#/api';
import { z } from '#/adapter/form';
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
        placeholder: '请输入昵称',
      },
      label: '昵称',
      rules: z.string().trim().min(1, { message: '请输入昵称' }),
    },
    {
      fieldName: 'avatar',
      component: 'Input',
      componentProps: {
        maxlength: 500,
        placeholder: '请输入头像 URL，留空可清除',
      },
      label: '头像 URL',
    },
    {
      fieldName: 'desc',
      component: 'Input',
      componentProps: {
        maxlength: 500,
        rows: 4,
        type: 'textarea',
      },
      label: '个人简介',
    },
  ];
});

async function handleSubmit(values: Recordable<any>) {
  await updateAdminUserInfo({
    avatar: values.avatar,
    desc: values.desc,
    nickname: values.nickname,
  });
  const userInfo = await authStore.getUserInfo();
  profileBaseSettingRef.value?.getFormApi().setValues(userInfo);
  ElMessage.success('基本资料已更新');
}

onMounted(async () => {
  const data = await getAdminUserInfo();
  profileBaseSettingRef.value.getFormApi().setValues(data);
});
</script>
<template>
  <ProfileBaseSetting
    ref="profileBaseSettingRef"
    :form-schema="formSchema"
    @submit="handleSubmit"
  />
</template>
