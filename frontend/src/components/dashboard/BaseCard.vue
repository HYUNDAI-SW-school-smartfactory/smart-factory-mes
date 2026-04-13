<template>
  <section class="panel-card">
    <header v-if="title || subtitle || $slots.header" class="panel-card__header">
      <slot name="header">
        <div>
          <h2 v-if="title" class="panel-card__title">{{ title }}</h2>
          <p v-if="subtitle" class="panel-card__subtitle">{{ subtitle }}</p>
        </div>
      </slot>
    </header>
    <div class="panel-card__body" :class="{ 'panel-card__body--flush': flush }">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  flush: { type: Boolean, default: false },
})
</script>
