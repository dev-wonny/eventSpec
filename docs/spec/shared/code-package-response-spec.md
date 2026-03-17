# API Error Handling And Response Specification

이 문서는 이벤트 플랫폼의 API 응답 구조와 Validation / Business / System 예외 처리 방식을 정의한다. 목표는 API 응답 형식을 통일하고, Validation 처리를 단순화하며, 프론트엔드는 `CommonCode`만 의존하고 서버는 domain code와 `requestId`로 운영 추적이 가능하도록 만드는 것이다.

## 1. 목적

- API 응답 형식을 통일한다.
- Validation 오류 처리를 단순화한다.
- 프론트엔드 분기 복잡도를 줄인다.
- Business 오류는 domain code로 관리한다.
- 로그와 ELK 분석은 `requestId + domainCode + commonCode` 기준으로 수행한다.

## 2. Code의 범주

코드는 보통 아래 세 종류가 섞인다.

| 종류 | 예시 |
| --- | --- |
| Validation | DTO annotation message, 헤더 누락 |
| Business | `ENTRY_ALREADY_APPLIED` |
| System | `INTERNAL_ERROR` |

원칙:

- Validation은 annotation message를 직접 사용한다.
- Business는 domain enum code를 사용한다.
- API 응답 body의 `code`는 `CommonCode` 기준으로 반환한다.
- 로그와 운영 분석에는 domain code를 유지한다.

## 3. 권장 패키지 구조

```text
domain
 └ exception
     ├ ResponseCode
     ├ BusinessException
     └ code
         ├ CommonCode
         ├ EventCode
         ├ EntryCode
         ├ AttendanceCode
         ├ PrizeCode
         └ RewardCode

presentation
 ├ dto
 │   └ response
 │       └ BaseResponse
 └ exception
     └ GlobalExceptionHandler

infrastructure
 └ filter
     └ RequestIdFilter
```

설명:

- `ResponseCode`, `BusinessException`은 `domain.exception`에 둔다.
- 공통/도메인 code enum은 `domain.exception.code`에 둔다.
- Validation 전용 enum은 두지 않는다.
- `BaseResponse`는 `presentation.dto.response`에 둔다.
- `GlobalExceptionHandler`는 `presentation.exception`에 둔다.
- `requestId` 생성 및 MDC 주입은 `infrastructure.filter.RequestIdFilter`에서 관리한다.
- 이번 범위에서는 `traceId`를 별도로 두지 않는다.

## 4. BaseResponse

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

    public static BaseResponse<Void> error(ResponseCode code) {
        return new BaseResponse<>(
                code.getCommonCode().getCode(),
                code.getMessage(),
                Instant.now(),
                null
        );
    }

    public static <T> BaseResponse<T> error(CommonCode code, String message, T data) {
        return new BaseResponse<>(
                code.getCode(),
                message,
                Instant.now(),
                data
        );
    }
}
```

설명:

- 이 구현 자체는 고정이다.
- 성공 응답은 `CommonCode.SUCCESS`를 사용해 `BaseResponse.of(...)`로 생성할 수 있다.
- 오류 응답도 `BaseResponse.error(...)` 팩토리 메서드로 생성하는 것을 권장한다.
- `BusinessException` 오류 응답은 `BaseResponse.error(ResponseCode code)`를 사용한다.
- Validation 오류 응답은 `BaseResponse.error(CommonCode code, String message, T data)`를 사용한다.

## 5. ResponseCode 인터페이스

```java
public interface ResponseCode {

    HttpStatus getStatus();

    String getCode();         // domain code

    String getMessage();      // 사용자 메시지

    CommonCode getCommonCode(); // API 응답 카테고리
}
```

핵심:

- 서비스는 `ResponseCode` 기준으로 예외를 던진다.
- 로그에는 `getCode()`의 domain code를 남긴다.
- API 응답 body에는 `getCommonCode().getCode()`를 사용한다.

## 6. CommonCode

프론트는 `CommonCode.code` 기준으로 분기한다.

```java
@Getter
@AllArgsConstructor
public enum CommonCode implements ResponseCode {

