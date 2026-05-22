# 📈 주식 포트폴리오 관리 서비스 — 프로젝트 기획

> 이력서용 포트폴리오 프로젝트. 단순 CRUD를 넘어 실무 운영 감각을 증명하는 것이 목표.

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 서비스명 | StockFolio (가칭) |
| 유형 | 웹 서비스 (백엔드 중심 / 약식 풀스택) |
| 목표 | 실제 배포 및 운영, 이력서 증빙 가능한 수치 확보 |
| 기간 | 1~2개월 (설계 → 개발 → 배포 → 운영) |

### 핵심 기능

- 주식 종목 검색 및 관심종목 등록
- 포트폴리오 구성 및 수익률 계산
- 목표가 / 손절가 도달 시 알림 (이메일 or Slack)
- 관리자 대시보드 (유저 현황, 에러율, 트래픽)

---

## 2. 기술 스택

### Backend

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| View | Thymeleaf |
| ORM | Spring Data JPA + QueryDSL |
| DB | MySQL 8.x |
| Cache | Redis (주식 시세 캐싱, TTL 30초) |
| Queue | RabbitMQ (알림 비동기 처리) |
| Auth | Spring Security + JWT |

### Infra / Ops

| 분류 | 기술 |
|------|------|
| Container | Docker + Docker Compose |
| Monitoring | Prometheus + Grafana |
| Logging | Logback + MDC (traceId) |
| API 문서 | SpringDoc (OpenAPI 3.0) |
| 부하 테스트 | k6 |
| 배포 | AWS EC2 or Oracle Cloud (무료) |

---

## 3. 프로젝트 선정 기준 (4대 원칙)

### ① 운영 가능 (Operability)
- Spring Actuator + Micrometer → Prometheus 메트릭 수집
- Grafana 대시보드: 요청 수, 응답 시간, 에러율, JVM 지표
- 구조화 로깅: MDC로 traceId 심어 요청 추적 가능
- Slack/이메일 알림: 에러 임계치 초과 시 자동 통보

### ② 확장 가능 (Scalability)
- Redis: 외부 주식 API 호출 최소화, 시세 데이터 캐싱
- RabbitMQ: 알림 발송을 비동기 처리, 서비스 간 결합도 감소
- Docker Compose: 전체 인프라를 코드로 관리 (IaC 개념)
- Stateless 설계: JWT 인증으로 수평 확장 가능한 구조

### ③ 계약 우선 (Contract-First)
- OpenAPI 3.0 스펙을 먼저 작성 후 개발
- SpringDoc으로 스펙 자동 동기화 및 Swagger UI 배포
- 클라이언트 코드 자동 생성 가능한 형태 유지

### ④ 증빙 용이 (Evidence-Based)
- SLO 정의: p99 응답시간 ≤ 500ms, 가용성 ≥ 99.5%
- k6 부하 테스트: VU(Virtual User) 시나리오 작성 및 결과 문서화
- Grafana 대시보드 스크린샷 README 첨부
- GitHub README에 아키텍처 다이어그램 포함

---

## 4. 로드맵

### 1~2주차 — 기반 설계

- [ ] 도메인 모델 확정 및 ERD 작성
- [ ] OpenAPI 3.0 스펙 Contract-First 작성
- [ ] Docker Compose 환경 구성 (MySQL + Redis + RabbitMQ)
- [ ] Spring Boot 프로젝트 초기 세팅
- [ ] GitHub 레포 생성, 브랜치 전략 수립 (main / develop / feature/*)

### 3~4주차 — 핵심 기능 구현

- [ ] 회원가입 / 로그인 (JWT + Spring Security)
- [ ] 포트폴리오 CRUD
- [ ] 주식 API 연동 + Redis 캐싱 레이어
- [ ] 관심종목 등록/해제
- [ ] JPA 연관관계 매핑 + N+1 해결 (fetch join / batch size)
- [ ] 전역 예외 처리 + 공통 응답 형식 정의

### 5~6주차 — 운영 인프라 구축

- [ ] Prometheus + Grafana 연동
- [ ] 구조화 로깅 (Logback + MDC traceId)
- [ ] RabbitMQ 알림 파이프라인 (가격 알림 → 이메일/Slack)
- [ ] Thymeleaf 관리자 대시보드 페이지
- [ ] SpringDoc Swagger UI 설정

### 7~8주차 — 배포 + 증빙

- [ ] EC2 or Oracle Cloud 서버 세팅
- [ ] Docker Compose로 전체 스택 배포
- [ ] k6 부하 테스트 시나리오 작성 및 실행
- [ ] SLO 달성 여부 측정 + 결과 문서화
- [ ] GitHub README 정리 (아키텍처 다이어그램, 성능 결과)

---

## 5. 아키텍처 개요

```
Client (Browser)
    │
    ▼
[Thymeleaf + REST API]
    │
[Spring Boot Application]
    ├── Spring Security (JWT)
    ├── Spring Data JPA ──→ MySQL
    ├── RedisTemplate ──────→ Redis (시세 캐시)
    ├── RabbitTemplate ─────→ RabbitMQ → [알림 Consumer]
    └── WebClient ──────────→ 외부 주식 API
    │
[Prometheus] ← Actuator/Micrometer
    │
[Grafana Dashboard]
```

---

## 6. 외부 주식 API 후보

| API | 특징 | 무료 한도 |
|-----|------|-----------|
| Alpha Vantage | 미국 주식, 간단한 REST | 25 req/day |
| Yahoo Finance (비공식) | 한국 포함, 불안정 | 제한 없음(비공식) |
| KIS Developers (한국투자증권) | 국내 주식, 공식 API | 무료 모의계좌 제공 |
| DART (전자공시) | 재무제표, 공시 정보 | 무료 |

> 추천: KIS Developers 모의계좌 API + Alpha Vantage 조합

---

## 7. 주요 의사결정 로그

| 날짜 | 결정 내용 | 이유 |
|------|-----------|------|
| 2026-05-22 | JSP → Thymeleaf 변경 | Spring Boot 네이티브 지원, 현재 선호도 높음 |
| 2026-05-22 | RabbitMQ 채택 | Kafka 대비 낮은 러닝커브, 1인 프로젝트에 적합 |
| 2026-05-22 | Contract-First 방식 채택 | API 명세를 먼저 확정해 개발 방향 고정 |
| 2026-05-22 | JWT Stateless 인증 | 수평 확장 가능한 구조 확보 |

---

*최초 작성: 2026-05-22*
