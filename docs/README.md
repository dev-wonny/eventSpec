# Docs

프로젝트 문서는 `docs/spec`를 기준으로 관리한다.

- `shared`: 기능 공통 범위, 용어, 기술 제약
- `features`: 기능별 spec 문서
- `templates`: 신규 기능 확장 시 재사용할 템플릿

공통 설계 기준은 아래 문서를 함께 본다.

- 패키지 구조 기준: `docs/spec/shared/package-structure.md`
- 도메인 아키텍처 기준: `docs/spec/shared/domain-architecture-guide.md`
- 코어 도메인 기준: `docs/spec/shared/core-domain-design.md`
- 엔티티 예제 기준: `docs/spec/shared/entity-example-guide.md`
- 조회 전략 기준: `docs/spec/shared/query-strategy-guide.md`
- 주석 작성 기준: `docs/spec/shared/comment-writing-guide.md`
- 로그/관측성 기준: `docs/spec/shared/logging-observability-spec.md`
- 응답/코드 패키지 기준: `docs/spec/shared/code-package-response-spec.md`
- OpenAPI 응답 문서화 기준: `docs/spec/shared/openapi-response-schema-guide.md`
- 기술 스택 기준: `docs/spec/shared/tech-stack.md`

현재 1차 개발 범위는 출석체크이며, 랜덤 게임은 문서 자리만 먼저 확보한다.

새 입력물이 들어오면 아래 문서를 우선 갱신한다.

- 반영 완료 DDL 기준 데이터 해석: `docs/spec/features/attendance-check/data-spec.md`
- 확정된 외부 API 계약 반영: `docs/spec/features/attendance-check/api-spec.md`
