# API Error Handling And Response Specification

이 문서는 이벤트 플랫폼의 API 응답 구조와 예외 처리 방식을 정의한다. 목표는 API 응답 구조를 표준화하고, 에러 코드를 일관되게 관리하며, 요청 단위 로그 추적과 Validation / Business / System 에러 분리를 명확히 하는 것이다.

## 1. 목적

- API 응답 구조를 표준화한다.
- 에러 코드를 일관되게 관리한다.
- 요청 단위 `requestId` 기반 로그 추적을 가능하게 한다.
- Validation / Business / System 에러를 명확히 분리한다.

## 2. 권장 패키지 트리

```text
com.event
├── domain
│   └── exception
│       ├── ResponseCode
│       ├── BusinessException
│       └── ValidationCode
│
├── application
│
├── presentation
│   ├── controller
│   ├── dto
│   │   ├── request
│   │   └── response
│   │       └── BaseResponse
│   │
│   └── exception
│       └── GlobalExceptionHandler
│
└── infrastructure
```

설명:

- `ResponseCode`, `BusinessException`, `ValidationCode`는 `domain.exception`에 둔다.
- `BaseResponse`는 `presentation.dto.response`에 둔다.
- `GlobalExceptionHandler`는 `presentation.exception`에 둔다.
- `requestId` 생성 및 MDC 주입 필터는 `infrastructure` 하위에 둔다.

## 3. 전체 구조

예외 처리 흐름은 아래와 같다.

```text
Controller
    ↓
Application / Domain
    ↓
throw BusinessException
    ↓
GlobalExceptionHandler
    ↓
BaseResponse(JSON)
```

요청 흐름에서 발생하는 모든 예외는 `GlobalExceptionHandler`에서 처리되어 표준 API 응답 형태로 변환된다.

## 4. 표준 API 응답 구조

모든 API 응답은 아래 구조를 따른다.

```json
{
  "code": "SUCCESS",
  "message": "정상 처리되었습니다.",
  "timestamp": "2026-01-01T12:00:00Z",
  "data": {}
}
```

| 필드 | 설명 |
| --- | --- |
| `code` | 응답 코드 |
| `message` | 사용자 메시지 |
| `timestamp` | 응답 시각 |
| `data` | 응답 데이터 |

핵심 원칙:

- 모든 API 응답은 `BaseResponse`를 사용한다.
- 성공/실패 여부와 무관하게 동일한 envelope 구조를 유지한다.
- `requestId`는 응답 body가 아니라 로그와 MDC를 통한 요청 추적에 사용한다.

## 5. BaseResponse

`BaseResponse`는 아래 코드 형태로 고정한다.

```java
public record BaseResponse<T>(
        String code,
        String message,
        Instant timestamp,
        T data
) {

    public static <T> BaseResponse<T> of(ResponseCode code, T data) {

        return new BaseResponse<>(
                code.getCode(),
                code.getMessage(),
                Instant.now(),
                data
        );
    }
}
```

특징:

- Generic response를 지원한다.
- 응답 시각은 `Instant.now()`로 생성한다.
- 응답 생성은 `BaseResponse.of(ResponseCode, data)`를 기준으로 통일한다.
- `requestId`는 응답 필드에 포함하지 않는다.

## 6. Error Code 설계

에러 코드는 `enum + interface` 구조로 관리한다.

### 6.1 ResponseCode 인터페이스

```java
public interface ResponseCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
```

모든 에러 코드는 해당 인터페이스를 구현한다.

### 6.2 ValidationCode

DTO validation 오류 코드는 `enum`으로 관리한다.

```java
public enum ValidationCode implements ResponseCode {

    EVENT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT_NAME_REQUIRED", "이벤트명은 필수입니다."),
    EVENT_NAME_SIZE(HttpStatus.BAD_REQUEST, "EVENT_NAME_SIZE", "이벤트명은 100자 이내여야 합니다."),
    EVENT_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT_TYPE_REQUIRED", "이벤트 유형은 필수입니다.");
}
```

