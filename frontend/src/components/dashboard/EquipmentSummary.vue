<template>
  <BaseCard title="Equipment Summary">
    <div class="summary-grid">
      <article class="summary-stat">
        <div class="summary-stat__icon summary-stat__icon--run">RUN</div>
        <div>
          <p class="summary-stat__label">Running</p>
          <p class="summary-stat__value">{{ summary.running }}</p>
        </div>
      </article>

      <article class="summary-stat">
        <div class="summary-stat__icon summary-stat__icon--stop">STOP</div>
        <div>
          <p class="summary-stat__label">Stopped</p>
          <p class="summary-stat__value">{{ summary.stopped }}</p>
        </div>
      </article>
    </div>

    <div class="progress-block">
      <div class="progress-block__header">
        <span>Availability</span>
        <span class="progress-block__value">{{ runPercentage }}%</span>
      </div>
      <div class="progress-block__bar">
        <div class="progress-block__fill" :style="{ width: `${runPercentage}%` }" />
      </div>
    </div>

    <p class="summary-footnote">Total Equipment: <strong>{{ summary.total }}</strong></p>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from './BaseCard.vue'

const props = defineProps({
  summary: { type: Object, required: true },
})

const runPercentage = computed(() => {
  if (!props.summary.total) return 0
  return Math.round((props.summary.running / props.summary.total) * 100)
})
</script>
