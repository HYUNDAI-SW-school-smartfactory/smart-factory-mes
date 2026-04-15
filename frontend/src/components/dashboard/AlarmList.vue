<template>
  <BaseCard :title="title">
    <div v-if="displayAlarms.length" class="alarm-list">
      <article
        v-for="alarm in displayAlarms"
        :key="alarm.id"
        class="alarm-item"
        :class="[
          `alarm-item--${alarm.severity}`,
          { 'alarm-item--clickable': resolveAlarmTarget(alarm) },
        ]"
        @click="handleAlarmClick(alarm)"
      >
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
import { useRouter } from 'vue-router'
import { markAlarmAsRead } from '@/lib/alarm-inbox'
import BaseCard from './BaseCard.vue'

const props = defineProps({
  alarms: { type: Array, default: () => [] },
  title: { type: String, default: 'Recent Alarms' },
  maxItems: { type: Number, default: 5 },
})

const router = useRouter()
const displayAlarms = computed(() => props.alarms.slice(0, props.maxItems))

const severitySymbol = (severity) => {
  if (severity === 'critical') return '!'
  if (severity === 'warning') return '*'
  return 'i'
}

const resolveAlarmTarget = (alarm) => {
  if (alarm.equipmentId) return `/equipment/${alarm.equipmentId}`
  if (alarm.lineId) return `/line/${alarm.lineId}`
  return ''
}

const handleAlarmClick = (alarm) => {
  const target = resolveAlarmTarget(alarm)
  markAlarmAsRead(alarm.id)
  if (!target) return
  router.push(target)
}
</script>