DTO annotation에서는 enum 이름을 메시지 키로 사용한다.

```java
@NotBlank(message = "EVENT_NAME_REQUIRED")
private String eventName;
```

원칙:

- Validation 메시지는 문자열 하드코딩 대신 `enum`으로 중앙 관리한다.
- 도메인 / 기능별 비즈니스 코드도 동일하게 `ResponseCode` 구현 enum으로 관리한다.
- 클라이언트는 code와 message를 응답에서 받고, 서버는 동일 code를 로그 추적에 활용한다.

## 7. BusinessException

도메인 / 애플리케이션 로직에서 발생하는 예외는 `BusinessException`으로 표현한다.

```java
throw new BusinessException(ValidationCode.EVENT_NAME_REQUIRED);
```

특징:

- `ResponseCode` 기반이다.
- HTTP status 정보를 포함한다.
- `GlobalExceptionHandler`에서 일관되게 처리된다.

## 8. GlobalExceptionHandler

전역 예외 처리는 `GlobalExceptionHandler`가 담당한다.

위치:

```text
com.event.presentation.exception.GlobalExceptionHandler
```

역할:

- `BusinessException` 처리
- Validation exception 처리
- 시스템 예외 처리
- API 응답 포맷 통일

### 8.1 BusinessException 처리

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex)
```

처리 흐름:

```text
BusinessException
    ↓
ResponseCode 조회
    ↓
BaseResponse.of(code, data)
    ↓
HTTP Response 반환
```

### 8.2 ValidationException 처리

Spring validation 오류는 `MethodArgumentNotValidException`으로 처리한다.

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
```

Validation 메시지는 `ValidationCode` enum을 통해 조회한다.

예시 응답:

```json
{
  "code": "BAD_REQUEST",
  "message": "잘못된 요청입니다.",
  "timestamp": "2026-01-01T12:00:00Z",
  "data": {
    "eventName": "이벤트명은 필수입니다."
  }
}
```

### 8.3 시스템 예외 처리

처리되지 않은 예외는 모두 시스템 예외로 간주한다.

```java
@ExceptionHandler(Exception.class)
```

응답 원칙:

- HTTP status는 `500`
- 응답 코드는 `INTERNAL_ERROR`
- 내부 상세 정보는 응답에 직접 노출하지 않는다.
- 상세 원인은 `requestId` 기반 로그와 ELK에서 추적한다.

## 9. RequestId

모든 요청에는 `requestId`가 생성된다.

목적:

- 요청 단위 로그 추적
- ELK 로그 분석
- 장애 분석
- API 요청과 서버 로그의 상관관계 유지

동작 방식:

```text
Request
    ↓
RequestIdFilter
    ↓
MDC.put("requestId")
    ↓
Logging / ExceptionHandler 사용
```

로그 예시:

```text
[F3A92B8E12C34A5B9D8F] BusinessException: EVENT_NAME_REQUIRED
```

원칙:

- `requestId`는 모든 요청 로그에 포함한다.
- `requestId`는 MDC에 저장한다.
- ELK에서는 `requestId` 기준으로 검색 가능해야 한다.
- `requestId`는 응답 body가 아니라 로그 추적용 식별자다.

## 10. 예외 처리 흐름 요약

```text
DTO Validation
    ↓
MethodArgumentNotValidException
    ↓
GlobalExceptionHandler
    ↓
ValidationCode lookup
    ↓
BaseResponse 반환

Service Logic
    ↓
throw BusinessException
    ↓
GlobalExceptionHandler
    ↓
ResponseCode 기반 응답

Unexpected Exception
    ↓
GlobalExceptionHandler
    ↓
INTERNAL_ERROR 응답
```

## 11. 설계 원칙

1. 에러 코드는 중앙에서 `enum`으로 관리한다.
2. 모든 API 응답은 고정된 `BaseResponse`를 사용한다.
3. 모든 로그는 `requestId` 기반으로 추적 가능해야 한다.
4. DTO validation 메시지는 `enum` 기반으로 중앙 관리한다.
