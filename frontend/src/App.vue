<template>
  <RouterView />
  <GlobalAlarmOverlay />
</template>

<script setup>
import { RouterView } from 'vue-router'
import GlobalAlarmOverlay from '@/components/app/GlobalAlarmOverlay.vue'
</script>
