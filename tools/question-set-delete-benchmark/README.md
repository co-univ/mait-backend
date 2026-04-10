# Question Set Delete Benchmark

문제 셋 삭제 API의 단건 분석과 k6 부하 테스트를 반복 측정하기 위한 실행 가이드다.

## 1. 준비

- 앱 실행: `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`
- MySQL 컨테이너: podman에서 실행 중인 컨테이너 사용
- k6 설치: `brew install k6`
- 인증 토큰 준비: 삭제 API를 호출할 수 있는 관리자 권한 토큰 필요

권장 환경 변수:

```bash
export MYSQL_CONTAINER=mysql8
export MYSQL_USER=root
export MYSQL_PASSWORD=your-password
export BASE_URL=http://localhost:8080
export ACCESS_TOKEN=your-token
```

## 2. 테스트 데이터 생성 전략

기본 구조는 API로 만들고, 대량 풀이 이력은 DB로 채우는 혼합 방식 기준이다.

### API로 만들어야 하는 데이터

- 문제 셋 생성: `POST /api/v1/question-sets`
- 문제 이미지 업로드: `POST /api/v1/question-sets/{questionSetId}/questions/images`
- 문제 생성: `POST /api/v1/question-sets/{questionSetId}/questions?type={type}`
- 문제 셋 완료: `PUT /api/v1/question-sets/{questionSetId}`
- 실시간 종료/재시작이 필요하면 관련 문제 셋 API 사용

Swagger UI 또는 기존 FE 요청 payload를 그대로 재사용하는 것을 권장한다.

### DB로 채우는 데이터

- `LIVE + AFTER`
  - 참가자 50명
  - 제출 기록 문제당 50개
  - scorer 문제당 최대 1개
- `STUDY + 진행 이력 있음`
  - 세션 50개
  - draft 문제당 50 x 20
  - 제출 기록 문제당 50개

실제 부하를 보려면 문제 셋 10~20개를 준비해서 삭제 대상 ID를 JSON 파일로 정리한다.

## 3. 삭제 1건 분석

digest 집계 초기화:

```bash
./tools/question-set-delete-benchmark/mysql-metrics.sh reset-digest
```

slow query log 잠깐 켜기:

```bash
./tools/question-set-delete-benchmark/mysql-metrics.sh enable-slow 0.05
./tools/question-set-delete-benchmark/mysql-metrics.sh show-slow-config
```

필요하면 general log도 아주 짧게 켠다:

```bash
./tools/question-set-delete-benchmark/mysql-metrics.sh enable-general
./tools/question-set-delete-benchmark/mysql-metrics.sh show-general-config
```

삭제 API 1건 호출:

```bash
curl -X DELETE "${BASE_URL}/api/v1/question-sets/{questionSetId}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

digest 결과 확인:

```bash
./tools/question-set-delete-benchmark/mysql-metrics.sh top-digest 50
```

로그 종료:

```bash
./tools/question-set-delete-benchmark/mysql-metrics.sh disable-general
./tools/question-set-delete-benchmark/mysql-metrics.sh disable-slow
```

## 4. k6 부하 테스트

삭제 대상 ID 파일 예시는 `question-set-ids.sample.json`을 참고한다.

LIVE + AFTER:

```bash
k6 run \
  -e BASE_URL="${BASE_URL}" \
  -e ACCESS_TOKEN="${ACCESS_TOKEN}" \
  -e IDS_FILE=./tools/question-set-delete-benchmark/question-set-ids.sample.json \
  -e TARGET_GROUP=liveAfter \
  -e VUS=1 \
  -e ITERATIONS=2 \
  ./tools/question-set-delete-benchmark/delete-question-set.k6.js
```

STUDY + 진행 이력 있음:

```bash
k6 run \
  -e BASE_URL="${BASE_URL}" \
  -e ACCESS_TOKEN="${ACCESS_TOKEN}" \
  -e IDS_FILE=./tools/question-set-delete-benchmark/question-set-ids.sample.json \
  -e TARGET_GROUP=studyWithProgress \
  -e VUS=1 \
  -e ITERATIONS=2 \
  ./tools/question-set-delete-benchmark/delete-question-set.k6.js
```

그 다음 `VUS=2`, `VUS=3`으로 올려서 p95, p99와 실패율을 비교한다.

## 5. 비교 지표

개선 전/후 모두 같은 방식으로 아래를 기록한다.

- 삭제 1건 latency
- k6 avg / p95 / p99
- digest 기준 상위 SQL count
- slow query 개수
- 가장 많이 호출된 DELETE / SELECT 패턴

최종 비교는 아래 문장으로 요약할 수 있어야 한다.

- `LIVE + AFTER 삭제 1건 평균 Xms -> Yms`
- `STUDY + 진행 이력 있음 삭제 1건 평균 Xms -> Yms`
- `총 SQL 수 A -> B`
- `개선 포인트: subtype bulk delete, participant bulk delete`
