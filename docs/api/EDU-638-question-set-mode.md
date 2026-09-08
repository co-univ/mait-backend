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

| 상태 | 실시간↔학습 PATCH | 복습 PATCH |
| --- | --- | --- |
| MAKING | 불가 (0004) | 불가 (0001) |
| BEFORE | 가능 | 불가 (0001) |
| ONGOING | 불가 (0004) | 불가 (0001) |
| AFTER | 불가 (0004) | 가능 |
| REVIEW | 불가 (0004) | 불가 (0001) |

복습 문제셋을 다른 풀이 방식으로 재사용할 때는 복제 API에서 모드를 선택한다.
기존 재시작 API는 모드를 바꾸지 않는 별도 기능으로 유지한다.

## 오류

두 PATCH API 공통이다.

| 상황 | HTTP | code |
| --- | --- | --- |
| solveMode 누락/null/지원하지 않는 값 | 400 | C-001 |
| BEFORE가 아닌 상태의 풀이 방식 변경 | 400 | 0004 |
| AFTER가 아닌 상태의 복습 전환 | 400 | 0001 |
| 개인 팀에서 LIVE_TIME 요청 | 400 | 2003 |
| 해당 팀 PLAYER의 요청 | 403 | C-008 |
| 문제셋/사용자/팀이 없거나 해당 팀에 미가입 | 404 | C-002 |

여러 조건을 동시에 위반한 경우 서버의 검증 순서에 따라 오류가 반환된다.
권한·미가입 응답은 기존 TeamRoleValidator 계약을 따른다.

## 알려진 갭 — 최종 저장 PUT

`PUT /api/v1/question-sets/{questionSetId}`(제작 완료)는 이번 작업에서 **변경하지 않았다.**
이 API는 여전히 상태·팀 권한을 검증하지 않으며, `solveMode`를 인자로 받고 `status`를 무조건
`BEFORE`로 대입한다. 따라서 직접 호출하면 위 상태별 계약을 우회할 수 있다.

- 진행 중·풀이 완료·복습 문제셋에 호출하면 `status`가 `BEFORE`로 역행하고 모드도 바뀐다.
- 개인 팀에서는 이어지는 `markOngoingOnComplete()`로 `startTime`이 재설정된다.
- `categoryIds`를 비우거나 생략하면 기존 카테고리 매핑이 모두 삭제된다.

기존 동작을 그대로 유지했으므로 이번 배포로 새로 생기는 문제는 없다. 이 우회 경로의 차단은
별도 티켓에서 다룬다. 그 티켓에서는 `difficulty`를 `BEFORE` 상태에서 수정할 대체 API가 없다는
점도 함께 결정해야 한다 (현재는 이 PUT이 유일한 경로다).

## 프론트 전환 및 검증

1. 모드 변경 버튼에서 제목·난이도·카테고리를 보내던 PUT 호출을 새 PATCH로 바꾼다.
   대상은 `useManagementMoveQuestionSet.ts`의 `updateQuestionSetMode`다.
   PATCH는 `solveMode` 하나만 보내면 되므로 변경 직전 단건 조회로 본문을 채울 필요가 없다.
   상태·권한 재확인이 목적이었다면 그 조회는 유지해도 된다.
2. 제작 화면의 최종 저장은 기존 PUT을 그대로 유지한다. 요청·응답 계약에 변화가 없다.
3. 변경 성공 시 응답의 모드/상태를 목록과 상세에 반영한다. 복습 전환 후에는 다시 조회한다.
4. 배포 순서 제약은 없다. 기존 프론트의 모드 변경 PUT은 계속 동작하므로 FE 전환을 나중에 해도 된다.
5. 개발 환경에서 카테고리와 난이도가 있는 BEFORE 문제셋으로 양방향 변경 후 값이 유지되는지 확인한다.
6. PLAYER/타 팀 사용자, ONGOING/AFTER/REVIEW, 개인 팀 LIVE_TIME 요청을 직접 호출해 거절 여부를 확인한다.
7. 두 풀이 방식의 AFTER→REVIEW 전환 후 원래 solveMode 및 풀이 기록이 유지되는지 확인한다.

모드 변경과 실시간/학습 시작은 같은 문제셋 행 잠금을 사용한다.
시작이 먼저 처리되면 뒤따른 모드 변경은 상태 오류, 모드 변경이 먼저 처리되면 이전 모드의 시작은 모드 오류로 거절된다.
DB 스키마 변경은 없다.
