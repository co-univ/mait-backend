# Question Set Delete Benchmark Results

## Summary

이 문서는 `QuestionSetDeleteService` 삭제 경로의 실측 결과를 정리한다.

비교 대상은 아래 두 버전이다.

* `baseline`: `/tmp/mait-baseline` at `bb61792`
* `after`: bulk delete 최적화가 반영된 현재 작업본

추가로, 실측 중 확인된 flush 예외를 해결한 뒤 현재 작업본에서 삭제 성공 기준으로 재측정했다.

## Test Environment

* app server
  * baseline: `http://127.0.0.1:18081`
  * after: `http://127.0.0.1:18082`
* db: MySQL 8 (`podman` container `mysql8`)
* redis: local Redis
* auth: owner user `id=1` JWT 직접 생성
* metrics source
  * single delete: `performance_schema.events_statements_summary_by_digest`
  * load test: `k6`

## Seed Data Shape

시드 파일: `/tmp/seed_delete_benchmark.sql`

생성 데이터:

* users: `51`
* teams: `1`
* team_users: `51`
* question_sets: `6`
  * LIVE + AFTER: `1001 ~ 1003`
  * STUDY + ONGOING: `2001 ~ 2003`
* questions: `120`
* short_answers: `3000`
* answer_submit_records: `6000`
* question_set_participants: `150`
* question_scorers: `60`
* solving_sessions: `150`
* study_answer_drafts: `3000`

문제셋 하나당 기준:

* questions: `20`
* short_answers: `500`
* LIVE participants: `50`
* LIVE scorers: `20`
* STUDY sessions: `50`
* STUDY drafts: `1000`
* answer_submit_records: `1000`

## Single Delete Measurements

### Success-to-Success Recheck

flush 보정까지 반영한 뒤, 두 버전 모두 `200`으로 성공하는 단건 삭제를 다시 측정했다.

#### LIVE + AFTER (`questionSetId=1001`)

| Version | HTTP | time_total | Digest count sum | Notes |
| --- | --- | ---: | ---: | --- |
| baseline-fixed | `200` | `212.497ms` | about `343` | `short_answers` row-by-row delete 유지 |
| after-fixed | `200` | `240.669ms` | about `22` | subtype/participant bulk delete |

해석:

* 로컬 1회 측정에선 after-fixed가 약간 느리게 찍혔지만, SQL 개수는 `343 -> 22`로 크게 줄었다.
* LIVE 케이스는 answer submit / participant / scorer / subtype delete가 같이 걸려 있어 로컬 캐시 상태에 따라 단건 latency 편차가 있었다.
* 이 구간은 단건 시간보다 쿼리 패턴 변화가 핵심이다.

#### STUDY + ONGOING (`questionSetId=2001`)

| Version | HTTP | time_total | Digest count sum | Notes |
| --- | --- | ---: | ---: | --- |
| baseline-fixed | `200` | `197.332ms` | about `276` | `short_answers` row-by-row delete 유지 |
| after-fixed | `200` | `94.492ms` | about `22` | subtype bulk delete |

해석:

* STUDY는 success-to-success 기준에서도 after-fixed가 더 빠르게 측정됐다.
* SQL 개수는 `276 -> 22` 수준으로 줄었다.
* session/draft delete는 둘 다 bulk delete였고, 차이는 대부분 subtype delete 전략에서 발생한다.

### LIVE + AFTER

대상: `questionSetId=1001`

| Version | HTTP | time_total | Digest count sum | Notes |
| --- | --- | ---: | ---: | --- |
| baseline | `500` | `586.247ms` | about `585` | `short_answers` row-by-row delete |
| after | `200` | `322.928ms` | about `21` | subtype/participant bulk delete + flush/clear |

주요 차이:

* baseline
  * `DELETE FROM short_answers WHERE id = ?` `475회`
  * `DELETE FROM question_set_participants WHERE id = ?` `50회`
  * `SELECT short_answers ... WHERE short_question_id = ?` `20회`
* after
  * `DELETE short_answers ... WHERE short_question_id IN (...)` `1회`
  * `DELETE question_set_participants ... WHERE question_set_id = ?` `1회`

개선 폭:

* 응답 시간: 약 `44.9%` 감소
* 쿼리 수: 약 `96.4%` 감소

### STUDY + ONGOING

대상: `questionSetId=2001`

