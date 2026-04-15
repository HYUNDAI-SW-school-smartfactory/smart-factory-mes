<template>
  <BaseCard :title="`${lineName} Process Layout`">
    <div class="line-layout-board" v-if="equipments.length">
      <div class="line-layout" role="list">
        <template v-for="(equipment, index) in orderedEquipments" :key="equipment.id">
          <button type="button" class="line-layout__station" role="listitem" @click="goToEquipment(equipment.id)">
            <span class="line-layout__status" :class="`line-layout__status--${equipment.status.toLowerCase()}`">
              {{ equipment.status }}
            </span>

            <div class="line-layout__illustration">
              <svg
                v-if="equipment.iconKind === 'coil'"
                viewBox="0 0 96 96"
                class="line-layout__svg"
                aria-hidden="true"
              >
                <circle cx="36" cy="42" r="14" />
                <circle cx="36" cy="42" r="6" class="line-layout__svg-cut" />
                <rect x="18" y="58" width="36" height="8" rx="4" />
                <rect x="58" y="24" width="10" height="42" rx="3" />
                <rect x="52" y="20" width="22" height="8" rx="3" />
              </svg>

              <svg
                v-else-if="equipment.iconKind === 'press1' || equipment.iconKind === 'press2' || equipment.iconKind === 'press3'"
                viewBox="0 0 96 96"
                class="line-layout__svg"
                aria-hidden="true"
              >
                <rect x="20" y="18" width="56" height="10" rx="3" />
                <rect x="28" y="28" width="10" height="30" rx="3" />
                <rect x="58" y="28" width="10" height="30" rx="3" />
                <rect x="34" y="42" width="28" height="10" rx="3" />
                <rect x="24" y="62" width="48" height="12" rx="4" />
                <circle cx="34" cy="78" r="5" />
                <circle cx="62" cy="78" r="5" />
              </svg>

              <svg
                v-else-if="equipment.iconKind === 'trim'"
                viewBox="0 0 96 96"
                class="line-layout__svg"
                aria-hidden="true"
              >
                <rect x="18" y="54" width="60" height="12" rx="4" />
                <rect x="26" y="26" width="12" height="28" rx="3" />
                <rect x="58" y="26" width="12" height="28" rx="3" />
                <path d="M38 28 L56 46" />
                <path d="M56 28 L38 46" />
                <circle cx="30" cy="74" r="5" />
                <circle cx="66" cy="74" r="5" />
              </svg>

              <svg
                v-else-if="equipment.iconKind === 'vision'"
                viewBox="0 0 96 96"
                class="line-layout__svg"
                aria-hidden="true"
              >
                <rect x="18" y="52" width="60" height="12" rx="4" />
                <rect x="26" y="22" width="22" height="20" rx="4" />
                <circle cx="37" cy="32" r="5" class="line-layout__svg-cut" />
                <rect x="56" y="24" width="16" height="24" rx="3" />
                <path d="M64 48 L74 58" />
                <circle cx="30" cy="74" r="5" />
                <circle cx="66" cy="74" r="5" />
              </svg>

              <svg
                v-else
                viewBox="0 0 96 96"
                class="line-layout__svg"
                aria-hidden="true"
              >
                <rect x="18" y="52" width="60" height="12" rx="4" />
                <rect x="26" y="22" width="16" height="22" rx="4" />
                <rect x="54" y="22" width="18" height="30" rx="4" />
                <path d="M42 34 C52 24, 66 24, 72 36" />
                <circle cx="30" cy="74" r="5" />
                <circle cx="66" cy="74" r="5" />
              </svg>
            </div>

            <p class="line-layout__name">{{ equipment.name }}</p>
            <p class="line-layout__caption">{{ equipment.caption }}</p>
          </button>

          <div
            v-if="index < orderedEquipments.length - 1"
            class="line-layout__arrow"
            aria-hidden="true"
          >
            <span class="line-layout__arrow-line" />
            <span class="line-layout__arrow-dot">></span>
          </div>
        </template>
      </div>
    </div>
    <div v-else class="empty-state">No equipment found</div>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import BaseCard from '@/components/dashboard/BaseCard.vue'

const props = defineProps({
  lineName: { type: String, default: 'Line' },
  equipments: { type: Array, required: true },
})

const router = useRouter()

const orderedEquipments = computed(() =>
  [...props.equipments]
    .sort((left, right) => (left.processOrder ?? 0) - (right.processOrder ?? 0))
    .map((equipment) => ({
      ...equipment,
      iconKind: createIconKind(equipment.name),
      caption: createCaption(equipment.name),
    })),
)

function goToEquipment(id) {
  router.push(`/equipment/${id}`)
}

function createIconKind(name) {
  if (name.includes('Coil')) return 'coil'
  if (name.includes('Press Machine 1')) return 'press1'
  if (name.includes('Press Machine 2')) return 'press2'
  if (name.includes('Press Machine 3')) return 'press3'
  if (name.includes('Trim')) return 'trim'
  if (name.includes('Vision')) return 'vision'
  return 'robot'
}

function createCaption(name) {
  if (name.includes('Coil')) return 'Raw sheet feed'
  if (name.includes('Press')) return 'Forming stage'
  if (name.includes('Trim')) return 'Edge trimming'
  if (name.includes('Vision')) return 'Surface check'
  if (name.includes('Robot')) return 'Transfer handoff'
  return 'Process station'
}
</script>
