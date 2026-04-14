<template>
  <BaseCard title="Production Trend" subtitle="Last 8 hours">
    <div ref="chartCardRef" class="chart-card" @mouseleave="clearHoveredPoint">
      <div
        v-if="hoveredPoint"
        ref="tooltipRef"
        class="chart-card__tooltip"
        :style="tooltipStyle"
        role="status"
        aria-live="polite"
      >
        <p class="chart-card__tooltip-time">{{ hoveredPoint.time }}</p>
        <p class="chart-card__tooltip-row">
          <span class="chart-card__tooltip-series">
            <i class="legend legend--actual" />
            Actual
          </span>
          <strong>{{ formatValue(hoveredPoint.production) }}</strong>
        </p>
        <p class="chart-card__tooltip-row">
          <span class="chart-card__tooltip-series">
            <i class="legend legend--target" />
            Target
          </span>
          <strong>{{ formatValue(hoveredPoint.target) }}</strong>
        </p>
      </div>

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
        <line
          v-if="hoveredPoint"
          :x1="hoveredPoint.x"
          :x2="hoveredPoint.x"
          y1="24"
          y2="214"
          class="chart-card__guide"
        />
        <polyline :points="targetPoints" class="chart-card__line chart-card__line--target" />
        <polyline :points="productionPoints" class="chart-card__line chart-card__line--actual" />
        <g v-for="point in chartPoints" :key="point.time">
          <rect
            :x="point.hoverStart"
            y="20"
            :width="point.hoverWidth"
            height="240"
            class="chart-card__hover-zone"
            @mouseenter="setHoveredPoint(point, $event)"
            @mousemove="setHoveredPoint(point, $event)"
          />
          <circle
            :cx="point.x"
            :cy="point.targetY"
            :r="hoveredPoint?.time === point.time ? 5 : 4"
            class="chart-card__dot--target"
          />
          <circle
            :cx="point.x"
            :cy="point.productionY"
            :r="hoveredPoint?.time === point.time ? 5 : 4"
            class="chart-card__dot--actual"
          />
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
import { computed, ref } from 'vue'
import BaseCard from './BaseCard.vue'

const props = defineProps({
  points: { type: Array, default: () => [] },
})

const minX = 72
const maxX = 716
const minY = 24
const maxY = 214
const chartCardRef = ref(null)
const tooltipRef = ref(null)
const hoveredPoint = ref(null)
const tooltipPosition = ref({ x: 0, y: 0 })

const safePoints = computed(() => {
  if (props.points.length > 0) {
    return props.points
  }

  return [
    { time: '00:00', production: 0, target: 0 },
    { time: '01:00', production: 0, target: 0 },
  ]
})

const maxValue = computed(() => {
  const value = Math.max(...safePoints.value.map((item) => Math.max(item.production, item.target)))
  return value > 0 ? value : 1
})

const chartPoints = computed(() => {
  const basePoints = safePoints.value.map((point, index) => {
    const denominator = Math.max(1, safePoints.value.length - 1)
    const x = minX + ((maxX - minX) / denominator) * index
    const mapY = (value) => maxY - ((value / maxValue.value) * (maxY - minY))

    return {
      ...point,
      x,
      productionY: mapY(point.production),
      targetY: mapY(point.target),
    }
  })

  return basePoints.map((point, index) => {
    const previousX = index === 0 ? minX - 36 : basePoints[index - 1].x
    const nextX = index === basePoints.length - 1 ? maxX + 36 : basePoints[index + 1].x
    const hoverStart = index === 0 ? previousX : (previousX + point.x) / 2
    const hoverEnd = index === basePoints.length - 1 ? nextX : (point.x + nextX) / 2

    return {
      ...point,
      hoverStart,
      hoverWidth: hoverEnd - hoverStart,
    }
  })
})

const productionPoints = computed(() => chartPoints.value.map((point) => `${point.x},${point.productionY}`).join(' '))
const targetPoints = computed(() => chartPoints.value.map((point) => `${point.x},${point.targetY}`).join(' '))
const gridLines = [36, 84, 132, 180, 228]

const tooltipStyle = computed(() => {
  if (!hoveredPoint.value) {
    return {}
  }

  const chartWidth = chartCardRef.value?.clientWidth ?? 0
  const tooltipWidth = tooltipRef.value?.offsetWidth ?? 0
  const tooltipHeight = tooltipRef.value?.offsetHeight ?? 0
  let left = tooltipPosition.value.x + 16
  let top = tooltipPosition.value.y - 16

  if (chartWidth && tooltipWidth) {
    left = Math.min(left, chartWidth - tooltipWidth - 12)
  }

  left = Math.max(left, 12)

  if (tooltipHeight) {
    top = Math.max(top, tooltipHeight + 12)
  }

  return {
    left: `${left}px`,
    top: `${top}px`,
    transform: 'translateY(-100%)',
  }
})

function setHoveredPoint(point, event) {
  hoveredPoint.value = point

  const svgElement = event.currentTarget.ownerSVGElement
  if (!svgElement) {
    return
  }

  const bounds = svgElement.getBoundingClientRect()
  tooltipPosition.value = {
    x: event.clientX - bounds.left,
    y: event.clientY - bounds.top,
  }
}

function clearHoveredPoint() {
  hoveredPoint.value = null
}

function formatValue(value) {
  return value.toLocaleString()
}
</script>
