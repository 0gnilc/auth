<script setup lang="ts">
import type { Props } from './types';

import { preferences } from '@vben-core/preferences';
import {
  Card,
  Separator,
  Tabs,
  TabsList,
  TabsTrigger,
  VbenAvatar,
} from '@vben-core/shadcn-ui';

import { Page } from '../../components';

defineOptions({
  name: 'ProfileUI',
});

withDefaults(defineProps<Props>(), {
  title: '关于项目',
  tabs: () => [],
});

const tabsValue = defineModel<string>('modelValue');
</script>
<template>
  <Page auto-content-height>
    <div class="flex size-full flex-col gap-4 md:flex-row md:gap-0">
      <Card class="w-full flex-none md:w-1/6">
        <div
          class="mt-4 flex h-auto items-center gap-4 px-4 md:h-40 md:flex-col md:justify-center md:px-0"
        >
          <VbenAvatar
            :src="userInfo?.avatar ?? preferences.app.defaultAvatar"
            class="size-16 shrink-0 md:size-20"
          />
          <div class="min-w-0 md:text-center">
            <span class="block break-words text-lg font-semibold">
              {{ userInfo?.nickname ?? '' }}
            </span>
            <span class="block break-all text-sm text-foreground/80">
              {{ userInfo?.username ?? '' }}
            </span>
          </div>
        </div>
        <Separator class="my-4" />
        <Tabs v-model="tabsValue" orientation="vertical" class="m-4">
          <TabsList class="grid w-full grid-cols-2 bg-card md:grid-cols-1">
            <TabsTrigger
              v-for="tab in tabs"
              :key="tab.value"
              :value="tab.value"
              class="h-12 justify-center data-[state=active]:bg-primary data-[state=active]:text-primary-foreground md:justify-start"
            >
              {{ tab.label }}
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </Card>
      <Card class="w-full flex-auto p-4 md:ml-4 md:w-5/6 md:p-8">
        <slot name="content"></slot>
      </Card>
    </div>
  </Page>
</template>
