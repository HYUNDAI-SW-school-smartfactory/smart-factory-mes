<template>
  <main class="page-shell" v-if="line">
    <div class="page-container page-container--narrow">
      <header class="detail-header">
        <RouterLink class="icon-button icon-button--link" to="/">Back</RouterLink>
        <div class="detail-header__text">
          <div class="detail-header__row">
            <h1>{{ line.name }}</h1>
            <StatusBadge :status="line.status" />
          </div>
          <p>Line Details and Equipment Status</p>
        </div>
      </header>

      <section class="kpi-grid">
        <KpiCard title="Production" :value="line.production.toLocaleString()" unit="units" icon-label="PRD" />
        <KpiCard title="Target Production" :value="line.targetProduction.toLocaleString()" unit="units" icon-label="TGT" />
        <KpiCard title="Achievement Rate" :value="achievementRate" unit="%" :variant="achievementRate >= 85 ? 'success' : 'warning'" icon-label="ACH" />
        <KpiCard title="Uptime" :value="line.uptime" unit="%" :variant="line.uptime >= 90 ? 'success' : 'warning'" icon-label="UPT" />
        <KpiCard title="Defect Rate" :value="line.defectRate" unit="%" :variant="line.defectRate <= 1 ? 'success' : 'danger'" icon-label="QTY" />
      </section>

      <section class="stack-block">
        <LineProcessLayout :line-name="line.name" :equipments="lineEquipment" />
      </section>

      <section class="dashboard-grid">
        <div class="dashboard-grid__wide">
          <EquipmentTable :items="lineEquipment" :title="`Equipment (${lineEquipment.length})`" />
        </div>
        <div>
          <AlarmList :alarms="lineAlarms" :title="`Line Alarms (${lineAlarms.length})`" :max-items="6" />
        </div>
      </section>
    </div>
  </main>
  <main v-else-if="isLoading" class="page-shell page-shell--centered">
    <div class="page-container page-container--tiny">
      <p class="not-found__eyebrow">Loading</p>
      <h1 class="not-found__title">Preparing line view...</h1>
      <p class="not-found__text">Realtime line data is being connected now.</p>
    </div>
  </main>
  <main v-else class="page-shell page-shell--centered">
    <div class="page-container page-container--tiny">
      <h1 class="not-found__title">{{ errorMessage || 'Line not found' }}</h1>
      <RouterLink to="/" class="text-link">Return to dashboard</RouterLink>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AlarmList from '@/components/dashboard/AlarmList.vue'
import EquipmentTable from '@/components/dashboard/EquipmentTable.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import StatusBadge from '@/components/dashboard/StatusBadge.vue'
import LineProcessLayout from '@/components/line/LineProcessLayout.vue'
import { connectLineStream } from '@/lib/mes-live'

const route = useRoute()
const line = ref(null)
const lineEquipment = ref([])
const lineAlarms = ref([])
const errorMessage = ref('')
const isLoading = ref(true)
let disconnect = () => {}

const achievementRate = computed(() => {
  if (!line.value || !line.value.targetProduction) return 0
  return Number(((line.value.production / line.value.targetProduction) * 100).toFixed(1))
})

watch(
  () => route.params.id,
  (lineId) => {
    disconnect()
    line.value = null
    lineEquipment.value = []
    lineAlarms.value = []
    errorMessage.value = ''
    isLoading.value = true

    disconnect = connectLineStream(lineId, {
      onData: (payload) => {
        line.value = payload.line
        lineEquipment.value = payload.equipments
        lineAlarms.value = payload.alarms
        isLoading.value = false
      },
      onError: (error) => {
        errorMessage.value = error.message
        line.value = null
        isLoading.value = false
      },
    })
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  disconnect()
})
</script>
