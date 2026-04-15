<template>
  <main class="page-shell">
    <div class="page-container">
      <DashboardHeader :alarms="dashboard.alarms" />

      <section class="kpi-grid">
        <KpiCard title="Total Production" :value="dashboard.kpis.totalProduction.toLocaleString()" unit="units" trend="up" trend-value="Realtime" icon-label="PRD" />
        <KpiCard title="Target Production" :value="dashboard.kpis.totalTarget.toLocaleString()" unit="units" icon-label="TGT" />
        <KpiCard title="Achievement Rate" :value="dashboard.kpis.achievementRate" unit="%" :trend="dashboard.kpis.achievementRate >= 85 ? 'up' : 'down'" :trend-value="dashboard.kpis.achievementRate >= 85 ? 'On track' : 'Below target'" :variant="dashboard.kpis.achievementRate >= 85 ? 'success' : 'warning'" icon-label="ACH" />
        <KpiCard title="Average Uptime" :value="dashboard.kpis.avgUptime" unit="%" :trend="dashboard.kpis.avgUptime >= 90 ? 'up' : 'down'" :trend-value="dashboard.kpis.avgUptime >= 90 ? 'Healthy' : 'Needs attention'" :variant="dashboard.kpis.avgUptime >= 90 ? 'success' : 'warning'" icon-label="UPT" />
        <KpiCard title="Defect Rate" :value="dashboard.kpis.avgDefectRate" unit="%" :trend="dashboard.kpis.avgDefectRate <= 1 ? 'down' : 'up'" :trend-value="dashboard.kpis.avgDefectRate <= 1 ? 'Good' : 'High'" :variant="dashboard.kpis.avgDefectRate <= 1 ? 'success' : 'danger'" icon-label="QTY" />
      </section>

      <section class="stack-block">
        <ProductionChart :points="dashboard.productionTrend" />
      </section>

      <section class="dashboard-grid">
        <div class="dashboard-grid__wide">
          <LineStatusTable :lines="dashboard.productionLines" />
        </div>
        <div class="dashboard-grid__side">
          <EquipmentSummary :summary="dashboard.summary" />
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import DashboardHeader from '@/components/dashboard/DashboardHeader.vue'
import EquipmentSummary from '@/components/dashboard/EquipmentSummary.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import LineStatusTable from '@/components/dashboard/LineStatusTable.vue'
import ProductionChart from '@/components/dashboard/ProductionChart.vue'
import { connectDashboardStream, getEmptyDashboardState } from '@/lib/mes-live'

const dashboard = ref(getEmptyDashboardState())

const disconnect = connectDashboardStream({
  onData: (nextDashboard) => {
    dashboard.value = nextDashboard
  },
})

onBeforeUnmount(() => {
  disconnect()
})
</script>
