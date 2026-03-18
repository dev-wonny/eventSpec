# Event Platform Backend

출석체크 1차 범위를 기준으로 시작한 Event Platform Backend 프로젝트다.

현재 구현 범위:

- Spring Boot 3.3.2
- Java 21
- JPA 저장 + QueryDSL 조회
- Attendance `POST /event/v1/events/{eventId}/rounds/{roundId}/entries`
- Event detail `GET /event/v1/events/{eventId}`
- `BaseResponse`, `CommonCode`, `BusinessException`, `GlobalExceptionHandler`
- `requestId` 필터 + access log + OpenAPI + Actuator
- Point API timeout / idempotency 정책 반영

## Run

### 1. PostgreSQL 실행

```bash
docker compose up -d postgres
```

초기 schema는 아래 SQL로 적재된다.

- `docs/spec/features/attendance-check/event-platform-schema-draft.sql`

### 2. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

기본 DB 연결 정보:

- URL: `jdbc:postgresql://localhost:5432/event`
- username: `postgres`
- password: `postgres`

환경변수로 변경 가능:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `POINT_API_BASE_URL`
- `POINT_API_GRANT_PATH`

`local` profile에서는 내부 point mock endpoint(`/mock/points/grants`)를 사용한다.

### 3. 로컬 seed 데이터

docker init SQL에 아래 로컬 seed가 함께 적재된다.

- `docker/postgres/init/02-local-seed.sql`

기본 샘플 데이터:

- 이벤트 ID: `1`
- 이벤트명: `2026년 3월 출석체크 이벤트`
- 테스트 회원 ID: `1001`
- 3월 1일, 2일, 5일 출석 이력 선적재
- 모든 회차에 `3월 출석 포인트 30P` 보상 매핑

### 4. 샘플 호출

이벤트 상세 조회:

```bash
curl -s http://localhost:8080/event/v1/events/1
curl -s -H 'X-Member-Id: 1001' http://localhost:8080/event/v1/events/1
```

출석 체크:

```bash
curl -s -X POST -H 'X-Member-Id: 1001' \
  http://localhost:8080/event/v1/events/1/rounds/17/entries
```

## Test

```bash
./gradlew test
```

## 주요 경로

- 애플리케이션 시작점: `src/main/java/com/event/EventApplication.java`
- 출석 응모 API: `src/main/java/com/event/presentation/controller/EventEntryController.java`
- 이벤트 상세 API: `src/main/java/com/event/presentation/controller/EventController.java`
- 출석 서비스: `src/main/java/com/event/application/service/AttendEventService.java`
- 이벤트 조회 서비스: `src/main/java/com/event/application/service/GetEventDetailService.java`
- 문서 인덱스: `docs/spec/README.md`

## 현재 설계 원칙

- FK는 두지 않고 Service 검증으로 정합성을 보완한다.
- 최소 unique만 유지한다.
- `event_applicant`는 회차별 applicant 기준, `event_entry`는 응모권/참여 이력, `event_win`은 실제 지급 이력이다.
- 출석 이벤트에서는 회차마다 `event_applicant`와 `event_entry`가 각각 생성되고, `event_entry.is_winner = true`로 저장한다.
- 출석 중복 기준은 `event_id + round_id + member_id`다.
- point API timeout 기준은 connect 1초, read 2초다.
- point API는 `idempotency_key = event_id + round_id + member_id`를 사용한다.
