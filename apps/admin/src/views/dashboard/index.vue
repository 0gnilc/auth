<script setup lang="ts">
import { computed } from 'vue';

import { useAccessStore, useUserStore } from '@vben/stores';

import { $t } from '#/locales';

const accessStore = useAccessStore();
const userStore = useUserStore();

const userInfo = computed(() => userStore.userInfo);
const roleCodes = computed(() => userInfo.value?.roleCodes ?? []);
const accessCodes = computed(() => accessStore.accessCodes ?? []);
const initials = computed(() => {
  const source = userInfo.value?.nickname || userInfo.value?.username || 'A';
  return source.trim().slice(0, 2).toUpperCase();
});
</script>

<template>
  <main
    class="min-h-full bg-background px-4 py-6 text-foreground md:px-8 md:py-8"
  >
    <header
      class="flex flex-col gap-5 border-b border-border pb-7 sm:flex-row sm:items-center"
    >
      <div
        class="flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-full bg-primary text-lg font-semibold text-primary-foreground"
      >
        <img
          v-if="userInfo?.avatar"
          :src="userInfo.avatar"
          :alt="userInfo.nickname || userInfo.username"
          class="size-full object-cover"
        />
        <span v-else>{{ initials }}</span>
      </div>
      <div class="min-w-0">
        <p class="mb-1 text-sm text-muted-foreground">
          {{ $t('page.dashboard.currentAdmin') }}
        </p>
        <h1 class="break-words text-2xl font-semibold">
          {{ userInfo?.nickname || userInfo?.username }}
        </h1>
        <p class="mt-1 break-words text-sm text-muted-foreground">
          {{ userInfo?.desc || $t('page.dashboard.emptyDescription') }}
        </p>
      </div>
    </header>

    <div class="grid gap-5 py-6 lg:grid-cols-2">
      <section class="rounded-md border border-border bg-card p-5">
        <h2 class="text-base font-semibold">
          {{ $t('page.dashboard.basicInfo') }}
        </h2>
        <dl class="mt-4 grid gap-4 text-sm sm:grid-cols-2">
          <div class="min-w-0">
            <dt class="text-muted-foreground">
              {{ $t('page.dashboard.username') }}
            </dt>
            <dd class="mt-1 break-all font-medium">{{ userInfo?.username }}</dd>
          </div>
          <div class="min-w-0">
            <dt class="text-muted-foreground">
              {{ $t('page.dashboard.defaultHome') }}
            </dt>
            <dd class="mt-1 break-all font-medium">
              {{ userInfo?.homePath || '/dashboard' }}
            </dd>
          </div>
        </dl>
      </section>

      <section class="rounded-md border border-border bg-card p-5">
        <div class="flex items-center justify-between gap-3">
          <h2 class="text-base font-semibold">
            {{ $t('page.dashboard.roleCodes') }}
          </h2>
          <span class="text-sm text-muted-foreground">{{
            roleCodes.length
          }}</span>
        </div>
        <div v-if="roleCodes.length" class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="code in roleCodes"
            :key="code"
            class="max-w-full break-all rounded-md border border-border bg-muted px-2.5 py-1 text-sm"
          >
            {{ code }}
          </span>
        </div>
        <p v-else class="mt-4 text-sm text-muted-foreground">
          {{ $t('page.dashboard.emptyRoleCodes') }}
        </p>
      </section>
    </div>

    <section class="rounded-md border border-border bg-card p-5">
      <div class="flex items-center justify-between gap-3">
        <h2 class="text-base font-semibold">
          {{ $t('page.dashboard.accessCodes') }}
        </h2>
        <span class="text-sm text-muted-foreground">{{
          accessCodes.length
        }}</span>
      </div>
      <div
        v-if="accessCodes.length"
        class="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-3"
      >
        <code
          v-for="code in accessCodes"
          :key="code"
          class="min-w-0 break-all rounded-md border border-border bg-muted px-3 py-2 text-sm"
        >
          {{ code }}
        </code>
      </div>
      <p v-else class="mt-4 text-sm text-muted-foreground">
        {{ $t('page.dashboard.emptyAccessCodes') }}
      </p>
    </section>
  </main>
</template>
