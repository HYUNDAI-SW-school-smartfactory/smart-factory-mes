export const productionLines = [
  { id: 'line-1', name: 'Assembly Line A', status: 'RUN', production: 1250, targetProduction: 1500, uptime: 94.2, defectRate: 0.8 },
  { id: 'line-2', name: 'Assembly Line B', status: 'RUN', production: 980, targetProduction: 1200, uptime: 91.5, defectRate: 1.2 },
  { id: 'line-3', name: 'Packaging Line 1', status: 'STOP', production: 450, targetProduction: 800, uptime: 56.3, defectRate: 2.1 },
  { id: 'line-4', name: 'Packaging Line 2', status: 'RUN', production: 720, targetProduction: 800, uptime: 90.0, defectRate: 0.5 },
  { id: 'line-5', name: 'Quality Check', status: 'RUN', production: 3200, targetProduction: 3500, uptime: 97.8, defectRate: 0.3 },
]

export const equipment = [
  { id: 'eq-1', lineId: 'line-1', lineName: 'Assembly Line A', name: 'Robot Arm A1', status: 'RUN', production: 420, uph: 60, uptime: 96.5, lastUpdated: '2 min ago' },
  { id: 'eq-2', lineId: 'line-1', lineName: 'Assembly Line A', name: 'Robot Arm A2', status: 'RUN', production: 415, uph: 58, uptime: 94.2, lastUpdated: '1 min ago' },
  { id: 'eq-3', lineId: 'line-1', lineName: 'Assembly Line A', name: 'Conveyor A1', status: 'RUN', production: 415, uph: 62, uptime: 92.0, lastUpdated: '30 sec ago' },
  { id: 'eq-4', lineId: 'line-2', lineName: 'Assembly Line B', name: 'Robot Arm B1', status: 'RUN', production: 320, uph: 52, uptime: 90.1, lastUpdated: '1 min ago' },
  { id: 'eq-5', lineId: 'line-2', lineName: 'Assembly Line B', name: 'Robot Arm B2', status: 'STOP', production: 280, uph: 0, uptime: 85.4, lastUpdated: '15 min ago' },
  { id: 'eq-6', lineId: 'line-2', lineName: 'Assembly Line B', name: 'Conveyor B1', status: 'RUN', production: 380, uph: 55, uptime: 99.0, lastUpdated: '45 sec ago' },
  { id: 'eq-7', lineId: 'line-3', lineName: 'Packaging Line 1', name: 'Packer P1', status: 'STOP', production: 150, uph: 0, uptime: 45.2, lastUpdated: '32 min ago' },
  { id: 'eq-8', lineId: 'line-3', lineName: 'Packaging Line 1', name: 'Packer P2', status: 'STOP', production: 150, uph: 0, uptime: 50.1, lastUpdated: '32 min ago' },
  { id: 'eq-9', lineId: 'line-3', lineName: 'Packaging Line 1', name: 'Sealer S1', status: 'STOP', production: 150, uph: 0, uptime: 73.6, lastUpdated: '32 min ago' },
  { id: 'eq-10', lineId: 'line-4', lineName: 'Packaging Line 2', name: 'Packer P3', status: 'RUN', production: 240, uph: 40, uptime: 92.3, lastUpdated: '20 sec ago' },
  { id: 'eq-11', lineId: 'line-4', lineName: 'Packaging Line 2', name: 'Packer P4', status: 'RUN', production: 240, uph: 42, uptime: 88.7, lastUpdated: '15 sec ago' },
  { id: 'eq-12', lineId: 'line-4', lineName: 'Packaging Line 2', name: 'Sealer S2', status: 'RUN', production: 240, uph: 38, uptime: 89.0, lastUpdated: '10 sec ago' },
  { id: 'eq-13', lineId: 'line-5', lineName: 'Quality Check', name: 'Scanner Q1', status: 'RUN', production: 1100, uph: 150, uptime: 98.5, lastUpdated: '5 sec ago' },
  { id: 'eq-14', lineId: 'line-5', lineName: 'Quality Check', name: 'Scanner Q2', status: 'RUN', production: 1050, uph: 148, uptime: 97.2, lastUpdated: '8 sec ago' },
  { id: 'eq-15', lineId: 'line-5', lineName: 'Quality Check', name: 'Scanner Q3', status: 'RUN', production: 1050, uph: 145, uptime: 97.7, lastUpdated: '3 sec ago' },
]

