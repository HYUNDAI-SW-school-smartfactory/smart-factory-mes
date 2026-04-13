<template>
  <BaseCard title="Production Trend" subtitle="Last 8 hours">
    <div class="chart-card">
      <svg class="chart-card__svg" viewBox="0 0 760 280" preserveAspectRatio="none" aria-label="Production Trend Chart">
        <line
          v-for="y in gridLines"
          :key="`grid-${y}`"
          x1="54"
          x2="734"
          :y1="y"
          :y2="y"
          class="chart-card__grid"
        />
        <polyline :points="targetPoints" class="chart-card__line chart-card__line--target" />
        <polyline :points="productionPoints" class="chart-card__line chart-card__line--actual" />
        <g v-for="point in chartPoints" :key="point.time">
          <circle :cx="point.x" :cy="point.targetY" r="4" class="chart-card__dot--target" />
          <circle :cx="point.x" :cy="point.productionY" r="4" class="chart-card__dot--actual" />
          <text :x="point.x" y="262" text-anchor="middle" class="chart-card__label">{{ point.time }}</text>
        </g>
      </svg>

      <div class="chart-card__legend">
        <span><i class="legend legend--actual" />Actual</span>
        <span><i class="legend legend--target" />Target</span>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import { productionTrend } from '@/lib/mes-data'
import BaseCard from './BaseCard.vue'

const maxValue = Math.max(...productionTrend.map((item) => Math.max(item.production, item.target)))
const minX = 72
const maxX = 716
const minY = 24
const maxY = 214

const chartPoints = computed(() =>
  productionTrend.map((point, index) => {
    const x = minX + ((maxX - minX) / (productionTrend.length - 1)) * index
    const mapY = (value) => maxY - ((value / maxValue) * (maxY - minY))

    return {
      ...point,
      x,
      productionY: mapY(point.production),
      targetY: mapY(point.target),
    }
  }),
)

const productionPoints = computed(() => chartPoints.value.map((point) => `${point.x},${point.productionY}`).join(' '))
const targetPoints = computed(() => chartPoints.value.map((point) => `${point.x},${point.targetY}`).join(' '))
const gridLines = [36, 84, 132, 180, 228]
</script>
