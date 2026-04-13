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
      <div class="dashboard-header__clock">
        <p class="dashboard-header__time">{{ currentTime }}</p>
        <p class="dashboard-header__date">{{ currentDate }}</p>
      </div>
      <button type="button" class="icon-button" @click="refreshTime">↻</button>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const now = ref(new Date())
let timerId

const refreshTime = () => {
  now.value = new Date()
}

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
})

onBeforeUnmount(() => {
  window.clearInterval(timerId)
})
</script>
