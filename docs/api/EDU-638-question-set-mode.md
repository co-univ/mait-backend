# EDU-638 문제셋 모드 변경

문제셋 카드의 실시간↔학습 변경은 제작 완료용 PUT 대신 전용 PATCH를 사용한다.
해당 팀의 MAKER 또는 OWNER만 호출할 수 있다.

## 실시간↔학습 변경

```http
PATCH /api/v1/question-sets/{questionSetId}/solve-mode
Authorization: Bearer <accessToken>
Content-Type: application/json

{"solveMode":"STUDY"}
```

- `solveMode`는 필수이며 `STUDY`, `LIVE_TIME`을 지원한다.
- `BEFORE` 상태에서만 허용한다. 같은 모드로 재요청해도 동일 상태를 유지하며 성공한다.
- 개인 팀은 `LIVE_TIME`으로 변경할 수 없다.
- 제목·난이도·카테고리·문제 번호·상태·풀이 기록은 보존한다.
- 성공 시 HTTP 200과 기존 단건 조회와 동일한 `QuestionSetApiResponse`를 반환한다.
  `data.solveMode`와 파생 값인 `data.deliveryMode`는 요청한 모드이고, `data.status`는 `BEFORE`다.
  `categories`, `questionCount`, `questionTypeCounts`도 실제 조회 결과를 반환한다.

## 복습 전환

```http
PATCH /api/v1/question-sets/{questionSetId}/review
Authorization: Bearer <accessToken>
```

- 기존 경로를 유지하며 팀의 MAKER/OWNER 권한 검증을 추가한다.
- `AFTER` 상태의 실시간·학습 문제셋만 `REVIEW`로 전환한다.
- 원래 `solveMode`와 풀이 기록을 보존한다. `deliveryMode`는 `REVIEW`로 계산된다.
- 성공 응답은 기존과 동일한 HTTP 200, `ApiResponse.noContent()`다. 화면 갱신 시 목록/상세를 다시 조회한다.

## 상태별 계약

| 상태 | 실시간↔학습 PATCH | 복습 PATCH | 최종 저장 PUT |
| --- | --- | --- | --- |
| MAKING | 불가 | 불가 | 가능 |
| BEFORE | 가능 | 불가 | 불가 |
| ONGOING | 불가 | 불가 | 불가 |
| AFTER | 불가 | 가능 | 불가 |
| REVIEW | 불가 | 불가 | 불가 |

복습 문제셋을 다른 풀이 방식으로 재사용할 때는 복제 API에서 모드를 선택한다.
기존 재시작 API는 모드를 바꾸지 않는 별도 기능으로 유지한다.

## 오류

| 상황 | HTTP | code |
| --- | --- | --- |
| solveMode 누락/null/지원하지 않는 값 | 400 | C-001 |
| BEFORE가 아닌 상태의 풀이 방식 변경 | 400 | 0004 |
| AFTER가 아닌 상태의 복습 전환 | 400 | 0001 |
| MAKING이 아닌 상태의 최종 저장 | 400 | 0005 |
| 개인 팀에서 LIVE_TIME 요청 | 400 | 2003 |
| 해당 팀 PLAYER의 요청 | 403 | C-008 |
| 문제셋/사용자/팀이 없거나 해당 팀에 미가입 | 404 | C-002 |

여러 조건을 동시에 위반한 경우 서버의 검증 순서에 따라 오류가 반환된다.
권한·미가입 응답은 기존 TeamRoleValidator 계약을 따른다.

## 프론트 전환 및 검증

1. 모드 변경 버튼에서 제목·난이도·카테고리를 보내던 PUT 호출을 새 PATCH로 바꾼다.
2. 제작 화면의 최종 저장은 기존 PUT을 유지한다. 이제 MAKING 상태와 팀 권한을 검증한다.
3. 변경 성공 시 응답의 모드/상태를 목록과 상세에 반영한다. 복습 전환 후에는 다시 조회한다.
4. 새 백엔드와 프론트 호출 전환을 함께 배포한다. 기존 프론트의 모드 변경 PUT은 400/0005로 거절된다.
5. 개발 환경에서 카테고리와 난이도가 있는 BEFORE 문제셋으로 양방향 변경 후 값이 유지되는지 확인한다.
6. PLAYER/타 팀 사용자, ONGOING/AFTER/REVIEW, 개인 팀 LIVE_TIME 요청을 직접 호출해 거절 여부를 확인한다.
7. 두 풀이 방식의 AFTER→REVIEW 전환 후 원래 solveMode 및 풀이 기록이 유지되는지 확인한다.

모드 변경·최종 저장·복습 전환·시작/종료·재시작은 같은 문제셋 행 잠금을 사용한다.
시작이 먼저 처리되면 뒤따른 모드 변경은 상태 오류, 모드 변경이 먼저 처리되면 이전 모드의 시작은 모드 오류로 거절된다.
DB 스키마 변경은 없다.
