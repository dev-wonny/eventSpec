# Package Structure

이 문서는 이벤트 서비스 구현 시 사용할 패키지 구조 기준을 정리한다. 기본 방향은 개발리더가 공유한 `DS Event Backend` 예시를 따르되, 현재 이벤트 서비스의 범위인 출석체크와 향후 랜덤 리워드 확장을 반영해 보정한다.

## 목표

- 팀이 이미 익숙한 레이어드 + 포트/어댑터(헥사고날) 혼합 구조를 유지한다.
- Spring Boot + JPA + QueryDSL + Actuator + ELK에 맞는 실무형 DDD 구조를 사용한다.
- 현재는 출석체크 중심으로 시작하되, `event`, `prize`, `entry`, `win` 구조가 자연스럽게 확장되도록 한다.

## 채택 방향

- 기본 구조는 `presentation -> application -> domain <- infrastructure`
- 구현 스타일은 "엄격한 순수 헥사고날"보다는 "실무형 Spring 구조"
- `domain.entity`는 JPA 엔티티를 허용한다.
- `domain.service`는 도메인 정책/검증 컴포넌트로 사용한다.
- Controller는 `input port`에만 의존한다.
- Service는 `output port`를 통해서만 DB/외부 API에 접근한다.

## 권장 루트 패키지

현재 Gradle `group`을 기준으로 아래 형태를 사용한다.

```text
com.event
```

## 권장 패키지 트리

```text
com.event
├── EventApplication
├── common
│   ├── logging
│   ├── tracing
│   └── util
├── presentation
│   ├── controller
│   ├── dto
│   │   ├── request
│   │   └── response
│   │       └── BaseResponse
│   └── exception
│       └── GlobalExceptionHandler
├── application
│   ├── dto
│   │   ├── attendance
│   │   ├── event
│   │   ├── prize
│   │   └── condition
│   ├── port
│   │   ├── input
│   │   └── output
│   └── service
├── domain
│   ├── entity
│   ├── exception
│   │   ├── ResponseCode
│   │   ├── BusinessException
│   │   └── ValidationCode
│   ├── service
│   ├── value
│   └── util
└── infrastructure
    ├── config
    ├── external
    │   └── point
    │       ├── client
    │       ├── config
    │       └── dto
    ├── filter
    ├── interceptor
    ├── logging
    ├── persistence
    │   └── database
    │       ├── builder
    │       ├── impl
    │       └── repository
    └── monitoring
```

## 계층별 역할

### `presentation`

- HTTP 요청/응답 처리
- Header, PathVariable, QueryString 검증
- `BaseResponse` 조립
- 전역 예외를 HTTP 응답으로 변환

예시 클래스:

- `EventEntryController`
- `EventController`
- `BaseResponse`
- `GlobalExceptionHandler`

### `common.logging`

- 공통 로그 필드명
- MDC key 상수
- 로그 컨텍스트 유틸

응답 코드와 별개로 운영 분석에 필요한 공통 로그 키를 한 곳에서 관리한다.

### `application`

- 유스케이스 정의와 구현
- 요청 하나를 처리하기 위한 흐름 오케스트레이션
- 트랜잭션 경계
- 외부 시스템/DB 접근을 포트로 추상화

예시 클래스:

- `AttendEventUseCase`
- `GetEventDetailUseCase`
- `AttendEventService`
- `GetEventDetailService`

### `domain`

- 이벤트 핵심 엔티티
- 비즈니스 정책
- 도메인 예외 및 에러 코드
- 값 객체와 enum

예시 클래스:

- `EventEntity`
- `EventRoundEntity`
- `EventApplicantEntity`
- `EventEntryEntity`
- `EventWinEntity`
- `AttendanceEntryPolicy`
- `ResponseCode`, `BusinessException`, `ValidationCode`

### `infrastructure`

- Spring Data JPA
- QueryDSL 조회 구현
- 외부 point API 연동
- ELK 로그 설정
- Actuator/필터/설정

예시 클래스:

- `EventJpaRepository`
- `EventEntryRepositoryImpl`
- `AttendanceEventQueryBuilder`
- `PointClient`
- `TraceIdFilter`
- `RequestLoggingFilter`
- `ActuatorConfig`

## 출석체크 기준 패키지 매핑

현재 범위가 출석체크이므로 최소 구현은 아래 단위로 시작하는 것을 권장한다.

### `presentation.controller`

- `EventEntryController`
  - `POST /event/v1/events/{eventId}/rounds/{roundId}/entries`
- `EventController`
  - `GET /event/v1/events/{eventId}`

### `presentation.dto.response`

- `BaseResponse`
- `EventEntryResponse`
- `EventDetailResponse`
- `AttendanceRoundResponse`
- `AttendanceSummaryResponse`

현재 출석 API는 Request Body가 없으므로 request DTO는 당장 필수가 아니다.

### `application.port.input`

- `AttendEventUseCase`
- `GetEventDetailUseCase`

### `application.service`

- `AttendEventService`
- `GetEventDetailService`

### `application.dto.attendance`

- `AttendanceEntryResult`
- `AttendanceWinResult`
- `AttendanceRoundStatusDto`
- `AttendanceSummaryDto`

### `application.port.output`

