<template>
  <header class="dashboard-header">
    <div class="dashboard-header__brand">
      <div class="dashboard-header__mark">M</div>
      <div>
        <h1 class="dashboard-header__title">MES Dashboard</h1>
        <p class="dashboard-header__subtitle">Smart Factory Control Center</p>
      </div>
    </div>

    <div class="dashboard-header__meta">
      <div class="dashboard-header__user" v-if="user">
        <p class="dashboard-header__welcome">{{ user.displayName }}</p>
        <button type="button" class="dashboard-header__logout" @click="handleLogout">Logout</button>
      </div>
      <div class="dashboard-header__alarm-box">
        <button
          type="button"
          class="icon-button dashboard-header__alarm-button"
          aria-label="Open alarm inbox"
          @click.stop="toggleAlarmPanel"
        >
          <svg class="dashboard-header__alarm-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M4 8.5a2.5 2.5 0 0 1 2.5-2.5h11A2.5 2.5 0 0 1 20 8.5v7A2.5 2.5 0 0 1 17.5 18h-11A2.5 2.5 0 0 1 4 15.5z"
            />
            <path d="M9 6V4.8A1.8 1.8 0 0 1 10.8 3h2.4A1.8 1.8 0 0 1 15 4.8V6" />
            <path d="M4 10h16" />
            <path d="M9.5 13.5h5" />
          </svg>
          <span v-if="unreadAlarmCount" class="dashboard-header__alarm-count">{{ unreadAlarmCount }}</span>
        </button>

        <section v-if="isAlarmPanelOpen" class="dashboard-header__alarm-panel">
          <div class="dashboard-header__alarm-panel-header">
            <h2>Alarm Inbox</h2>
            <span>{{ unreadAlarmCount }} unread</span>
          </div>

          <div v-if="displayAlarms.length" class="dashboard-header__alarm-list">
            <button
              v-for="alarm in displayAlarms"
              :key="alarm.id"
              type="button"
              class="dashboard-header__alarm-entry"
              :class="{ 'dashboard-header__alarm-entry--read': alarm.isRead }"
              @pointerdown.stop="markAlarmEntryRead(alarm)"
              @click.stop.prevent="handleAlarmClick(alarm)"
            >
              <span
                class="dashboard-header__alarm-entry-dot"
                :class="`dashboard-header__alarm-entry-dot--${alarm.severity}`"
              />
              <div class="dashboard-header__alarm-entry-body">
                <div class="dashboard-header__alarm-entry-header">
                  <strong>{{ alarm.lineName }}</strong>
                  <span>{{ alarm.time }}</span>
                </div>
                <p>{{ alarm.equipmentName }}</p>
                <small>{{ alarm.message }}</small>
              </div>
            </button>
          </div>
          <p v-else class="dashboard-header__alarm-empty">No recent alarms</p>
        </section>
      </div>
      <div class="dashboard-header__clock">
        <p class="dashboard-header__time">{{ currentTime }}</p>
        <p class="dashboard-header__date">{{ currentDate }}</p>
      </div>
      <button type="button" class="icon-button" aria-label="Refresh time" @click="refreshTime">R</button>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser, logout } from '@/lib/auth'
import { getReadAlarmIds, isAlarmRead, markAlarmAsRead, syncReadAlarmIds } from '@/lib/alarm-inbox'

const props = defineProps({
  alarms: { type: Array, default: () => [] },
})

const router = useRouter()
const now = ref(new Date())
const user = ref(getCurrentUser())
const isAlarmPanelOpen = ref(false)
const readAlarmIds = ref(getReadAlarmIds())
let timerId

const refreshTime = () => {
  now.value = new Date()
}

const handleLogout = () => {
  logout()
  router.push('/login')
}

const displayAlarms = computed(() =>
  props.alarms.slice(0, 6).map((alarm) => ({
    ...alarm,
    isRead: isAlarmRead(readAlarmIds.value, alarm.id),
  })),
)
const unreadAlarmCount = computed(() =>
  props.alarms
    .slice(0, 6)
    .filter((alarm) => !isAlarmRead(readAlarmIds.value, alarm.id)).length,
)

const resolveAlarmTarget = (alarm) => {
  if (alarm.equipmentId) return `/equipment/${alarm.equipmentId}`
  if (alarm.lineId) return `/line/${alarm.lineId}`
  return ''
}

const toggleAlarmPanel = () => {
  isAlarmPanelOpen.value = !isAlarmPanelOpen.value
}

const handleAlarmClick = (alarm) => {
  const target = resolveAlarmTarget(alarm)
  readAlarmIds.value = markAlarmAsRead(alarm.id)
  isAlarmPanelOpen.value = false
  if (!target) return
  router.push(target)
}

const markAlarmEntryRead = (alarm) => {
  readAlarmIds.value = markAlarmAsRead(alarm.id)
}

const closeAlarmPanel = () => {
  isAlarmPanelOpen.value = false
}

watch(
  () => props.alarms,
  (alarms) => {
    if (!alarms.length) {
      return
    }

    readAlarmIds.value = syncReadAlarmIds(alarms, 6)
  },
  { immediate: true },
)

const currentTime = computed(() =>
  now.value.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }),
)

const currentDate = computed(() =>
  now.value.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  }),
)

onMounted(() => {
  refreshTime()
  timerId = window.setInterval(refreshTime, 1000)
  window.addEventListener('click', closeAlarmPanel)
})

onBeforeUnmount(() => {
  window.clearInterval(timerId)
  window.removeEventListener('click', closeAlarmPanel)
})
</script>
