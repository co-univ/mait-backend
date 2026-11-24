# 문제 제작 화면의 순서 처리 가이드

EDU-180은 제작 중이거나 풀이 전인 문제셋의 순서 키를 서버에서 주기적으로 재배치한다. 재배치 전후의 문제 ID와 화면 순서는 같다. FE에서 키를 계산하거나 배치 시각에 맞춰 목록을 새로 고칠 필요는 없다. 지금 제작 화면에서 손볼 곳은 문제 추가 실패 안내다. 문제 간 드래그 정렬을 제공할 때에는 아래 이동 API도 연결한다. [EDU-180](https://youthing.atlassian.net/browse/EDU-180) · [기존 순서 변경 API 및 동시 편집 처리](https://github.com/co-univ/mait-backend/pull/372)

## 화면별 반영 사항

| 화면 | 사용자 동작 | FE 처리 |
|---|---|---|
| 새 문제셋 만들기 · AI 생성 | 수동 제작 또는 AI 생성을 시작한다. | 기존 문제셋 생성 요청과 AI 완료 확인 흐름을 사용한다. 순서 키는 서버가 할당한다. [생성 훅](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/domains/creation/hooks/new/useCreationNewQuestionSet.ts#L247) |
| 문제 제작 · 풀이 전 재편집 | 왼쪽 목록에서 문제를 추가한다. | 기존 `POST …/questions/default`와 성공 후 목록 재조회를 유지한다. 현재 공통 실패 토스트를 아래의 오류 코드별 안내로 나눈다. 제작용 목록은 `GET …/questions?mode=MAKING` 응답 순서로 표시하고, 선택한 문제는 ID로 추적한다. [추가 및 오류 처리](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/domains/creation/hooks/question/useCreationQuestionSet.ts#L161) |
| 문제 제작 · 문제 간 순서 변경 | 문제 카드 자체를 다른 위치로 옮긴다. | 현재 내비게이션에는 선택·추가·삭제가 연결돼 있다. 드래그 기능 도입 시 아래 PATCH를 연결한다. 편집 중 문제 유형을 바꾸면 저장 응답의 ID가 달라질 수 있으므로 저장을 마치고 최신 ID로 이웃을 구한다. [문제 내비게이션](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/domains/creation/components/question/CreationQuestionNavigation.tsx#L189) |
| 순서형 문제의 답안 편집 | 한 문제 안에서 정답 보기 순서를 바꾼다. | 기존 답안 옵션 편집 및 문제 저장을 사용한다. 이 화면에서 재정렬하는 대상은 한 문제 안의 보기다. [답안 드래그 구현](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/domains/creation/pages/question/answer/CreationQuestionAnswerOrderingDragDrop.tsx#L31) |
| 발행 · 풀이 관리 · 학습 · 복습 · 대시보드 | 문제셋을 발행하거나 최종 번호로 문제를 본다. | 발행 요청에 순서 키를 추가할 필요가 없다. 조회 화면은 각 모드의 기존 목록과 문제 번호를 사용한다. [발행 요청](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/domains/creation/hooks/publish/useCreationPublishQuestionSet.ts#L140) · [복습 목록](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/domains/solving/hooks/review/useSolvingReviewQuestions.ts#L25) |

**생성 API 구분.** 현재 FE에서 호출하지 않는 경로는 `POST /api/v1/question-sets/{questionSetId}/questions?type={type}`이다. **제작 화면의 추가 버튼이 호출하는 경로는 `POST /api/v1/question-sets/{questionSetId}/questions/default`**다. 작성한 내용은 `PUT /api/v1/question-sets/{questionSetId}/questions/{questionId}`로 저장한다. 일반 생성 API의 제거 여부는 별도 백로그에서 검토한다. [추가 버튼 호출](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/domains/creation/hooks/question/useCreationQuestionSet.ts#L108) · [문제 저장 호출](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/domains/creation/hooks/question/useCreationQuestion.ts#L91) · [EDU-292](https://youthing.atlassian.net/browse/EDU-292)

## 드래그 기능 도입 시 요청 예시

문제 ID 목록이 `[11, 22, 33, 44]`이고 사용자가 44를 11과 22 사이로 옮겼다면, 이동 후 배열에서 44의 바로 앞뒤 ID를 보낸다.

```http
PATCH /api/v1/question-sets/7/questions/44/orders
Content-Type: application/json

{"prevQuestionId": 11, "nextQuestionId": 22}
```

맨 앞이면 앞 ID를, 맨 뒤면 뒤 ID를 생략할 수 있다. 자리가 같은 드롭이나 문제가 하나뿐인 목록에서는 요청을 생략한다. 성공하면 제작 목록을 다시 조회한다. 화면에서 먼저 순서를 바꿔 보여 줬다면 실패 시 서버 목록을 복원한다. [요청 타입](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/libs/types/api.d.ts#L2805)

| HTTP 409 응답의 `code` | 발생 상황 | 화면 동작 |
|---|---|---|
| `T-003` | 이동을 요청한 앞뒤 문제가 현재 목록에서 이웃 관계를 벗어났다. | 목록을 새로 받고 “문제 순서가 바뀌었어요. 새 목록에서 위치를 다시 선택해 주세요.”라고 안내한다. |
| `T-004` | 요청한 위치나 목록 끝에 사용할 순서 키가 부족하다. | 추가·이동을 성공 처리하지 않는다. “지금은 이 위치에 추가할 수 없어요. 순서 정리 후 다시 시도해 주세요.”라고 안내한다. 같은 요청을 즉시 반복해도 배치를 앞당길 수 없다. |
| `C-010` | 서버가 이번 요청에서 잠금을 획득하지 못했다. | 목록을 다시 받고 잠시 후 사용자가 재시도할 수 있게 한다. |

`apiClient.PATCH` 등 클라이언트를 직접 쓰면 반환값의 `response.ok`와 오류 본문의 `code`를 확인한 뒤 성공 UI를 표시한다. 오류 안내를 적용할 때는 화면 순서만 복구하고 사용자가 입력 중인 문제 본문은 보존한다. [FE API 클라이언트](https://github.com/co-univ/mait-frontend/blob/9c51f53654d1121b680762fae9686e3026d17fa0/src/libs/api/client.ts#L58)

## 화면에서 확인할 사례

- 제작 화면에서 문제를 추가하면 응답으로 받은 ID가 선택되고 목록 끝에 한 번 표시된다.
- 두 탭에서 같은 위치로 이동했을 때 두 번째 탭이 `T-003`을 받으면 최신 순서가 나타난다. 드래그 기능을 연결한 뒤 검증한다.
- 재배치 전후에 같은 제작 목록을 불러오면 문제 ID의 배열과 선택한 문제가 같다. 이전에 `T-004`였던 위치도 해당 문제셋의 배치가 성공한 뒤 이동할 수 있다.