- `LoadEventPort`
- `LoadEventRoundPort`
- `LoadEventApplicantPort`
- `LoadAttendancePrizePort`
- `SaveEventEntryPort`
- `SaveEventWinPort`
- `LoadAttendanceEntryPort`
- `LoadEventDetailPort`
- `PointRewardPort`

포트는 기술 구현이 아니라 유스케이스 관점 이름으로 두는 것을 권장한다.

### `domain.entity`

- `EventEntity`
- `EventRoundEntity`
- `EventApplicantEntity`
- `EventEntryEntity`
- `EventWinEntity`
- `EventRoundPrizeEntity`
- `PrizeEntity`

### `domain.service`

- `AttendanceEligibilityPolicy`
- `AttendanceEntryPolicy`
- `AttendancePrizePolicy`
- `AttendanceStatusPolicy`

이 계층에는 "규칙 계산"을 둔다. HTTP나 JPA 조회 조합은 두지 않는다.

### `infrastructure.persistence.database.repository`

- `EventJpaRepository`
- `EventRoundJpaRepository`
- `EventApplicantJpaRepository`
- `EventEntryJpaRepository`
- `EventWinJpaRepository`
- `EventRoundPrizeJpaRepository`
- `PrizeJpaRepository`

### `infrastructure.persistence.database.impl`

- `EventRepositoryImpl`
- `EventRoundRepositoryImpl`
- `EventApplicantRepositoryImpl`
- `EventEntryRepositoryImpl`
- `EventWinRepositoryImpl`
- `EventRoundPrizeRepositoryImpl`

### `infrastructure.persistence.database.builder`

- `EventQueryBuilder`
- `AttendanceEventDetailQueryBuilder`

QueryDSL 조건 조합과 read model 조회 최적화 코드는 이쪽에 둔다.

### `infrastructure.external.point`

- `PointClient`
- `PointClientConfig`
- `PointGrantRequest`
- `PointGrantResponse`

외부 point API 스키마는 여기서 격리한다. 현재는 Spring Web 기반 클라이언트를 권장한다.

### `infrastructure.monitoring`

- Actuator endpoint 설정
- health/readiness 노출 설정

ECS 배포 환경에서 운영 상태 확인에 필요한 최소 actuator 구성을 여기서 관리한다.

### `infrastructure.logging`

- Request/Response logging filter
- Logback 설정 지원
- ELK 적재용 구조화 로그 설정

이번 범위에 ELK 연동이 포함되므로 로깅 구성은 별도 인프라 패키지로 분리한다.

## 의존 방향

권장 의존 방향은 아래와 같다.

```text
presentation -> application -> domain
infrastructure -> application, domain
domain -> (가능하면 다른 계층을 모름)
```

실무형 타협으로 아래는 허용한다.

- `domain.entity`에 JPA 어노테이션 사용
- `domain.service`를 Spring Bean으로 등록

다만 아래는 피한다.

- Controller가 Entity를 직접 반환
- Service가 Spring Data Repository를 직접 의존
- Domain이 `presentation` DTO를 참조
- External DTO가 `domain`에 침투

## 네이밍 규칙

- Controller: `...Controller`
- Input Port: `...UseCase`
- Output Port: `...Port`
- Application Service: `...Service`
- Domain Policy: `...Policy`
- Persistence 구현체: `...RepositoryImpl`
- Spring Data 저장소: `...JpaRepository`
- QueryDSL 조립기: `...QueryBuilder`

## 현재 프로젝트에 맞는 판단

- 지금은 기능 수가 많지 않으므로 리더 예시와 같은 레이어 중심 구조를 우선 사용한다.
- `event`, `prize`, `entry`가 더 커지면 기능별 상위 패키지 아래에 레이어를 두는 하이브리드 구조를 검토한다.
- 그러나 초기 버전에서는 팀이 익숙한 전역 레이어 구조가 온보딩과 유지보수에 유리하다.
- 에러 코드와 비즈니스 예외는 `domain.exception`에서 관리한다.
- 공통 응답 포맷과 전역 예외 변환은 `presentation.dto.response`, `presentation.exception`에서 관리한다.
- 공통 로그/추적 코드는 `common.logging`, `common.tracing`, `infrastructure.logging`에 분리한다.

## 나중에 하이브리드 구조로 전환하는 기준

아래 조건이 보이면 기능 중심 상위 패키지 전환을 검토한다.

- 기능 수가 크게 늘어 `presentation/controller`가 비대해질 때
- 출석, 랜덤 리워드, 관리자 기능이 서로 다른 팀/릴리즈 주기를 가질 때
- 한 기능 변경 시 너무 많은 전역 패키지를 동시에 오가야 할 때

## 구현 시작 체크리스트

1. Controller는 `input port`에만 의존한다.
2. Application Service는 `output port`에만 의존한다.
3. 출석 전용 검증은 [service-validation.md](/Users/wonny/Documents/GitHub/event/docs/spec/features/attendance-check/service-validation.md)를 기준으로 둔다.
4. 응답 포맷은 `presentation.dto.response`에서 끝낸다.
5. QueryDSL 검색/조합 로직은 `infrastructure.persistence.database.builder`로 분리한다.
6. 외부 point API DTO는 `infrastructure.external.point.dto`로 격리한다.
7. 로그/추적 필터는 `infrastructure.logging`, `infrastructure.filter`, `common.tracing` 조합으로 분리한다.
8. Actuator 운영 설정은 `infrastructure.monitoring`으로 분리한다.
