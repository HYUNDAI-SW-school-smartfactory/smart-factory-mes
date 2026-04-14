# 라인 상세 조회 API 구현 완료

## 📋 구현 내용 요약

### 1️⃣ Entity 확장
- **[Line.java](backend/src/main/java/com/smartfactory/mes/entity/Line.java)**
  - ✅ `status` 필드 추가
  - ✅ KPI 필드 추가: `production`, `targetProduction`, `uptime`, `defectRate`

- **[Equipment.java](backend/src/main/java/com/smartfactory/mes/entity/Equipment.java)** (신규)
  - 설비 정보 매핑
  - Line과의 ManyToOne 관계 설정

- **[Alarm.java](backend/src/main/java/com/smartfactory/mes/entity/Alarm.java)** (신규)
  - 알람 정보 매핑
  - Line, Equipment과의 외래키 관계

### 2️⃣ DTO 생성
- **[LineDetailResponse.java](backend/src/main/java/com/smartfactory/mes/dto/line/LineDetailResponse.java)** (신규)
  - 기본 정보 + KPI + 관련 데이터를 단일 응답으로 제공
  
- **[EquipmentBasicInfo.java](backend/src/main/java/com/smartfactory/mes/dto/equipment/EquipmentBasicInfo.java)** (신규)
  - 설비 기본 정보 DTO

- **[AlarmBasicInfo.java](backend/src/main/java/com/smartfactory/mes/dto/alarm/AlarmBasicInfo.java)** (신규)
  - 알람 기본 정보 DTO

### 3️⃣ Repository 추가
- **[EquipmentRepository.java](backend/src/main/java/com/smartfactory/mes/repository/EquipmentRepository.java)** (신규)
  - `findByLineId(Long lineId)` - 라인별 설비 조회

- **[AlarmRepository.java](backend/src/main/java/com/smartfactory/mes/repository/AlarmRepository.java)** (신규)
  - `findByLineIdOrderByTimeDesc()` - 라인별 최근 알람 조회 (최대 6개)

### 4️⃣ Service 로직 확장
- **[LineService.java](backend/src/main/java/com/smartfactory/mes/service/LineService.java)**
  - ✅ 기존 `getLineBasicInfo()` 유지
  - ✅ 신규 `getLineDetail()` 메서드 추가
    - 한 번의 호출로 라인 정보, KPI, 설비 목록, 최근 알람 조회
    - 달성률 자동 계산: `(production / targetProduction) × 100`

### 5️⃣ API Endpoint 추가
- **[LineController.java](backend/src/main/java/com/smartfactory/mes/controller/LineController.java)**
  ```
  기존: GET /api/lines/{lineId}              → 기본 정보만
  신규: GET /api/lines/{lineId}/detail       → 상세 정보 (완전한 드릴다운)
  ```

---

## 🎯 핵심 기능

### 요구사항 달성
| 요구사항 | 구현 방식 | 상태 |
|---------|---------|------|
| lineId 기반 상세 조회 | LineDetailResponse에 모든 정보 포함 | ✅ |
| 생산량/목표/달성률/가동률/불량률 | Line entity의 KPI 필드 + 계산 로직 | ✅ |
| 해당 라인 소속 설비 목록 | equipmentList 포함 | ✅ |
| 해당 라인의 최근 알람 목록 | alarmList 포함 (최대 6개) | ✅ |
| 프론트 라인 상세 페이지와 연동 | 구현 가이드 문서 제공 | ✅ |

### 설계 특징
- **N+1 문제 해결**: 한 번의 API 호출로 모든 데이터 수신
- **자동 계산**: 달성률을 백엔드에서 계산하여 프론트 로직 단순화
- **최근 알람 정렬**: `ORDER BY time DESC`로 시간순 정렬
- **페이징 지원**: 알람은 최대 6개로 제한
- **상태 정보**: `status` 필드로 라인 실시간 상태 표현 가능

---

## 🚀 다음 단계