    SUCCESS(HttpStatus.OK, "SUCCESS", "성공"),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),

    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "대상을 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청 상태가 충돌합니다."),

    BUSINESS_ERROR(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", "요청을 처리할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override
    public CommonCode getCommonCode() {
        return this;
    }
}
```

원칙:

- 프론트는 `CommonCode`만 의존한다.
- 세부 domain code가 늘어나도 프론트 분기 수는 늘리지 않는다.
- 공통 성공 코드는 `SUCCESS`를 사용한다.

## 7. Validation 처리 원칙

DTO validation은 annotation message를 직접 사용한다.

예:

```java
@NotNull(message = "회차 식별자는 필수입니다.")
private Long roundId;
```

원칙:

- Validation 메시지는 enum으로 관리하지 않는다.
- DTO 가독성을 우선한다.
- 불필요한 enum 증가를 막는다.
- Validation 실패 응답은 항상 `CommonCode.INVALID_REQUEST`를 사용한다.

### DTO 예시

```java
@Schema(description = "이벤트 응모 요청")
@Getter
public class EventEntryRequest {

    @Schema(description = "회차 식별자", example = "1")
    @NotNull(message = "회차 식별자는 필수입니다.")
    private Long roundId;
}
```

참고:

- 현재 출석 POST API는 body가 없고 `roundId`가 path variable이므로 위 예시는 공통 validation 방식 설명용이다.
- `X-Member-Id`는 DTO annotation이 아니라 `@RequestHeader` 바인딩으로 처리한다.

## 8. Domain ErrorCode 설계

domain code enum은 아래 필드를 가진다.

- `code`: 영어 enum 식별자
- `message`: 한글 메시지
- `commonCode`: API 응답 카테고리
- `statusOverride`: 특정 상황에서 HTTP status override

종류:

- `EventCode`
- `EntryCode`
- `AttendanceCode`
- `PrizeCode`
- `RewardCode`

### EventCode 예시

```java
@Getter
@AllArgsConstructor
public enum EventCode implements ResponseCode {

    EVENT_NOT_FOUND(
        "EVENT_NOT_FOUND",
        "이벤트가 존재하지 않습니다.",
        CommonCode.NOT_FOUND
    ),

    EVENT_NOT_STARTED(
        "EVENT_NOT_STARTED",
        "이벤트 시작 전입니다.",
        CommonCode.BUSINESS_ERROR
    ),

    EVENT_EXPIRED(
        "EVENT_EXPIRED",
        "이벤트 기간이 종료되었습니다.",
        CommonCode.BUSINESS_ERROR
    ),

    EVENT_ALREADY_APPLIED(
        "EVENT_ALREADY_APPLIED",
        "이미 참여한 이벤트입니다.",
        CommonCode.CONFLICT
    ),

    EVENT_SUPPLIER_MISMATCH(
        "EVENT_SUPPLIER_MISMATCH",
        "이벤트 공급사가 일치하지 않습니다.",
        CommonCode.FORBIDDEN,
        HttpStatus.FORBIDDEN
    );

    private final String code;
    private final String message;
    private final CommonCode commonCode;
    private final HttpStatus statusOverride;

    EventCode(String code, String message, CommonCode commonCode) {
        this(code, message, commonCode, null);
    }

    @Override
    public HttpStatus getStatus() {
        return statusOverride != null ? statusOverride : commonCode.getStatus();
    }
}
```

핵심:

- domain code는 로그/운영 분석용 식별자다.
- `commonCode`는 API 응답용 카테고리다.
- `statusOverride`가 없으면 `commonCode.status`를 따른다.

## 9. BusinessException

```java
public class BusinessException extends RuntimeException {

