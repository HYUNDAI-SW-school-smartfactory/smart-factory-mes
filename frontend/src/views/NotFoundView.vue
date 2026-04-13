<template>
  <main class="page-shell page-shell--centered">
    <div class="page-container page-container--tiny not-found">
      <p class="not-found__eyebrow">404</p>
      <h1 class="not-found__title">Page not found</h1>
      <p class="not-found__text">The MES screen you requested does not exist in this Vue port.</p>
      <RouterLink to="/" class="text-link">Return to dashboard</RouterLink>
    </div>
  </main>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
