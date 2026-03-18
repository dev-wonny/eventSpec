# Package Structure

이 문서는 이벤트 서비스 구현 시 사용할 패키지 구조 기준을 정리한다. 기본 방향은 개발리더가 공유한 `DS Event Backend` 예시를 따르되, 현재 이벤트 서비스의 범위인 출석체크와 향후 랜덤 리워드 확장을 반영해 보정한다.

## 목표

- 팀이 이미 익숙한 Layered + Hexagonal Hybrid 구조를 유지한다.
- Spring Boot + JPA + QueryDSL + Actuator + Swagger + ELK에 맞는 실무형 DDD 구조를 사용한다.
- 현재는 출석체크 중심으로 시작하되, `event`, `prize`, `entry`, `win` 구조가 자연스럽게 확장되도록 한다.

## 채택 방향

- 기본 구조는 `presentation -> application -> domain <- infrastructure`
- Spring Boot 기반 레이어드 아키텍처에 Hexagonal Architecture(Ports & Adapters) 패턴을 일부 적용한 하이브리드 구조를 사용한다.
- 구현 스타일은 "엄격한 순수 헥사고날"보다는 "실무형 Spring 구조"
- `domain.entity`는 JPA 엔티티를 허용한다.
- 이벤트 타입 확장은 JPA 상속보다 `EventEntity + Policy` 전략을 우선한다.
- `application.service`는 유스케이스 흐름 조립 계층으로 사용한다.
- `domain.service`는 도메인 정책 및 비즈니스 규칙 판단 계층으로 사용한다.
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
│   ├── model
│   ├── policy
│   ├── exception
│   │   ├── ResponseCode
│   │   ├── BusinessException
│   │   └── code
│   │       ├── CommonCode
│   │       ├── EventCode
│   │       ├── EntryCode
│   │       ├── AttendanceCode
│   │       ├── PrizeCode
│   │       └── RewardCode
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
    ├── logging
    ├── persistence
    │   └── database
    │       ├── builder
    │       ├── impl
    │       └── repository
    └── monitoring
```

## 전체 아키텍처 개요

이 프로젝트는 Spring Boot 기반 레이어드 아키텍처에 Hexagonal Architecture(Ports & Adapters) 패턴을 일부 적용한 하이브리드 구조다. 전체 흐름은 아래를 기준으로 한다.

```text
Presentation (Controller)
    -> Application Input Port (UseCase)
        -> Application Service
            -> Domain Service
            -> Application Output Port
                -> Infrastructure Persistence / External