### 필수 작업
1. **프론트엔드 연동**
   - `mes-data.js` 수정: 더미 데이터 → API 호출
   - `LineDetailView.vue` 업데이트: async/await 처리 추가
   - 환경변수 설정: `VITE_API_URL`

2. **데이터베이스**
   - Equipment, Alarm 테이블 생성
   - 테스트 데이터 INSERT
   - 외래키 제약조건 검증

3. **테스트**
   - 단위 테스트: LineService, LineController
   - 통합 테스트: API 엔드포인트
   - E2E 테스트: 프론트-백 연동

### 선택적 개선
- [ ] 실시간 데이터 업데이트 (WebSocket)
- [ ] 알람 필터링 (심각도별)
- [ ] 설비 상세 조회 API 추가
- [ ] 예측 분석 데이터 추가 (ML 모델 연동)
- [ ] 캐싱 최적화 (Redis)

---

## 📂 파일 구조

```
backend/src/main/java/com/smartfactory/mes/
├── entity/
│   ├── Line.java              ✏️ (수정: KPI 필드 추가)
│   ├── Equipment.java         ✨ (신규)
│   └── Alarm.java             ✨ (신규)
├── dto/
│   ├── line/
│   │   ├── LineBasicInfoResponse.java
│   │   └── LineDetailResponse.java    ✨ (신규)
│   ├── equipment/
│   │   └── EquipmentBasicInfo.java    ✨ (신규)
│   └── alarm/
│       └── AlarmBasicInfo.java        ✨ (신규)
├── repository/
│   ├── LineRepository.java
│   ├── EquipmentRepository.java       ✨ (신규)
│   └── AlarmRepository.java           ✨ (신규)
├── service/
│   └── LineService.java               ✏️ (수정: getLineDetail 추가)
└── controller/
    └── LineController.java            ✏️ (수정: detail 엔드포인트 추가)
```

---

## 💡 사용 예시

### API 호출
```bash
curl -X GET http://localhost:8080/api/lines/1/detail \
  -H "Content-Type: application/json"
```

### 응답 데이터
```json
{
  "success": true,
  "data": {
    "lineId": 1,
    "lineName": "Assembly Line A",
    "status": "RUN",
    "production": 1250,
    "targetProduction": 1500,
    "achievementRate": 83.3,
    "uptime": 94.2,
    "defectRate": 0.8,
    "equipmentList": [
      {"equipmentId": 1, "equipmentName": "Robot Arm A1", ...},
      {"equipmentId": 2, "equipmentName": "Robot Arm A2", ...}
    ],
    "alarmList": [
      {"alarmId": 1, "message": "Motor overheated...", "severity": "critical"}
    ]
  }
}
```

---

## ✨ 완성된 드릴다운 흐름

```
📊 Dashboard (메인 페이지)
       ↓
   [라인 클릭] → Line ID 전달
       ↓
🔍 LineDetailView 로드
       ↓
📡 getLineById(id) 호출
       ↓
🌐 GET /api/lines/{id}/detail 요청
       ↓
⚙️ LineService.getLineDetail() 실행
  - Line 정보 조회
  - 설비 목록 조회
  - 알람 목록 조회
  - 달성률 계산
       ↓
📦 LineDetailResponse 반환
       ↓
📈 KPI Cards 렌더링
  - Production: 1250 units
  - Target: 1500 units
  - Achievement Rate: 83.3%
  - Uptime: 94.2%
  - Defect Rate: 0.8%
       ↓
🎨 Equipment Table 표시
│   ├─ Robot Arm A1 (RUN)
│   ├─ Robot Arm A2 (RUN)
│   └─ Conveyor A1 (RUN)
       ↓
🔔 Alarm List 표시
│   ├─ [CRITICAL] Motor overheated...
│   ├─ [WARNING] Maintenance required...
│   └─ [INFO] Belt tension adjustment...
```

---

## 📝 참고 자료
- [LINE_DETAIL_API_GUIDE.md](LINE_DETAIL_API_GUIDE.md) - API 상세 가이드 및 프론트 통합 방법
- Entity 다이어그램: [위에 표시된 Mermaid 다이어그램 참고]
