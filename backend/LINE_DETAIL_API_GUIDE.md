# 라인 상세 조회 API 통합 가이드

## 백엔드 API 엔드포인트

### 1. 라인 기본 정보 조회 (기존)
```
GET /api/lines/{lineId}
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "lineId": 1,
    "lineCode": "LINE-001",
    "lineName": "Assembly Line A",
    "processName": "Assembly",
    "panelType": "LCD Panel",
    "description": "Main assembly line",
    "equipmentCount": 3,
    "processSteps": ["Soldering", "Assembly", "Testing"]
  }
}
```

### 2. 라인 상세 조회 (신규) ⭐
```
GET /api/lines/{lineId}/detail
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "lineId": 1,
    "lineCode": "LINE-001",
    "lineName": "Assembly Line A",
    "status": "RUN",
    "processName": "Assembly",
    "panelType": "LCD Panel",
    "description": "Main assembly line",
    "equipmentCount": 3,
    "processSteps": ["Soldering", "Assembly", "Testing"],
    
    // KPI 정보
    "production": 1250,
    "targetProduction": 1500,
    "achievementRate": 83.3,
    "uptime": 94.2,
    "defectRate": 0.8,
    
    // 관련 데이터
    "equipmentList": [
      {
        "equipmentId": 1,
        "equipmentName": "Robot Arm A1",
        "status": "RUN",
        "production": 420,
        "uph": 60,
        "uptime": 96.5,
        "lastUpdated": "2 min ago"
      }
    ],
    "alarmList": [
      {
        "alarmId": 1,
        "time": "14:32:15",
        "equipmentId": 1,
        "equipmentName": "Robot Arm A1",
        "message": "Motor overheated - Emergency stop activated",
        "severity": "critical"
      }
    ]
  }
}
```

## 프론트엔드 mes-data.js 수정

기존의 더미 함수들을 백엔드 API로 대체:

```javascript
// 변경 전 (더미 데이터)
export const getLineById = (id) => productionLines.find((line) => line.id === id)
export const getEquipmentByLineId = (lineId) => equipment.filter((item) => item.lineId === lineId)
export const getAlarmsByLineId = (lineId) => alarms.filter((alarm) => alarm.lineId === lineId)

// 변경 후 (API 연동)
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

export async function getLineById(id) {
  try {
    const response = await fetch(`${API_BASE_URL}/lines/${id}/detail`)
    const result = await response.json()
    if (result.success) {
      return result.data
    }
  } catch (error) {
    console.error('Failed to fetch line details:', error)
  }
  return null
}

export async function getEquipmentByLineId(lineId) {
  // lineById에서 이미 equipmentList를 포함하므로 별도 호출 불필요
  const line = await getLineById(lineId)
  return line ? line.equipmentList : []
}

export async function getAlarmsByLineId(lineId) {
  // lineById에서 이미 alarmList를 포함하므로 별도 호출 불필요
  const line = await getLineById(lineId)
  return line ? line.alarmList : []
}
```

## LineDetailView.vue 수정

```javascript
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const line = ref(null)
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    line.value = await getLineById(route.params.id)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
})

const lineEquipment = computed(() => line.value?.equipmentList || [])
const lineAlarms = computed(() => line.value?.alarmList || [])
const achievementRate = computed(() => line.value?.achievementRate || 0)
```

## 개발환경 설정

### application.properties
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/smart_factory_mes
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# API 응답 설정
server.servlet.context-path=/
```

### vite.config.js
```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

## 데이터베이스 초기화 스크립트 (선택사항)

```sql
-- Line 테이블
INSERT INTO line (id, code, name, process_name, panel_type, description, equipment_count, status, production, target_production, uptime, defect_rate)
VALUES 
  (1, 'LINE-001', 'Assembly Line A', 'Assembly', 'LCD Panel', 'Main assembly line', 3, 'RUN', 1250, 1500, 94.2, 0.8),
  (2, 'LINE-002', 'Assembly Line B', 'Assembly', 'LCD Panel', 'Secondary assembly line', 3, 'RUN', 980, 1200, 91.5, 1.2);

-- Equipment 테이블  
INSERT INTO equipment (id, name, line_id, status, production, uph, uptime, last_updated)
VALUES 
  (1, 'Robot Arm A1', 1, 'RUN', 420, 60, 96.5, '2 min ago'),
  (2, 'Robot Arm A2', 1, 'RUN', 415, 58, 94.2, '1 min ago');

-- Alarm 테이블
INSERT INTO alarm (id, time, line_id, equipment_id, message, severity)
VALUES 
  (1, NOW(), 1, 1, 'Motor overheated - Emergency stop activated', 'critical');
```

## 완성된 드릴다운 흐름

1. 📊 대시보드 → 라인 클릭
2. 🔍 LineDetailView 에 `lineId` 전달
3. 📡 `getLineById()` 호출 → `/api/lines/{lineId}/detail` 엔드포인트
4. 📈 KPI, 설비, 알람 데이터 한 번에 수신
5. 🎨 상세 페이지 렌더링

## 주요 특징

✅ **한 번의 API 호출로 모든 데이터 수신** (N+1 문제 해결)  
✅ **설비 목록과 알람 포함** (레이아웃과 정확히 매칭)  
✅ **달성률 자동 계산** (백엔드에서 처리)  
✅ **최근 알람 자동 정렬** (최대 6개)  
✅ **실시간 상태 반영 가능** (status 필드 추가)
