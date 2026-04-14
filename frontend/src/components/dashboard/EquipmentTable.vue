<template>
  <BaseCard :title="title" flush>
    <div class="data-table">
      <table>
        <thead>
          <tr>
            <th>Equipment</th>
            <th>Status</th>
            <th class="align-right">Production</th>
            <th class="align-right">Uptime</th>
            <th />
          </tr>
        </thead>
        <tbody>
          <tr v-if="items.length === 0">
            <td colspan="5" class="empty-state">No equipment found</td>
          </tr>
          <tr v-for="item in items" :key="item.id" class="table-link" @click="goToEquipment(item.id)">
            <td class="cell-strong">{{ item.name }}</td>
            <td><StatusBadge :status="item.status" size="sm" /></td>
            <td class="align-right">{{ item.production.toLocaleString() }}</td>
            <td class="align-right">{{ item.uptime }}%</td>
            <td class="align-right">Open</td>
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
  items: { type: Array, required: true },
  title: { type: String, default: 'Equipment List' },
})

const router = useRouter()
const goToEquipment = (id) => router.push(`/equipment/${id}`)
</script>
