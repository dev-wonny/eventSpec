# Point API Feign Client

이 문서는 출석체크의 외부 point API 호출을 Spring Cloud 없이 pure Feign으로 구현하는 기준을 정리한다.

## 목적

- 외부 point API 호출 방식을 코드와 문서 기준으로 고정한다.
- `@FeignClient` 없이 OpenFeign 라이브러리만 사용하는 이유를 명확히 남긴다.
- after-commit listener와 외부 호출 adapter의 책임을 분리한다.

## 현재 환경 판단

현재 시스템 전제:

- ECS 기반 컨테이너 배포
- 단일 `event-api` 서비스
- 외부 point API 호출만 필요한 초기 MSA 수준

이 상황에서는 Spring Cloud가 하던 인프라 역할을 AWS가 이미 대부분 제공한다.

| 기능 | 현재 기준 |
| --- | --- |
| 서비스 디스커버리 | ALB / Cloud Map |
| 로드밸런싱 | ALB |
| 오토스케일링 | ECS Service |
| 네트워크 라우팅 | VPC |

결론:

- Spring Cloud는 현재 범위에 필요하지 않다.
- `@FeignClient`도 도입하지 않는다.
- 외부 HTTP 호출은 pure OpenFeign builder 기반으로 구현한다.

## 왜 pure Feign인가

- 지금 필요한 것은 "외부 point API 1개 호출"이다.
- 서비스 디스커버리, client-side load balancing, config server 같은 Spring Cloud 기능은 현재 과하다.
- Feign 인터페이스 기반 호출 방식은 유지하면서도 의존성과 복잡도를 최소화할 수 있다.
- ECS/ALB 환경에서는 인프라가 이미 discovery/LB 문제를 해결한다.

## 구현 기준

권장 구조:

```text
PointRewardAfterCommitListener
  -> PointRewardPort
      -> PointRewardImpl
          -> PointApiFeignClient
              -> Point API
```

### 역할

- `PointRewardAfterCommitListener`
  - 로컬 DB commit 이후에만 외부 호출을 시작한다.
- `PointRewardPort`
  - application이 의존하는 외부 reward 포트다.
- `PointRewardImpl`
  - Feign client를 감싸는 infrastructure adapter다.
  - request/response 변환을 담당한다.
- `PointApiFeignClient`
  - pure OpenFeign 인터페이스다.
  - `@FeignClient`를 쓰지 않고 Feign native annotation을 사용한다.

## 설정 기준

- `build.gradle`에는 Spring Cloud starter를 추가하지 않는다.
- OpenFeign core/jackson 정도만 최소 의존성으로 추가한다.
- timeout은 기존 `point-api.connect-timeout`, `point-api.read-timeout` 설정을 그대로 재사용한다.
- base URL과 grant path도 기존 `PointApiProperties`를 그대로 사용한다.

## 예외 처리 기준

- Feign이 발생시키는 timeout/connection/4xx/5xx/runtime 예외는 infrastructure에서 point 전용 예외로 다시 감싸지 않는다.
- listener는 외부 API 호출 실패를 일반 런타임 예외로 받아 사용자 응답을 바꾸지 않고 로그와 운영 알림만 남긴다.

## 비고

- 나중에 외부 호출 대상이 많아지고 공통 인증, 재시도, circuit breaker, discovery가 필요해지면 그때 Spring Cloud 도입을 다시 검토한다.
- 현재 단계에서는 pure Feign + ECS 인프라 조합이 가장 단순하다.
