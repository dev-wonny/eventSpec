# Spec-Driven Development

이 디렉터리는 구현 전에 합의해야 하는 spec 문서를 관리한다.

## 작성 원칙

1. 공통 범위와 용어를 먼저 정리한다.
2. 기능별로 요구사항과 유스케이스를 정리한다.
3. 비즈니스 규칙을 분리해서 명시한다.
4. API 계약과 데이터 구조를 연결한다.
5. 테스트 시나리오로 검증 기준을 고정한다.
6. 미정 항목은 `open-questions.md`에 모은다.

## 권장 작성 순서

1. `shared/product-scope.md`
2. `shared/glossary.md`
3. `features/<feature>/README.md`
4. `features/<feature>/use-cases.md`
5. `features/<feature>/business-rules.md`
6. `features/<feature>/api-spec.md`
7. `features/<feature>/data-spec.md`
8. `features/<feature>/exception-handling.md`
9. `features/<feature>/test-scenarios.md`
10. `features/<feature>/open-questions.md`
11. `features/<feature>/traceability.md`

## 식별자 규칙

- 요구사항: `<FEATURE>-REQ-XXX`
- 규칙: `<FEATURE>-RULE-XXX`
- API: `<FEATURE>-API-XXX`
- 데이터: `<FEATURE>-DATA-XXX`
- 테스트: `<FEATURE>-TEST-XXX`

예시:

- 출석체크 기능 prefix: `ATT`
- 랜덤 게임 기능 prefix: `RG`

## 디렉터리 구조

```text
docs/
  README.md
  spec/
    README.md
    shared/
      product-scope.md
      glossary.md
      tech-stack.md
    features/
      attendance-check/
        README.md
        use-cases.md
        business-rules.md
        api-spec.md
        data-spec.md
        exception-handling.md
        test-scenarios.md
        open-questions.md
        traceability.md
      random-game/
        README.md
    templates/
      feature-spec-template.md
```

## 운영 규칙

- 구현 코드보다 spec 문서를 먼저 수정한다.
- API, DB, 테스트가 변경되면 관련 문서를 함께 갱신한다.
- 요구사항별 연결 상태는 `traceability.md`에서 함께 확인한다.
- 새로운 DDL, API 계약, 운영 정책 입력물이 들어오면 해당 원본 기준으로 문서를 갱신한다.
