# 🏭 Smart Factory MES

프레스 공정 생산/설비 데이터를 실시간처럼 모니터링하는  
**스마트 팩토리 MES 대시보드 시스템**

> 생산/설비 데이터를 5초 주기로 시뮬레이션하여 WebSocket으로 전달하고  
> Vue 대시보드에서 실시간으로 시각화한 MES 시스템
---

## ⭐ 핵심 구현 포인트

- 시뮬레이터 기반 데이터 생성 → 실제 공장 데이터 없이도 MES 구조 구현
- 5초 주기 스케줄러 → 실시간에 가까운 데이터 흐름 구성
- WebSocket → 클라이언트 실시간 UI 반영
- 프론트/백엔드/DB 완전 분리 구조

---

## 🗄 1. 프로젝트 개요

Smart Factory MES는 제조 공정에서 발생하는 **생산, 설비, 알람 데이터를 통합 관리하고 시각화하는 시스템**입니다.

실제 공장 데이터를 사용할 수 없는 환경에서  
👉 **시뮬레이터 기반 더미 데이터 + WebSocket 실시간 전송 구조**를 통해  
현장과 유사한 모니터링 환경을 구현했습니다.

---

## 🎯 2. 프로젝트 목표

- 공장 전체 상태를 한눈에 볼 수 있는 대시보드 제공
- 설비 상태 및 비가동 상황을 실시간에 가깝게 모니터링
- 알람 발생 시 즉시 인지 가능한 UI/UX 제공
- 프론트/백엔드/DB 분리 구조를 통한 실제 서비스 수준 아키텍처 구현

---

## 🧩 3. 주요 기능

### 메인 대시보드
- 전체 KPI 조회 (생산량, 달성률, 가동률, 불량률)
- 시간대별 생산량 그래프
- 라인 상태 조회 (RUN / STOP / IDLE / ERROR)
- 설비 상태 요약
- 알람 리스트

### 라인 상세
- 공정 흐름 시각화
- 설비 배치 및 상태 확인

### 설비 상세
- 설비 상태 정보
- 상태 변경 이력
- 생산 관련 데이터

### 알람 시스템
- 이상 상황 발생 시 알람 표시
- 전역 오버레이 UI 제공

### 실시간 데이터 처리
- 시뮬레이터 기반 더미 데이터 생성
- 5초 주기 데이터 갱신
- WebSocket 기반 실시간 전송

---

## 🏗 4. 시스템 아키텍처
```
[ Client (Browser) ]
        ↓
[ Frontend (Vercel) - Vue ]
        ↓
[ Backend (Render) - Spring Boot ]
        ↓
[ Database (Aiven) - MySQL ]
```

---

## 🌐 5. 배포 환경

### Frontend
- 플랫폼: Vercel
- Vue 3 + Vite
- GitHub 연동 자동 배포 (CI/CD)

### Backend
- 플랫폼: Render
- Spring Boot (Java 17)
- GitHub 연동 자동 배포

### Database
- 플랫폼: Aiven
- MySQL (Managed DB)

---

## 🔄 6. CI/CD 흐름
```
GitHub Push
        ↓
Frontend → Vercel 자동 배포
Backend → Render 자동 배포
        ↓
Frontend → Backend API 호출
        ↓
Backend → Aiven MySQL 접근
```

---

## 🛠 7. 기술 스택

### Frontend
- Vue 3
- Vite
- Vue Router

### Backend
- Spring Boot
- MyBatis
- WebSocket
- Lombok

### Database
- MySQL

### Infra
- Vercel
- Render
- Aiven

---

## 📁 8. 디렉토리 구조
```
smart-factory-mes/
├─ frontend/
│ ├─ src/
│ │ ├─ components/
│ │ │ ├─ dashboard/
│ │ │ ├─ line/
│ │ │ └─ app/
│ │ ├─ views/
│ │ ├─ router/
│ │ └─ main.js
│
├─ backend/
│ ├─ src/main/java/com/smartfactory/mes/
│ │ ├─ auth/
│ │ ├─ global/
│ │ └─ simulation/
│ ├─ resources/
│ └─ sql/
```

---

## 📊 9. 데이터베이스 구조

- app_users : 사용자
- production_lines : 생산 라인
- equipments : 설비
- production_records : 생산 기록
- equipment_status_history : 설비 상태 이력
- alarm_histories : 알람 이력

---

## ⚙️ 10. 실행 방법

### Frontend
```bash
cd frontend
npm install
npm run dev
```
### Backend
```bash
cd backend
./mvnw spring-boot:run
```

## 🔐 11. 환경 변수
```env
DB_URL=jdbc:mysql://xxx.aivencloud.com:xxxxx/mes
DB_USERNAME=xxxx
DB_PASSWORD=xxxx

MES_SIMULATION_ENABLED=true
MES_SIMULATION_DELAY_MS=5000

PORT=8080
```

## 🌿 12. 브랜치 전략

- main : 배포 브랜치 (직접 push 금지)
- develop : 통합 개발 브랜치
- feature/{이슈번호}-{기능명}
- fix/{이슈번호}-{이슈명}

---

## 📝 13. Git 커밋 컨벤션

- feat: 기능 추가
- fix: 버그 수정
- refactor: 리팩토링
- docs: 문서 수정
- style: 코드 스타일
- test: 테스트 코드
- chore: 설정 변경


---

## 🔀 14. PR 컨벤션

`[이슈번호] type: 작업 내용`

- 리뷰 후 머지
- 기능 단위 PR
- 스크린샷 또는 테스트 결과 포함

---

## 📌 15. Jira 컨벤션

### 이슈 구조
- Epic → 기능 단위
- Story → 사용자 기능
- Task → 개발 작업
- Bug → 오류

### 흐름
- To Do → In Progress → Review → Done

---

## 👥 16. 팀원 역할

### 프론트엔드
- 대시보드 UI
- 라인 상세 UI
- 설비 상세 UI

### 백엔드 (API)
- 대시보드 / 라인 / 설비 조회 API
- DB 설계 및 MyBatis

### 백엔드 (공통)
- 로그인 / 회원가입
- 공통 응답 / 예외 처리

### 실시간 / 시뮬레이터
- 더미 데이터 생성
- 5초 주기 스케줄러
- WebSocket 브로드캐스트

---

## ⚠️ 17. 트러블슈팅

- Aiven MySQL → SSL 연결 필수
- Render → PORT 환경변수 사용 필요
- CORS → Vercel 도메인 허용 필요
- SQL 초기 스크립트 중복 정의 주의

---

## 💡 18. 개선 방향

- Redis 캐싱 도입
- SSE 기반 실시간 처리 개선
- 로그 모니터링
- 헬스 체크 API 추가

---

## 📅 19. 프로젝트 정보

- 기간: 5일 (MVP 프로젝트)
- 목적: 스마트 팩토리 MES 핵심 기능 구현
- 특징: 시뮬레이션 기반 실시간 시스템 설계 경험
