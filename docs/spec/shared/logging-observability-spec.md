# Logging And Observability Spec

이 문서는 Event Platform Backend의 로그 수집, 추적, ELK 연동 기준을 정의한다. 이번 개발 범위에는 애플리케이션 로그를 ELK로 적재하고 조회 가능한 상태로 만드는 작업이 포함된다.

## 1. Purpose

- 애플리케이션 로그 표준을 정한다.
- `traceId` 기반 요청 추적 기준을 정한다.
- ELK 적재 대상과 필수 필드를 정의한다.
- 운영 장애 분석과 비즈니스 이슈 추적 기준을 통일한다.

## 2. 범위

- 이번 범위에 ELK 연동이 포함된다.
- 모든 API 요청/응답 처리 과정은 구조화된 로그를 남긴다.
- 출석체크와 point 지급 연동 로그를 우선 관리 대상으로 본다.
- 상세 메트릭과 알람 체계는 추후 확장 가능하되, 이번에는 로그 수집과 검색 가능 상태 확보가 우선이다.

## 3. 운영 원칙

- 로그는 사람이 읽는 문자열 위주가 아니라 구조화된 JSON 로그를 기본으로 한다.
- 모든 요청은 `traceId`를 가진다.
- 요청 단위 상관관계 식별은 `traceId`로 한다.
- 비즈니스 식별자는 가능한 경우 함께 기록한다.
- 민감정보와 전체 payload는 로그에 남기지 않는다.
- 운영 분석은 Kibana 조회를 기본 경로로 한다.
- ECS 배포 환경을 고려해 애플리케이션 로그는 stdout 기반 수집에 적합해야 한다.

## 4. 필수 로그 필드

공통 필수 필드는 아래를 기준으로 한다.

| 필드 | 설명 |
| --- | --- |
| `timestamp` | 로그 발생 시각 |
| `level` | 로그 레벨 |
| `service` | 서비스명 (`ds-event-backend`) |
| `environment` | `dev`, `stg`, `prod` 등 배포 환경 |
| `traceId` | 요청 추적 식별자 |
| `logger` | logger 이름 |
| `message` | 로그 메시지 |

요청/비즈니스 문맥 필드는 아래를 권장한다.

| 필드 | 설명 |
| --- | --- |
| `eventId` | 이벤트 식별자 |
| `roundId` | 회차 식별자 |
| `memberId` | 회원 식별자 |
| `supplierId` | 공급사 식별자 |
| `path` | 요청 경로 |
| `method` | HTTP 메소드 |
| `responseCode` | 공통 응답 코드 |
| `httpStatus` | HTTP 상태 코드 |
| `externalRequestId` | 외부 point API 요청 추적값 |
| `exceptionClass` | 예외 클래스명 |

## 5. 로그 레벨 기준

- `INFO`: 정상 요청 시작/종료, 주요 비즈니스 상태 전이
- `WARN`: 중복 출석, 외부 API 재시도 가능 오류, 유효성 경계 상황
- `ERROR`: 외부 point API 실패, 타임아웃, 예기치 못한 예외
- `DEBUG`: 로컬 개발 또는 문제 분석용 추가 정보

## 6. 출석체크 기준 로깅 포인트

출석체크 유스케이스에서는 아래 로그를 남긴다.

1. 요청 수신
2. `event`, `round`, `applicant` 조회 결과
3. `round.event_id == event.id` 정합성 검증 실패 여부
4. 중복 출석 판정 결과
5. 보상 매핑 존재 여부
6. point API 요청 시작
7. point API 성공/실패/타임아웃 결과
8. `event_entry`, `event_win` 저장 결과
9. 최종 응답 결과

## 7. ELK 연동 기준

- 애플리케이션 로그는 구조화된 JSON 형태로 출력한다.
- Spring Boot에서는 `logstash-logback-encoder`를 사용해 JSON 로그를 출력한다.
- 수집 에이전트 구성 방식은 인프라 표준을 따르되, 애플리케이션은 ELK 적재를 전제로 로그 포맷을 맞춘다.
- Kibana에서 `traceId`, `memberId`, `eventId`, `roundId` 기준 조회가 가능해야 한다.
- 운영자는 중복 출석, point 지급 실패, 외부 API 타임아웃을 Kibana에서 검색할 수 있어야 한다.
- ECS 환경에서는 파일 로그보다 stdout/stderr 스트림 수집을 우선 기준으로 본다.

## 8. 코드 구조 권장

로그/추적 관련 코드는 아래 패키지에 둔다.

```text
com.event
├── common
│   ├── logging
│   │   ├── LogFieldNames
│   │   └── LogContextKeys
│   └── tracing
│       └── TraceIdHolder
└── infrastructure
    ├── filter
    │   └── TraceIdFilter
    └── logging
        ├── RequestLoggingFilter
        └── LogbackConfigSupport
```

## 9. 금지 항목

- 주민번호, 전화번호, 전체 토큰, 쿠폰 원문, 외부 API 전체 payload 로그 적재 금지
- point API 성공 응답 전문 전체 저장 금지
- 응답 코드 세분화를 로그 대체 수단으로 사용하는 것 금지

## 10. 결론

- 이번 범위에는 ELK 연동이 포함된다.
- 로그는 `traceId`와 비즈니스 식별자를 포함한 구조화 로그로 남긴다.
- 운영 분석은 `ResponseCode`가 아니라 ELK 로그를 중심으로 수행한다.
