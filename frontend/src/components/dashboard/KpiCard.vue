<template>
  <BaseCard class="kpi-card">
    <div class="kpi-card__row">
      <div class="kpi-card__content">
        <p class="kpi-card__label">{{ title }}</p>
        <div class="kpi-card__value-row">
          <span class="kpi-card__value" :class="variantClass">{{ value }}</span>
          <span v-if="unit" class="kpi-card__unit">{{ unit }}</span>
        </div>
        <div v-if="trend && trendValue" class="kpi-card__trend" :class="trendClass">
          <span>{{ trendSymbol }}</span>
          <span>{{ trendValue }}</span>
        </div>
      </div>
      <div v-if="iconLabel" class="kpi-card__icon">{{ iconLabel }}</div>
    </div>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from './BaseCard.vue'

const props = defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], required: true },
  unit: { type: String, default: '' },
  trend: { type: String, default: '' },
  trendValue: { type: String, default: '' },
  iconLabel: { type: String, default: '' },
  variant: { type: String, default: 'default' },
})

const variantClass = computed(() => `kpi-card__value--${props.variant}`)
const trendClass = computed(() => `kpi-card__trend--${props.trend}`)
const trendSymbol = computed(() => {
  if (props.trend === 'up') return '▲'
  if (props.trend === 'down') return '▼'
  return '•'
})
</script>
