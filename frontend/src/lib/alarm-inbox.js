const READ_ALARM_STORAGE_KEY = 'mes-read-alarm-ids'
let readAlarmIdsState = null

function normalizeAlarmId(alarmId) {
  if (alarmId === null || alarmId === undefined || alarmId === '') {
    return ''
  }

  return String(alarmId)
}

export function getReadAlarmIds() {
  if (readAlarmIdsState) {
    return readAlarmIdsState
  }

  const raw = window.localStorage.getItem(READ_ALARM_STORAGE_KEY)
  if (!raw) return []

  try {
    const parsed = JSON.parse(raw)
    readAlarmIdsState = Array.isArray(parsed) ? parsed.map(normalizeAlarmId).filter(Boolean) : []
    return readAlarmIdsState
  } catch {
    window.localStorage.removeItem(READ_ALARM_STORAGE_KEY)
    readAlarmIdsState = []
    return []
  }
}

export function persistReadAlarmIds(ids) {
  const normalized = Array.from(new Set(ids.map(normalizeAlarmId).filter(Boolean)))
  readAlarmIdsState = normalized
  window.localStorage.setItem(READ_ALARM_STORAGE_KEY, JSON.stringify(normalized))
  return normalized
}

export function markAlarmAsRead(alarmId) {
  const normalizedId = normalizeAlarmId(alarmId)
  if (!normalizedId) {
    return getReadAlarmIds()
  }

  return persistReadAlarmIds([...getReadAlarmIds(), normalizedId])
}

export function syncReadAlarmIds(alarms, visibleLimit = 6) {
  const visibleIds = new Set(
    alarms
      .slice(0, visibleLimit)
      .map((alarm) => normalizeAlarmId(alarm.id))
      .filter(Boolean),
  )

  const hiddenIds = alarms
    .map((alarm) => normalizeAlarmId(alarm.id))
    .filter((id) => id && !visibleIds.has(id))

  return persistReadAlarmIds([...getReadAlarmIds(), ...hiddenIds])
}

export function isAlarmRead(readIds, alarmId) {
  const normalizedId = normalizeAlarmId(alarmId)
  return normalizedId ? readIds.includes(normalizedId) : false
}
