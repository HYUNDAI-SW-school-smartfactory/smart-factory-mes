<template>
  <span class="status-badge" :class="[sizeClass, statusClass]">
    <span v-if="showDot" class="status-badge__dot" />
    {{ status }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
  size: { type: String, default: 'md' },
  showDot: { type: Boolean, default: true },
})

const sizeClass = computed(() => `status-badge--${props.size}`)
const statusClass = computed(() => `status-badge--${props.status.toLowerCase()}`)
</script>
