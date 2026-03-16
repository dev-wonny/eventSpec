# Attendance Check Spec

출석체크는 현재 프로젝트의 1차 개발 범위다. 이 디렉터리의 문서는 출석체크 기능 정의를 요구사항부터 테스트 기준까지 한 흐름으로 정리하기 위한 문서다.

## 문서 구성

- `use-cases.md`: 사용자 시나리오와 요구사항
- `business-rules.md`: 출석 정책과 검증 규칙
- `api-spec.md`: 제공 예정 API 목록을 반영할 계약 문서
- `data-spec.md`: 제공된 DDL을 반영한 데이터 문서
- `exception-handling.md`: try-catch, 외부 API 실패, 트랜잭션/롤백 정책
- `test-scenarios.md`: 테스트 및 인수 조건
- `open-questions.md`: 구현 전 확정이 필요한 항목
- `traceability.md`: 요구사항과 규칙/API/데이터/테스트 연결 상태

## 현재 상태

- 요구사항/규칙: DDL 및 도메인 설명 반영
- API 계약: 최소 API 목록 대기
- 데이터 구조: DDL 반영 완료
- 테스트 시나리오: DDL 기반으로 보정 완료

## 현재 핵심 해석

- 출석 단위는 `event_round`다.
- 월간 출석 이벤트는 날짜 수만큼 `event_round`를 가진다.
- `event_applicant`는 이벤트 참여자 앵커로 재사용된다.
- `event_entry`는 출석 성공 및 향후 랜덤 리워드 참여 이력을 append-only로 누적한다.
- `event_win`은 실제 지급된 보상과 외부 보상 API 성공 이력을 남긴다.
- 현재 출석 성공은 외부 point API 성공까지 포함한다.
- 현재 DDL만으로는 일자별 중복 출석이 DB에서 직접 강제되지 않으므로, 애플리케이션 규칙이 중요하다.

## 문서 사용 순서

1. `use-cases.md`로 기능 목표를 확인한다.
2. `business-rules.md`로 정책을 좁힌다.
3. `api-spec.md`와 `data-spec.md`에 입력물(DDL/API)을 연결한다.
4. `exception-handling.md`로 예외 및 rollback 흐름을 고정한다.
5. `test-scenarios.md`로 구현 완료 기준을 검증한다.
6. `traceability.md`로 연결 누락이 없는지 확인한다.
7. 합의되지 않은 내용은 `open-questions.md`에 남긴다.

## 기능 식별자 prefix

- 출석체크 관련 문서는 `ATT` prefix를 사용한다.