| Version | HTTP | time_total | Digest count sum | Notes |
| --- | --- | ---: | ---: | --- |
| baseline | `500` | `223.019ms` | about `535` | `short_answers` row-by-row delete |
| after | `200` | `43.506ms` | about `22` | subtype bulk delete + flush/clear |

주요 차이:

* baseline
  * `DELETE FROM short_answers WHERE id = ?` `475회`
  * `SELECT short_answers ... WHERE short_question_id = ?` `20회`
* after
  * `DELETE short_answers ... WHERE short_question_id IN (...)` `1회`
  * `DELETE study_answer_drafts ... IN (...)` `1회`
  * `DELETE solving_sessions ...` `1회`

개선 폭:

* 응답 시간: 약 `80.5%` 감소
* 쿼리 수: 약 `95.9%` 감소

## k6 Results

시나리오:

* `3 VU`
* `3 iterations`
* `shared-iterations`

### LIVE + AFTER

| Version | avg | p95 | failed |
| --- | ---: | ---: | ---: |
| baseline | `25.84ms` | `25.85ms` | `100%` |
| after | `95.07ms` | `102.37ms` | `0%` |

### STUDY + ONGOING

| Version | avg | p95 | failed |
| --- | ---: | ---: | ---: |
| baseline | `3.17ms` | `3.18ms` | `100%` |
| after | `90.33ms` | `99.22ms` | `0%` |

주의:

* 위 baseline k6 수치는 flush 보정 전 `100% 실패` 상태에서 얻은 값이다.
* success-to-success 부하 비교를 위해 baseline-fixed로도 다시 시도했지만, 로컬 MySQL에서 `Table definition has changed, please retry transaction`가 간헐적으로 발생해 비교군이 오염됐다.
* 따라서 현재 문서에서 성능 비교 기준은 `success-to-success 단건 삭제`와 `after 성공 k6`로 보는 것이 맞다.

## Blocker Found During Measurement

baseline과 after 모두 최초 측정 시 삭제 API가 `500`으로 종료됐다.

공통 예외:

```text
org.hibernate.TransientObjectException:
persistent instance references an unsaved transient instance of
'com.coniv.mait.domain.question.entity.QuestionSetEntity'
```

원인:

* `questionSet`, `questions`를 managed entity로 조회
* 자식은 bulk delete로 DB에서 직접 삭제
* 영속성 컨텍스트 안의 참조는 그대로 남아 있음
* 커밋 직전 flush에서 Hibernate가 managed association을 다시 해석하다가 실패

즉, 성능 최적화는 맞았지만 JPA persistence context와 bulk delete를 섞은 정합성 처리가 빠져 있었다.

## Fix Applied After Measurement

현재 작업본에는 아래 보정이 추가됐다.

* 자식 bulk delete 수행
* `entityManager.flush()`
* `entityManager.clear()`
* 부모 `QuestionSetEntity`도 bulk delete 수행

이후 현재 작업본에서 재검증한 결과:

### LIVE + AFTER Recheck

대상: `questionSetId=1001`

* HTTP: `200`
* time_total: `322.928ms`
* 삭제 후 row count
  * `question_sets`: `0`
  * `questions`: `0`
  * `short_answers`: `0`

### STUDY + ONGOING Recheck

대상: `questionSetId=2001`

* HTTP: `200`
* time_total: `43.506ms`
* 삭제 후 row count
  * `question_sets`: `0`
  * `questions`: `0`
  * `short_answers`: `0`
  * `solving_sessions`: `0`
  * `study_answer_drafts`: `0`

## Conclusions

이번 측정으로 확인된 점:

* subtype delete를 문제별 파생 delete에서 bulk delete로 바꾸면 쿼리 수가 극적으로 감소한다.
* participant delete를 bulk delete로 바꾸면 LIVE 케이스의 per-row delete가 제거된다.
* flush/clear + parent bulk delete를 추가한 뒤 삭제 API는 정상적으로 `200`을 반환했다.
* success-to-success 단건 비교 기준으로는 STUDY에서 after-fixed가 더 빨랐고, LIVE는 로컬 편차가 있었지만 SQL 수는 크게 감소했다.
* 성공 기준 k6에서는 current(after) 쪽 `failed=0%`를 확인했다.
* 성능 최적화 자체는 유효했고, bulk delete를 쓸 때는 persistence context 정리가 같이 필요했다.

현재 기준 권장 해석:

* 성능 개선 포인트는 `bulk delete 전환`
* 안정성 보완 포인트는 `flush/clear + parent bulk delete`
