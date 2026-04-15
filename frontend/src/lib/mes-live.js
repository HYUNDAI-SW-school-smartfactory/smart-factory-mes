const BACKEND_ORIGIN = import.meta.env.VITE_BACKEND_ORIGIN

function resolveWebSocketBaseUrl() {
  if (BACKEND_ORIGIN) {
    return BACKEND_ORIGIN.replace(/^http/, 'ws')
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const hostname = window.location.hostname
  const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1'
  const currentPort = window.location.port
  const port = isLocalhost && currentPort !== '8080' ? '8080' : currentPort
  return `${protocol}//${hostname}${port ? `:${port}` : ''}`
}

function createEmptyDashboardState() {
  return {
    kpis: {
      totalProduction: 0,
      totalTarget: 0,
      achievementRate: 0,
      avgUptime: 0,
      avgDefectRate: 0,
    },
    productionTrend: [],
    productionLines: [],
    summary: {
      running: 0,
      stopped: 0,
      total: 0,
    },
    alarms: [],
    generatedAt: null,
  }
}

export function connectDashboardStream({ onData, onError, onStatus } = {}) {
  return openRealtimeChannel('/ws/dashboard', {
    channel: 'dashboard',
    onData: (payload) => onData?.(mapDashboardPayload(payload)),
    onError,
    onStatus,
  })
}

export function connectLineStream(lineId, { onData, onError, onStatus } = {}) {
  return openRealtimeChannel(`/ws/line?id=${lineId}`, {
    channel: 'line',
    onData: (payload) => onData?.(mapLinePayload(payload)),
    onError,
    onStatus,
  })
}

export function connectEquipmentStream(equipmentId, { onData, onError, onStatus } = {}) {
  return openRealtimeChannel(`/ws/equipment?id=${equipmentId}`, {
    channel: 'equipment',
    onData: (payload) => onData?.(mapEquipmentPayload(payload)),
    onError,
    onStatus,
  })
}

export function getEmptyDashboardState() {
  return createEmptyDashboardState()
}

function openRealtimeChannel(path, { channel, onData, onError, onStatus }) {
  const url = `${resolveWebSocketBaseUrl()}${path}`
  let socket = null
  let reconnectTimer = null
  let manuallyClosed = false

  const connect = () => {
    onStatus?.('connecting')
    socket = new WebSocket(url)

    socket.addEventListener('open', () => {
      onStatus?.('open')
    })

    socket.addEventListener('message', (event) => {
      try {
        const payload = JSON.parse(event.data)
        if (payload.channel !== channel) {
          return
        }

        if (payload.error) {
          onError?.(payload.error)
          return
        }

        onData?.(payload.data)
      } catch (error) {
        onError?.({ code: 'SOCKET_PARSE_ERROR', message: 'Failed to parse realtime payload.', cause: error })
      }
    })

    socket.addEventListener('close', () => {
      onStatus?.('closed')
      if (!manuallyClosed) {
        reconnectTimer = window.setTimeout(connect, 1500)
      }
    })

    socket.addEventListener('error', () => {
      onStatus?.('error')
    })
  }

  connect()

  return () => {
    manuallyClosed = true
    if (reconnectTimer) {
      window.clearTimeout(reconnectTimer)
    }
    socket?.close()
  }
}

function mapDashboardPayload(payload) {
  return {
    kpis: payload.kpis,
    productionTrend: (payload.productionTrend ?? []).map((point) => ({
      time: point.time,
      production: point.production,
      target: point.target,
    })),
    productionLines: (payload.lines ?? []).map((line) => ({
      id: line.lineId,
      name: line.lineName,
      status: line.currentStatus,
      production: line.production,
      targetProduction: line.targetProduction,
      uptime: line.uptime,
      defectRate: line.defectRate,
    })),
    summary: {
      running: payload.equipmentSummary?.running ?? 0,
      stopped: payload.equipmentSummary?.stopped ?? 0,
      total: payload.equipmentSummary?.total ?? 0,
    },
    alarms: mapAlarms(payload.recentAlarms),
    generatedAt: payload.generatedAt,
  }
}

function mapLinePayload(payload) {
  return {
    line: {
      id: payload.lineId,
      name: payload.lineName,
      status: payload.currentStatus,
      production: payload.kpis.production,
      targetProduction: payload.kpis.targetProduction,
      uptime: payload.kpis.uptime,
      defectRate: payload.kpis.defectRate,
    },
    equipments: (payload.equipments ?? []).map((equipment) => ({
      id: equipment.equipmentId,
      lineId: payload.lineId,
      code: equipment.equipmentCode,
      name: equipment.equipmentName,
      type: equipment.equipmentType,
      processOrder: equipment.processOrder,
      status: equipment.currentStatus,
      production: equipment.production,
      uph: equipment.uph,
      uptime: equipment.uptime,
      lastUpdated: formatRelativeTime(equipment.updatedAt),
    })),
    alarms: mapAlarms(payload.alarms),
    generatedAt: payload.generatedAt,
  }
}

function mapEquipmentPayload(payload) {
  return {
    item: {
      id: payload.equipmentId,
      lineId: payload.lineId,
      lineName: payload.lineName,
      name: payload.equipmentName,
      status: payload.currentStatus,
      production: payload.kpis.production,
      uph: payload.kpis.uph,
      uptime: payload.kpis.uptime,
      lastUpdated: formatRelativeTime(payload.generatedAt),
    },
    alarms: mapAlarms(payload.alarms),
    generatedAt: payload.generatedAt,
  }
}

function mapAlarms(alarms = []) {
  return alarms.map((alarm) => ({
    id: alarm.alarmId,
    time: formatClockTime(alarm.createdAt),
    lineId: alarm.lineId,
    lineName: alarm.lineName,
    equipmentId: alarm.equipmentId,
    equipmentName: alarm.equipmentName ?? '-',
    alarmType: alarm.alarmType,
    message: alarm.message,
    severity: mapSeverity(alarm.severity),
  }))
}

function mapSeverity(severity) {
  if (severity === 'CRITICAL') return 'critical'
  if (severity === 'HIGH' || severity === 'MEDIUM') return 'warning'
  return 'info'
}

function formatClockTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

function formatRelativeTime(value) {
  if (!value) return '-'

  const date = new Date(value)
  const diffSeconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000))

  if (diffSeconds < 60) return `${diffSeconds} sec ago`
  if (diffSeconds < 3600) return `${Math.floor(diffSeconds / 60)} min ago`
  return `${Math.floor(diffSeconds / 3600)} hr ago`
}