    private final ResponseCode responseCode;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
```

서비스 계층은 `CommonCode`를 직접 다루지 않고 domain code enum만 사용한다.

## 10. GlobalExceptionHandler

### BusinessException 처리

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {

    ResponseCode rc = ex.getResponseCode();

    return ResponseEntity
        .status(rc.getStatus())
        .body(BaseResponse.error(rc));
}
```

처리 원칙:

- HTTP status는 `rc.getStatus()`를 사용한다.
- 응답 body의 `code`는 `rc.getCommonCode().getCode()`를 사용한다.
- 응답 body의 `message`는 domain code의 메시지를 사용한다.
- domain code 자체는 로그에 남긴다.

### Validation 예외 처리

Validation 실패는 아래 예외를 `INVALID_REQUEST`로 처리한다.

- `MethodArgumentNotValidException`
- `MissingRequestHeaderException`
- `ConstraintViolationException`
- `HttpMessageNotReadableException`

`MethodArgumentNotValidException` 예시:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException ex) {

    Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing
            ));

    return ResponseEntity
            .status(CommonCode.INVALID_REQUEST.getStatus())
            .body(BaseResponse.error(
                    CommonCode.INVALID_REQUEST,
                    CommonCode.INVALID_REQUEST.getMessage(),
                    errors
            ));
}
```

원칙:

- Validation 오류는 `data = Map<String, String>` 형태로 반환한다.
- field당 첫 번째 메시지만 반환한다.
- `X-Member-Id` 누락도 `INVALID_REQUEST`로 처리한다.

Validation 예시 응답:

```json
{
  "code": "INVALID_REQUEST",
  "message": "잘못된 요청입니다.",
  "timestamp": "2026-03-17T12:00:00Z",
  "data": {
    "roundId": "회차 식별자는 필수입니다."
  }
}
```

### 시스템 예외 처리

처리되지 않은 예외는 시스템 오류로 본다.

- HTTP status: `500`
- 응답 code: `INTERNAL_ERROR`
- message: 공통 시스템 메시지
- 상세 원인: `requestId`와 로그로 추적

## 11. 헤더 처리 원칙

- `X-Member-Id`는 interceptor가 아니라 controller header binding으로 처리한다.
- 출석 `POST`에서는 `@RequestHeader("X-Member-Id")`로 필수 처리한다.
- 이벤트 상세 `GET`에서는 `@RequestHeader(value = "X-Member-Id", required = false)`로 선택 처리한다.
- 헤더 누락 예외는 `GlobalExceptionHandler`가 `INVALID_REQUEST`로 변환한다.

## 12. 실제 API 응답 예시

Business 오류:

```json
{
  "code": "CONFLICT",
  "message": "이미 참여한 이벤트입니다.",
  "timestamp": "2026-03-17T11:30:00Z",
  "data": null
}
```

Validation 오류:

```json
{
  "code": "INVALID_REQUEST",
  "message": "잘못된 요청입니다.",
  "timestamp": "2026-03-17T12:00:00Z",
  "data": {
    "X-Member-Id": "X-Member-Id 헤더는 필수입니다."
  }
}
```

## 13. 로그 예시

Business 오류:

```text
requestId=ab12cd34
commonCode=CONFLICT
domainCode=ENTRY_ALREADY_APPLIED
message=이미 참여한 이벤트입니다.
```

Validation 오류:

```text
requestId=ab12cd34
commonCode=INVALID_REQUEST
message=X-Member-Id 헤더는 필수입니다.
```

## 14. RequestId 처리 원칙

- API 응답 body에는 `requestId`를 포함하지 않는다.
- `requestId`는 헤더 `X-Request-Id`와 MDC 로그 컨텍스트에서 관리한다.
- 클라이언트가 보낸 `X-Request-Id`가 없으면 `RequestIdFilter`가 새 값을 생성한다.
- 운영 분석은 응답 body가 아니라 ELK 로그의 `requestId`로 수행한다.

## 15. 핵심 요약

- DTO Validation -> annotation message
- Business Error -> domain code enum
- API Response -> `CommonCode`
- Header required/optional -> controller `@RequestHeader`
- Exception 처리 -> `GlobalExceptionHandler`
