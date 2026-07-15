<script lang="ts" setup>
import { AuthenticationForgetPassword } from '@vben/common-ui';

import { $t } from '#/locales';

defineOptions({ name: 'ForgetPassword' });
</script>

<template>
  <AuthenticationForgetPassword
    :show-submit-button="false"
    :sub-title="$t('page.auth.forgetPasswordContactAdmin')"
  />
</template>
