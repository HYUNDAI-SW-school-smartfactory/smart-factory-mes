<template>
  <BaseCard title="Line Status" flush>
    <div class="data-table">
      <table>
        <thead>
          <tr>
            <th>Line Name</th>
            <th>Status</th>
            <th class="align-right">Production</th>
            <th class="align-right">Uptime</th>
            <th />
          </tr>
        </thead>
        <tbody>
          <tr v-for="line in lines" :key="line.id" class="table-link" @click="goToLine(line.id)">
            <td class="cell-strong">{{ line.name }}</td>
            <td><StatusBadge :status="line.status" size="sm" /></td>
            <td class="align-right">{{ line.production.toLocaleString() }}</td>
            <td class="align-right">{{ line.uptime }}%</td>
            <td class="align-right">›</td>
          </tr>
        </tbody>
      </table>
    </div>
  </BaseCard>
</template>

<script setup>
import { useRouter } from 'vue-router'
import BaseCard from './BaseCard.vue'
import StatusBadge from './StatusBadge.vue'

defineProps({
  lines: { type: Array, required: true },
})

const router = useRouter()
const goToLine = (id) => router.push(`/line/${id}`)
</script>
