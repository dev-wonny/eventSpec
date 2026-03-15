# Attendance Check Spec

출석체크는 현재 프로젝트의 1차 개발 범위다. 이 디렉터리의 문서는 출석체크 기능 정의를 요구사항부터 테스트 기준까지 한 흐름으로 정리하기 위한 초안이다.

## 문서 구성

- `use-cases.md`: 사용자 시나리오와 요구사항
- `business-rules.md`: 출석 정책과 검증 규칙
- `api-spec.md`: 제공 예정 API 목록을 반영할 계약 문서
- `data-spec.md`: 제공 예정 DDL을 반영할 데이터 문서
- `test-scenarios.md`: 테스트 및 인수 조건
- `open-questions.md`: 구현 전 확정이 필요한 항목
- `traceability.md`: 요구사항과 규칙/API/데이터/테스트 연결 상태

## 현재 상태

- 요구사항/규칙: 초안 작성 완료
- API 계약: 최소 API 목록 대기
- 데이터 구조: 최소 DDL 대기
- 테스트 시나리오: 초안 작성 완료

## 문서 사용 순서

1. `use-cases.md`로 기능 목표를 확인한다.
2. `business-rules.md`로 정책을 좁힌다.
3. `api-spec.md`와 `data-spec.md`에 입력물(DDL/API)을 연결한다.
4. `test-scenarios.md`로 구현 완료 기준을 검증한다.
5. `traceability.md`로 연결 누락이 없는지 확인한다.
6. 합의되지 않은 내용은 `open-questions.md`에 남긴다.

## 기능 식별자 prefix

- 출석체크 관련 문서는 `ATT` prefix를 사용한다.
