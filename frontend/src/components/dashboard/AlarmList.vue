<template>
  <BaseCard :title="title">
    <div v-if="displayAlarms.length" class="alarm-list">
      <article v-for="alarm in displayAlarms" :key="alarm.id" class="alarm-item" :class="`alarm-item--${alarm.severity}`">
        <div class="alarm-item__symbol">{{ severitySymbol(alarm.severity) }}</div>
        <div class="alarm-item__content">
          <div class="alarm-item__header">
            <span>{{ alarm.lineName }}</span>
            <span>{{ alarm.equipmentName }}</span>
          </div>
          <p>{{ alarm.message }}</p>
          <small>{{ alarm.time }}</small>
        </div>
      </article>
    </div>
    <p v-else class="empty-state">No alarms</p>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from './BaseCard.vue'

const props = defineProps({
  alarms: { type: Array, default: () => [] },
  title: { type: String, default: 'Recent Alarms' },
  maxItems: { type: Number, default: 5 },
})

const displayAlarms = computed(() => props.alarms.slice(0, props.maxItems))

const severitySymbol = (severity) => {
  if (severity === 'critical') return '!'
  if (severity === 'warning') return '*'
  return 'i'
}
</script>
