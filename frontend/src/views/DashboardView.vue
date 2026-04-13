<template>
  <main class="page-shell">
    <div class="page-container">
      <DashboardHeader />

      <section class="kpi-grid">
        <KpiCard title="Total Production" :value="kpis.totalProduction.toLocaleString()" unit="units" trend="up" trend-value="+12.5%" icon-label="PRD" />
        <KpiCard title="Target Production" :value="kpis.totalTarget.toLocaleString()" unit="units" icon-label="TGT" />
        <KpiCard title="Achievement Rate" :value="kpis.achievementRate" unit="%" :trend="kpis.achievementRate >= 85 ? 'up' : 'down'" :trend-value="kpis.achievementRate >= 85 ? 'On track' : 'Below target'" :variant="kpis.achievementRate >= 85 ? 'success' : 'warning'" icon-label="ACH" />
        <KpiCard title="Average Uptime" :value="kpis.avgUptime" unit="%" :trend="kpis.avgUptime >= 90 ? 'up' : 'down'" :trend-value="kpis.avgUptime >= 90 ? 'Healthy' : 'Needs attention'" :variant="kpis.avgUptime >= 90 ? 'success' : 'warning'" icon-label="UPT" />
        <KpiCard title="Defect Rate" :value="kpis.avgDefectRate" unit="%" :trend="kpis.avgDefectRate <= 1 ? 'down' : 'up'" :trend-value="kpis.avgDefectRate <= 1 ? 'Good' : 'High'" :variant="kpis.avgDefectRate <= 1 ? 'success' : 'danger'" icon-label="QTY" />
      </section>

      <section class="stack-block">
        <ProductionChart />
      </section>

      <section class="dashboard-grid">
        <div class="dashboard-grid__wide">
          <LineStatusTable :lines="productionLines" />
        </div>
        <div class="dashboard-grid__side">
          <EquipmentSummary :summary="summary" />
          <AlarmList :alarms="alarms" :max-items="4" />
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import AlarmList from '@/components/dashboard/AlarmList.vue'
import DashboardHeader from '@/components/dashboard/DashboardHeader.vue'
import EquipmentSummary from '@/components/dashboard/EquipmentSummary.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import LineStatusTable from '@/components/dashboard/LineStatusTable.vue'
import ProductionChart from '@/components/dashboard/ProductionChart.vue'
import { alarms, getEquipmentSummary, getKPIs, productionLines } from '@/lib/mes-data'

const kpis = getKPIs()
const summary = getEquipmentSummary()
</script>