export const alarms = [
  { id: 'alm-1', time: '14:32:15', lineId: 'line-3', lineName: 'Packaging Line 1', equipmentId: 'eq-7', equipmentName: 'Packer P1', message: 'Motor overheated - Emergency stop activated', severity: 'critical' },
  { id: 'alm-2', time: '14:32:10', lineId: 'line-3', lineName: 'Packaging Line 1', equipmentId: 'eq-8', equipmentName: 'Packer P2', message: 'Motor overheated - Emergency stop activated', severity: 'critical' },
  { id: 'alm-3', time: '14:31:58', lineId: 'line-3', lineName: 'Packaging Line 1', equipmentId: 'eq-9', equipmentName: 'Sealer S1', message: 'Upstream equipment stopped', severity: 'warning' },
  { id: 'alm-4', time: '14:15:22', lineId: 'line-2', lineName: 'Assembly Line B', equipmentId: 'eq-5', equipmentName: 'Robot Arm B2', message: 'Maintenance required - Scheduled stop', severity: 'warning' },
  { id: 'alm-5', time: '13:45:00', lineId: 'line-1', lineName: 'Assembly Line A', equipmentId: 'eq-3', equipmentName: 'Conveyor A1', message: 'Belt tension adjustment needed', severity: 'info' },
  { id: 'alm-6', time: '12:30:45', lineId: 'line-5', lineName: 'Quality Check', equipmentId: 'eq-13', equipmentName: 'Scanner Q1', message: 'Calibration reminder', severity: 'info' },
]

export const productionTrend = [
  { time: '07:00', production: 320, target: 400 },
  { time: '08:00', production: 680, target: 800 },
  { time: '09:00', production: 1050, target: 1200 },
  { time: '10:00', production: 1420, target: 1600 },
  { time: '11:00', production: 1780, target: 2000 },
  { time: '12:00', production: 2100, target: 2400 },
  { time: '13:00', production: 2450, target: 2800 },
  { time: '14:00', production: 2820, target: 3200 },
]

export function getKPIs() {
  const totalProduction = productionLines.reduce((sum, line) => sum + line.production, 0)
  const totalTarget = productionLines.reduce((sum, line) => sum + line.targetProduction, 0)
  const achievementRate = Number(((totalProduction / totalTarget) * 100).toFixed(1))
  const avgUptime = Number((productionLines.reduce((sum, line) => sum + line.uptime, 0) / productionLines.length).toFixed(1))
  const avgDefectRate = Number((productionLines.reduce((sum, line) => sum + line.defectRate, 0) / productionLines.length).toFixed(2))

  return { totalProduction, totalTarget, achievementRate, avgUptime, avgDefectRate }
}

export function getEquipmentSummary() {
  const running = equipment.filter((item) => item.status === 'RUN').length
  const stopped = equipment.filter((item) => item.status === 'STOP').length
  return { running, stopped, total: equipment.length }
}

export const getLineById = (id) => productionLines.find((line) => line.id === id)
export const getEquipmentByLineId = (lineId) => equipment.filter((item) => item.lineId === lineId)
export const getEquipmentById = (id) => equipment.find((item) => item.id === id)
export const getAlarmsByLineId = (lineId) => alarms.filter((alarm) => alarm.lineId === lineId)
export const getAlarmsByEquipmentId = (equipmentId) => alarms.filter((alarm) => alarm.equipmentId === equipmentId)
