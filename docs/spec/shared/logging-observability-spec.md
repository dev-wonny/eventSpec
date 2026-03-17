# Logging And Observability Spec

이 문서는 Event Platform Backend의 로그 수집, 요청 추적, ELK 연동 기준을 정의한다. 이번 개발 범위에는 애플리케이션 로그를 ELK로 적재하고 조회 가능한 상태로 만드는 작업이 포함된다.

## 1. Purpose

- 애플리케이션 로그 표준을 정한다.
- `requestId` 기반 요청 추적 기준을 정한다.
- ELK 적재 대상과 필수 필드를 정의한다.
- 운영 장애 분석과 비즈니스 이슈 추적 기준을 통일한다.

## 2. 범위

- 이번 범위에 ELK 연동이 포함된다.
- 모든 API 요청/응답 처리 과정은 구조화된 로그를 남긴다.
- 출석체크와 point 지급 연동 로그를 우선 관리 대상으로 본다.
- 상세 메트릭과 알람 체계는 추후 확장 가능하되, 이번에는 로그 수집과 검색 가능 상태 확보가 우선이다.

## 3. 운영 원칙

- 로그는 사람이 읽는 문자열 위주가 아니라 구조화된 JSON 로그를 기본으로 한다.
- 모든 요청은 `requestId`를 가진다.
- 요청 단위 상관관계 식별은 `requestId`로 한다.
- 이번 범위에서는 `traceId` 개념을 별도로 도입하지 않는다.
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
| `requestId` | 요청 추적 식별자 |
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
| `commonCode` | 공통 응답 코드 |
| `domainCode` | 도메인/운영 추적 코드 |
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

### point API 타임아웃 로그 기준

- point API timeout 기준은 `connection timeout = 1초`, `read timeout = 2초`, `총 대기 시간 = 최대 3초`다.
- 타임아웃 발생 시 `ERROR` 레벨 로그를 남긴다.
- 필수 로그 필드는 `requestId`, `commonCode=INTERNAL_ERROR`, `domainCode=POINT_API_TIMEOUT`, `eventId`, `roundId`, `memberId`다.
- 재시도 시에도 같은 `idempotency_key = event_id + round_id + member_id`를 사용한다는 점을 로그 문맥에서 추적 가능해야 한다.

## 7. 요청 처리 체인

현재 이벤트 플랫폼의 로깅 구성 흐름은 아래를 기준으로 한다.

```text
RequestIdFilter
        │
        ▼
AccessLogFilter
        │
        ▼
Controller
        │
        ▼
Service
        │
        ▼
GlobalExceptionHandler
```

구성 요약:

| Component | 목적 |
| --- | --- |
| `RequestIdFilter` | 요청 추적 |
| `AccessLogFilter` | 트래픽 로그 |
| `MDC` | 로그 상관관계 |
| `logstash encoder` | JSON 로그 |
| `ELK` | 로그 분석 |

## 8. ELK 연동 기준

- 애플리케이션 로그는 구조화된 JSON 형태로 출력한다.
- Spring Boot에서는 `logstash-logback-encoder`를 사용해 JSON 로그를 출력한다.
- 수집 에이전트 구성 방식은 인프라 표준을 따르되, 애플리케이션은 ELK 적재를 전제로 로그 포맷을 맞춘다.
- Kibana에서 `requestId`, `memberId`, `eventId`, `roundId` 기준 조회가 가능해야 한다.
- 운영자는 중복 출석, point 지급 실패, 외부 API 타임아웃을 Kibana에서 검색할 수 있어야 한다.
- ECS 환경에서는 파일 로그보다 stdout/stderr 스트림 수집을 우선 기준으로 본다.

### Kibana 조회 예시

API 트래픽:

```text
uri : "/events/*"
```

오류 분석:

```text
status >= 500
```

특정 요청 추적:

```text
requestId : "8F1C5D9A"
```

## 9. 코드 구조 권장

로그/요청 식별 관련 코드는 아래 패키지에 둔다.

```text
com.event
├── common
│   ├── logging
│   │   ├── LogFieldNames
│   │   └── LogContextKeys
└── infrastructure
    ├── filter
    │   └── RequestIdFilter
    └── logging
        ├── AccessLogFilter
        └── LogbackConfigSupport
```

## 10. RequestId 정책

- 요청 식별자는 `requestId` 하나만 사용한다.
- 클라이언트가 `X-Request-Id`를 보내면 그대로 사용한다.
- `X-Request-Id`가 없으면 서버가 새 값을 생성한다.
- 생성 또는 수신한 `requestId`는 MDC에 `requestId` 키로 저장한다.
- 응답 헤더에도 동일한 `X-Request-Id`를 내려준다.

권장 구현은 아래와 같다.

```java
package com.event.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
```

## 11. AccessLog 기준

Access log는 요청 메소드, URI, 응답 상태, 처리 시간을 남기는 것을 기본으로 한다.

예시:

```java
log.info(
    "method={} uri={} status={} latency={}ms",
    request.getMethod(),
    request.getRequestURI(),
    response.getStatus(),
    latency
);
```

권장 사항:

- `requestId`는 MDC를 통해 자동 포함되도록 설정한다.
- access log는 모든 요청에 대해 동일한 필드 구조를 유지한다.
- 상태 코드와 latency는 Kibana 집계에 바로 쓸 수 있는 형태로 남긴다.

## 12. 금지 항목

- 주민번호, 전화번호, 전체 토큰, 쿠폰 원문, 외부 API 전체 payload 로그 적재 금지
- point API 성공 응답 전문 전체 저장 금지
- 응답 코드 세분화를 로그 대체 수단으로 사용하는 것 금지

## 13. 결론

- 이번 범위에는 ELK 연동이 포함된다.
- 로그는 `requestId`와 비즈니스 식별자를 포함한 구조화 로그로 남긴다.
- 운영 분석은 `ResponseCode`가 아니라 ELK 로그를 중심으로 수행한다.
