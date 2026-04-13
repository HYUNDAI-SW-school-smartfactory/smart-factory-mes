<template>
  <main class="page-shell" v-if="item">
    <div class="page-container page-container--compact">
      <header class="detail-header">
        <RouterLink class="icon-button icon-button--link" :to="`/line/${item.lineId}`">←</RouterLink>
        <div class="detail-header__text">
          <div class="detail-header__row">
            <h1>{{ item.name }}</h1>
            <StatusBadge :status="item.status" />
          </div>
          <p>Equipment Details</p>
        </div>
      </header>

      <BaseCard title="Basic Information">
        <div class="info-grid">
          <article>
            <p class="eyebrow">Equipment Name</p>
            <p class="info-value">{{ item.name }}</p>
          </article>
          <article>
            <p class="eyebrow">Production Line</p>
            <RouterLink :to="`/line/${item.lineId}`" class="text-link info-value">{{ item.lineName }}</RouterLink>
          </article>
          <article>
            <p class="eyebrow">Status</p>
            <div class="info-value"><StatusBadge :status="item.status" size="sm" /></div>
          </article>
          <article>
            <p class="eyebrow">Last Updated</p>
            <p class="info-value">{{ item.lastUpdated }}</p>
          </article>
        </div>
      </BaseCard>

      <section class="kpi-grid kpi-grid--three">
        <KpiCard title="Production" :value="item.production.toLocaleString()" unit="units" icon-label="PRD" />
        <KpiCard title="UPH (Units Per Hour)" :value="item.uph" unit="uph" :variant="item.uph > 0 ? 'success' : 'danger'" icon-label="SPD" />
        <KpiCard title="Uptime" :value="item.uptime" unit="%" :variant="uptimeVariant" icon-label="UPT" />
      </section>

      <section class="equipment-grid">
        <BaseCard title="Current Status">
          <div class="status-hero">
            <div class="status-hero__ring" :class="`status-hero__ring--${item.status.toLowerCase()}`">
              <span class="status-hero__core" :class="`status-hero__core--${item.status.toLowerCase()}`" />
            </div>
            <p class="status-hero__label" :class="`status-hero__label--${item.status.toLowerCase()}`">{{ item.status }}</p>
            <p class="status-hero__sub">Last updated: {{ item.lastUpdated }}</p>
          </div>
          <div class="status-hero__stats">
            <div>
              <p>{{ item.uph }}</p>
              <span>Current UPH</span>
            </div>
            <div>
              <p>{{ item.uptime }}%</p>
              <span>Uptime Today</span>
            </div>
          </div>
        </BaseCard>

        <AlarmList :alarms="equipmentAlarms" :title="`Equipment Alarms (${equipmentAlarms.length})`" :max-items="6" />
      </section>
    </div>
  </main>
  <main v-else class="page-shell page-shell--centered">
    <div class="page-container page-container--tiny">
      <h1 class="not-found__title">Equipment not found</h1>
      <RouterLink to="/" class="text-link">Return to dashboard</RouterLink>
    </div>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AlarmList from '@/components/dashboard/AlarmList.vue'
import BaseCard from '@/components/dashboard/BaseCard.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import StatusBadge from '@/components/dashboard/StatusBadge.vue'
import { getAlarmsByEquipmentId, getEquipmentById } from '@/lib/mes-data'

const route = useRoute()

const item = computed(() => getEquipmentById(route.params.id))
const equipmentAlarms = computed(() => (item.value ? getAlarmsByEquipmentId(item.value.id) : []))
const uptimeVariant = computed(() => {
  if (!item.value) return 'default'
  if (item.value.uptime >= 90) return 'success'
  if (item.value.uptime >= 70) return 'warning'
  return 'danger'
})
</script>