```

역할 요약:

- Controller: HTTP 요청/응답 처리
- UseCase: 애플리케이션 기능 계약
- Application Service: 유스케이스 흐름 실행 및 조립
- Domain Service: 도메인 정책 및 비즈니스 규칙 판단
- Output Port: 저장/조회 의존성 추상화
- Infrastructure: 실제 DB, 외부 시스템, 설정 구현

## Service 책임 분리

핵심 원칙은 아래 한 줄로 정리한다.

- Application Service = 어떻게 실행할까
- Domain Service = 실행 가능한가

### 책임 비교

| 구분 | Application Service | Domain Service |
| --- | --- | --- |
| 역할 | 흐름 조립 | 비즈니스 규칙 |
| 데이터 조회 | O | X |
| Repository 사용 | O | 보통 X |
| Transaction | O | X |
| 외부 시스템 호출 | O | X |
| 도메인 판단 | 위임 | 직접 수행 |

### 언제 Domain Service가 필요한가

아래 조건 중 하나라도 해당하면 `domain.service` 사용을 권장한다.

- 여러 Entity가 관련된 규칙
- 복잡한 비즈니스 정책
- 재사용 가능한 정책

예:

- 이벤트 참여 가능 여부
- 경품 확률 계산
- 출석 이벤트 규칙
- 향후 랜덤 리워드 재고 정책

### Domain Service 책임 범위

`Domain Service`는 아래와 같은 "판단" 책임을 가진다.

- 이벤트 오픈 여부 검증
- 이벤트 기간 검증
- 중복 응모 검증
- 중복 당첨 검증
- 즉시 당첨 판정
- 향후 랜덤 리워드 재고 가능 여부 판단

중요 원칙:

- 재고 조회 자체는 `Domain Service`가 하지 않는다.
- 현재 출석체크 범위에서는 재고 관리를 하지 않는다.
- 향후 랜덤 리워드에서 재고 판단이 필요해지면, 현재 당첨 수와 최대 당첨 수 같은 데이터는 `Application Service`가 조회한다.
- `Domain Service`는 전달받은 값과 도메인 상태를 기준으로 "실행 가능한가"만 판단한다.

예:

- 현재 출석체크 -> 보상 매핑이 있으면 지급
- 향후 랜덤 리워드 -> `Application Service`가 현재 당첨 수를 조회하고, `Domain Service`가 최대 당첨 수와 비교해 지급 가능 여부 판단

## 계층별 역할

### `presentation`

- HTTP 요청/응답 처리
- Header, PathVariable, QueryString 검증
- `BaseResponse` 조립
- 전역 예외를 HTTP 응답으로 변환
- Swagger annotation 기반 API 문서 노출

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
- 도메인 판단에 필요한 데이터 조회
- 도메인 규칙 판단은 `domain.service`에 위임
- 응모 규칙 자체가 아니라 응모 유스케이스 실행 책임을 가진다
- 응답용 DTO / read model 조립 책임을 가진다
- 조회 전략은 `JpaRepository 기본 + QueryDSL 선택` 원칙을 따른다

예시 클래스:

- `AttendEventUseCase`
- `GetEventDetailUseCase`
- `AttendEventService`
- `GetEventDetailService`

### 조회 원칙

현재 프로젝트는 JPA 연관 관계를 통한 join 조회나 엔티티 그래프 조립을 기본 전략으로 사용하지 않는다.

원칙:

- 저장과 상태 변경은 JPA Entity를 기준으로 처리한다.
- 고정 조건 단순 조회는 `JpaRepository` 파생 메서드를 우선 사용한다.
- 동적 조건 검색, 조인, projection, 집계가 필요한 조회만 QueryDSL을 사용한다.
- QueryDSL 조회는 `impl/*Impl`에서 직접 처리한다.
- 검색 조건 조합은 `builder` 패키지의 `...EntityBuilder`에 위임한다.
- 세부 기준은 `docs/spec/shared/query-strategy-guide.md`를 따른다.
- `event -> rounds -> prizes` 같은 연관을 JPA 컬렉션 순회나 join fetch로 풀지 않는다.
- `Application Service`가 `event`, `round`, `applicant`, `entry`, `prize` 등을 각각 조회한 뒤 유스케이스에 맞게 조립한다.
- 상태 변경 유스케이스에서는 조회한 `entity`를 그대로 사용해 판단하고 저장한다.
- 조회 유스케이스에서는 필요한 값을 따로 조회한 뒤 DTO로 조립한다.

정리:

- 쓰기/상태 변경: `entity` 중심
- 읽기/응답 조립: `JpaRepository` 또는 QueryDSL + DTO 중심
- JPA 연관 join: 사용하지 않음

### `domain`

- persistence model과 도메인 해석 모델 분리
- 이벤트 핵심 엔티티
- 도메인 정책 및 비즈니스 규칙 판단
- 도메인 예외 및 에러 코드
- 값 객체와 enum
- 저장/외부 연동 없이 규칙 판단 수행
- 판단에 필요한 데이터는 인자로 전달받아 사용
- 여러 Entity에 걸친 비즈니스 규칙 처리를 담당할 수 있다

예시 클래스:

- `EventEntity`
- `AttendanceEvent`
- `RandomRewardEvent`
- `EventRoundEntity`
- `EventApplicantEntity`
- `EventEntryEntity`
- `EventWinEntity`
- `AttendanceEventPolicy`
- `RandomRewardPolicy`
- `EventEntryDomainService`
- `AttendanceDomainService`
- `ResponseCode`, `BusinessException`, `CommonCode`, `EventCode`, `EntryCode`

### `infrastructure`

- Spring Data JPA 저장 구현
- QueryDSL 조회 구현
- 외부 point API 연동
- Swagger/OpenAPI 설정
- ELK 로그 설정
- Actuator/필터/설정

예시 클래스:

- `EventJpaRepository`
- `EventEntryImpl`
- `AttendanceEventQueryBuilder`
- `PointClient`
- `OpenApiConfig`
- `RequestIdFilter`
- `AccessLogFilter`
- `ActuatorConfig`

요청 식별은 `traceId`가 아니라 `requestId` 하나만 사용한다. `RequestIdFilter`는 `X-Request-Id`를 읽거나 생성해 MDC `requestId`에 넣고, 응답 헤더에도 같은 값을 내려주는 역할을 맡는다.
트래픽 로그는 `AccessLogFilter`에서 `method`, `uri`, `status`, `latency` 기준으로 남기는 것을 기본으로 한다.

## 출석체크 기준 패키지 매핑

현재 범위가 출석체크이므로 최소 구현은 아래 단위로 시작하는 것을 권장한다.

### `presentation.controller`

- `EventEntryController`
  - `POST /event/v1/events/{eventId}/rounds/{roundId}/entries`
- `EventController`
  - `GET /event/v1/events/{eventId}`

각 controller는 `@Tag`, `@Operation`, `@ApiResponse`를 사용해 Swagger 문서를 노출하는 것을 권장한다.

### `presentation.dto.response`

- `BaseResponse`
- `EventEntryResponse`
- `EventDetailResponse`
- `AttendanceRoundResponse`
- `AttendanceSummaryResponse`

현재 출석 API는 Request Body가 없으므로 request DTO는 당장 필수가 아니다. 헤더는 controller `@RequestHeader`로 바인딩하고, validation 메시지는 DTO annotation message를 직접 사용한다.
응답/요청 DTO에는 필요한 범위에서 `@Schema`를 사용해 Swagger 예시와 설명을 함께 적는 것을 권장한다.

### `application.port.input`

- `AttendEventUseCase`
- `GetEventDetailUseCase`

### `application.service`

- `AttendEventService`
- `GetEventDetailService`

이 계층은 repository, output port, transaction을 사용해 유스케이스 흐름을 조립한다. 현재는 기존 응모 여부, 이벤트 기간, 보상 매핑 여부 등을 조회해 전달하고, 향후 랜덤 리워드가 들어오면 재고 수량 같은 값도 이 계층에서 조회해 전달한다.
현재 출석체크 구현에서는 `AttendEventService`가 `AttendanceProcessor`를 호출해 `entry -> draw -> reward`를 한 번에 실행하는 구조를 권장한다.
조회 시에도 `GetEventDetailService`가 QueryDSL로 `event`, `round`, `win`, `prize` 데이터를 각각 불러온 뒤 응답 DTO를 조립하는 구조를 권장한다.

### `application.dto.attendance`

- `AttendanceEntryResult`
- `AttendanceWinResult`
- `AttendanceRoundStatusDto`
- `AttendanceSummaryDto`

### `application.dto.event`

- `EventDetailDto`
- `EventRoundDto`

조회 DTO는 엔티티 그래프를 직접 순회하지 않고, `Application Service`가 미리 조회한 값을 받아 조립하는 형태를 권장한다.
예를 들어 `EventDetailDto`는 `event.getEventRounds()`를 직접 호출하기보다 `EventEntity + List<EventRoundDto>`를 받아 생성하는 구조가 더 적절하다.
필요하면 `EventDetailAssembler` 같은 조립 전용 클래스를 `application` 계층에 둘 수 있다.
특히 현재 기준에서는 QueryDSL로 `event`, `rounds`, `wins`, `prizes`를 각각 조회한 뒤 DTO를 조립한다.

### `application.port.output`

- `LoadEventPort`
- `LoadEventRoundPort`
- `LoadEventApplicantPort`
- `LoadAttendancePrizePort`
- `SaveEventEntryPort`
- `SaveEventWinPort`
- `LoadAttendanceEntryPort`
- `LoadEventDetailPort`
- `LoadEventRoundsPort`
- `LoadRoundWinsPort`
- `LoadRoundPrizesPort`
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

이 계층은 JPA Entity 같은 persistence model을 둔다.
조회 DTO나 application DTO가 이 계층의 연관 컬렉션을 직접 따라가며 데이터를 조립하는 방식은 권장하지 않는다.
JPA 연관 관계는 매핑 용도로만 두고, 실제 유스케이스 조회는 QueryDSL 기반 개별 repository/port 호출로 푸는 것을 기본으로 한다.

### `domain.model`

- `Event`
- `EventStatus`
- `AttendanceStatus`
- `RewardDecision`

이 계층은 persistence model과 분리된 도메인 해석 모델을 둔다.
이벤트 타입별 차이를 `AttendanceEvent extends Event` 같은 상속 구조로 풀기보다, 공통 모델 + `Policy` 조합으로 해석하는 방식을 기본으로 한다.

### `domain.policy`

- `EventPolicy`
- `AttendanceEventPolicy`
- `RandomRewardPolicy`
- `GameEventPolicy`
- `LotteryEventPolicy`
- `EventPolicyFactory`

이 계층은 `EventType`별 정책 구현과 정책 선택 책임을 가진다.
현재 실제 사용 `EventType`은 `ATTENDANCE`, `RANDOM_REWARD`이며 `GAME`, `LOTTERY`는 확장 후보로 본다.
즉 이벤트 타입 확장의 주 수단은 JPA 상속이 아니라 전략 패턴 기반 `Policy` 분리다.

### `domain.service`

- `EventEntryDomainService`
- `AttendanceDomainService`
- `PrizeProbabilityDomainService`
- `AttendanceStatusDomainService`

이 계층에는 "규칙 계산"을 둔다. HTTP, JPA 조회 조합, 외부 API 호출, transaction 경계는 두지 않는다.
Repository를 직접 호출하지 않고, `Application Service`가 준비한 데이터를 받아 판단만 수행한다.

단일 계산 규칙만 잘라낼 때는 내부 보조 클래스를 `...Policy`로 둘 수 있지만, 기본 단위는 `...DomainService`를 권장한다.

### `infrastructure.persistence.database.repository`

- `EventJpaRepository`
- `EventRoundJpaRepository`
- `EventApplicantJpaRepository`
- `EventEntryJpaRepository`
- `EventWinJpaRepository`
- `EventRoundPrizeJpaRepository`
- `PrizeJpaRepository`

### `infrastructure.persistence.database.impl`

- `EventQueryImpl`
- `EventRoundQueryImpl`
- `EventApplicantQueryImpl`
- `EventEntryImpl`
- `EventWinImpl`
- `EventRoundPrizeImpl`

Repository Port의 Impl 구현은 이 계층에 둔다.
이 디렉터리에는 `application.port.output`을 구현하는 `*Impl.java`만 둔다.
동적 검색 또는 다른 테이블 조인이 필요하면 해당 `*Impl` 안에서 `JpaRepository`, `...EntityBuilder`, `JPAQueryFactory`를 함께 사용한다.

### `infrastructure.persistence.database.builder`

- `EventEntityBuilder`
- `AttendanceEventDetailQueryBuilder`

QueryDSL 조건 조합과 read model 조회 최적화 코드는 이쪽에 둔다.
검색 조건 DTO의 `eventType`은 `String`보다 enum을 사용하는 쪽을 권장한다.
read model 조회 시 필요한 round, prize, probability 데이터는 QueryDSL 조회 결과나 별도 조회를 통해 `Application Service`에 전달하고, DTO 조립은 application 계층에서 수행한다.
현재 기준에서는 `fetchJoin`이나 join이 많은 단일 쿼리보다 QueryDSL 개별 조회를 우선하고, `Application Service`가 이를 조립하는 구조를 권장한다.

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
- Domain Service: `...DomainService`
- Persistence 구현체: `...Impl`
- Spring Data 저장소: `...JpaRepository`
- QueryDSL 검색 조건 빌더: `...EntityBuilder`
- QueryDSL 조회 조립기: `...QueryBuilder`

## 이벤트 응모 예시

### Application Service

역할:

1. 이벤트 조회
2. 도메인 정책 검증 요청
3. 응모 저장
4. 결과 반환

예시:

```java
@Transactional
public void entry(Long eventId, Long memberId) {

    Event event = eventRepository.find(eventId);

    eventEntryDomainService.validateEntry(event, memberId);

    eventEntryRepository.save(...);
}
```

특징:

- Repository 사용
- Transaction 있음
- 응모 유스케이스 실행과 흐름 조립 담당
- 응모 규칙 판단은 `Domain Service`에 위임
- QueryDSL로 필요한 엔티티와 값을 각각 조회해 사용

### Domain Service

역할:

- 이벤트 참여 가능 여부 판단

예시:

```java
public void validateEntry(Event event, Long memberId) {

    if (!event.isActive()) {
        throw new BusinessException(EventCode.EVENT_NOT_ACTIVE);
    }

    if (event.isExpired()) {
        throw new BusinessException(EventCode.EVENT_EXPIRED);
    }

    if (event.isAlreadyApplied(memberId)) {
        throw new BusinessException(EntryCode.ENTRY_ALREADY_APPLIED);
    }
}
```

특징:

- 순수 규칙 담당
- Repository 없음
- Transaction 없음
- 여러 Entity에 걸친 비즈니스 규칙 판단 가능

재고 가능 여부 판단도 향후 랜덤 리워드에서 같은 원칙을 따른다. 예를 들어 현재 당첨 수 조회는 `Application Service`가 수행하고, `Domain Service`는 "현재 당첨 수 < 최대 당첨 수"인지 판단만 수행한다.

## 기본 호출 구조

기능 호출 흐름은 아래 패턴을 따른다.

```text
Controller
 -> UseCase
   -> Application Service
     -> Domain Service
     -> Output Port
       -> Infrastructure 구현체
```

이벤트 응모 기능의 예시는 아래와 같다.

```text
EventEntryController
 -> EventEntryUseCase
   -> EventEntryService
     -> EventEntryDomainService
     -> Repository Ports
 -> EventEntryResponse
```

핵심은 Controller가 Service 구현체를 직접 의존하지 않고 `UseCase` 인터페이스를 의존한다는 점이다.

## 현재 구현 정책

- 현재 개발 범위는 출석체크 하나다.
- 따라서 `entry / draw / reward`를 공통 인터페이스로 먼저 잘게 쪼개지 않는다.
- 지금은 `AttendanceProcessor` 같은 출석체크 전용 처리 컴포넌트로 묶는 쪽을 권장한다.
- 내부 단계는 `entry -> draw -> reward`로 구분하되, 공통 추상화는 랜덤 리워드가 실제 구현 범위에 들어올 때 검토한다.

예시:

```text
AttendEventService
 -> AttendanceDomainService.validate(...)
 -> AttendanceProcessor.process(...)
    -> entry
    -> draw
    -> reward
```

## 조회 DTO 조립 원칙

조회 DTO는 `Application Service`가 조립한다.

권장 방식:

```java
List<EventRoundDto> rounds = eventRounds.stream()
        .map(round -> EventRoundDto.of(round, prizeMap, probabilityMap))
        .toList();

EventDetailDto result = EventDetailDto.of(event, rounds);
```

권장하지 않는 방식:

```java
EventDetailDto.of(event); // 내부에서 event.getEventRounds() 직접 순회
```

원칙:

- application DTO가 JPA 연관 컬렉션에 직접 의존하지 않는다.
- `event.getEventRounds()` 같은 lazy 연관 순회는 DTO 내부에서 하지 않는다.
- 필요한 round / prize / probability 데이터는 `Application Service`가 조회해 넘긴다.
- `eventType`은 DTO 조건과 조회 모델에서 가능한 enum으로 유지한다.

## 현재 프로젝트에 맞는 판단

- 지금은 기능 수가 많지 않으므로 리더 예시와 같은 레이어 중심 구조를 우선 사용한다.
- `event`, `prize`, `entry`가 더 커지면 기능별 상위 패키지 아래에 레이어를 두는 하이브리드 구조를 검토한다.
- 그러나 초기 버전에서는 팀이 익숙한 전역 레이어 구조가 온보딩과 유지보수에 유리하다.
- 에러 코드와 비즈니스 예외는 `domain.exception`에서 관리한다.
- 공통 응답 포맷과 전역 예외 변환은 `presentation.dto.response`, `presentation.exception`에서 관리한다.
- 공통 로그 관련 코드는 `common.logging`과 `infrastructure.logging`, 요청 식별은 `infrastructure.filter.RequestIdFilter`로 분리한다.

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
7. 로그 필터는 `infrastructure.logging`, 요청 식별 필터는 `infrastructure.filter.RequestIdFilter`로 분리한다.
8. Actuator 운영 설정은 `infrastructure.monitoring`으로 분리한다.
