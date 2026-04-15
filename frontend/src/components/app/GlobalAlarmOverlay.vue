<template>
  <Teleport to="body">
    <div v-if="currentAlarm && shouldRender" class="global-alarm-overlay">
      <section class="global-alarm-modal">
        <div class="global-alarm-modal__topbar">
          <span>Emergency Broadcast</span>
          <span>{{ currentAlarm.time }}</span>
        </div>

        <div class="global-alarm-modal__eyebrow">
          <span class="global-alarm-modal__eyebrow-mark">!</span>
          <span>{{ eyebrowText }}</span>
        </div>

        <h2 class="global-alarm-modal__title">{{ title }}</h2>
        <p class="global-alarm-modal__instruction">{{ instruction }}</p>
        <p class="global-alarm-modal__message">{{ currentAlarm.message }}</p>

        <div class="global-alarm-modal__meta">
          <div>
            <span class="global-alarm-modal__label">Line</span>
            <strong>{{ currentAlarm.lineName }}</strong>
          </div>
          <div>
            <span class="global-alarm-modal__label">Equipment</span>
            <strong>{{ currentAlarm.equipmentName || '-' }}</strong>
          </div>
          <div>
            <span class="global-alarm-modal__label">Detected At</span>
            <strong>{{ currentAlarm.time }}</strong>
          </div>
        </div>

        <div class="global-alarm-modal__actions">
          <button type="button" class="global-alarm-modal__button global-alarm-modal__button--ghost" @click="dismissAlarm">
            Dismiss Alert
          </button>
          <button type="button" class="global-alarm-modal__button global-alarm-modal__button--primary" @click="moveToAlarmTarget">
            Open Equipment
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isAuthenticated } from '@/lib/auth'
import { connectDashboardStream } from '@/lib/mes-live'

const route = useRoute()
const router = useRouter()

const currentAlarm = ref(null)
const alarmQueue = ref([])
const announcedAlarmKeys = ref([])
const shouldRender = computed(() => route.meta.requiresAuth && isAuthenticated())

let primed = false
let disconnect = () => {}

watch(
  () => shouldRender.value,
  (enabled) => {
    disconnect()
    currentAlarm.value = null
    alarmQueue.value = []
    primed = false

    if (!enabled) {
      return
    }

    disconnect = connectDashboardStream({
      onData: (dashboard) => {
        handleDashboardAlarms(dashboard.alarms ?? [])
      },
    })
  },
  { immediate: true },
)

const title = computed(() => {
  if (!currentAlarm.value) return ''
  if (currentAlarm.value.alarmType === 'ERROR') {
    return 'ERROR CONDITION DETECTED'
  }
  return 'EMERGENCY STOP DETECTED'
})

const eyebrowText = computed(() => {
  if (!currentAlarm.value) return ''
  return currentAlarm.value.alarmType === 'ERROR' ? 'Critical Fault Alarm' : 'Critical Stop Alarm'
})

const instruction = computed(() => {
  if (!currentAlarm.value) return ''
  if (currentAlarm.value.alarmType === 'ERROR') {
    return 'Immediate operator intervention is required before this equipment can safely resume.'
  }
  return 'Production flow has been interrupted. Review the equipment state and upstream impact now.'
})

function handleDashboardAlarms(alarms) {
  const criticalAlarms = alarms
    .filter((alarm) => alarm.alarmType === 'STOP' || alarm.alarmType === 'ERROR')
    .map((alarm) => ({
      ...alarm,
      eventKey: createAlarmEventKey(alarm),
    }))

  if (!primed) {
    primed = true
    mergeAnnouncedAlarmKeys(criticalAlarms.map((alarm) => alarm.eventKey))
    return
  }

  const newAlarms = criticalAlarms.filter((alarm) => !announcedAlarmKeys.value.includes(alarm.eventKey))
  if (!newAlarms.length) {
    return
  }

  mergeAnnouncedAlarmKeys(newAlarms.map((alarm) => alarm.eventKey))

  newAlarms.forEach((alarm) => {
    if (currentAlarm.value?.eventKey === alarm.eventKey || alarmQueue.value.some((queued) => queued.eventKey === alarm.eventKey)) {
      return
    }
    alarmQueue.value.push(alarm)
  })

  if (!currentAlarm.value) {
    currentAlarm.value = alarmQueue.value.shift() ?? null
  }
}

function dismissAlarm() {
  currentAlarm.value = alarmQueue.value.shift() ?? null
}

function moveToAlarmTarget() {
  const target = currentAlarm.value?.equipmentId
    ? `/equipment/${currentAlarm.value.equipmentId}`
    : currentAlarm.value?.lineId
      ? `/line/${currentAlarm.value.lineId}`
      : ''

  dismissAlarm()

  if (target) {
    router.push(target)
  }
}

function createAlarmEventKey(alarm) {
  return [alarm.id, alarm.alarmType, alarm.lineId, alarm.equipmentId, alarm.time].join(':')
}

function mergeAnnouncedAlarmKeys(keys) {
  announcedAlarmKeys.value = Array.from(new Set([...announcedAlarmKeys.value, ...keys])).slice(-100)
}

onBeforeUnmount(() => {
  disconnect()
})
</script>
