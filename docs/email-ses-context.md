# Email SES Implementation Context

## Current Decision

사용자 대상 이메일 발송은 별도 서버로 분리하지 않고, MAIT 백엔드 내부에서 코드 책임만 분리한다.

현재 선택한 구조는 다음과 같다.

```text
Domain service
  -> EmailService 또는 EmailTemplateRenderer
  -> EmailSender
  -> SesEmailSender
  -> AWS SES API
```

이번 커밋에서는 `EmailService`나 템플릿 렌더러까지 만들지는 않고, 발송 포트와 SES 구현체만 먼저 추가했다.

## Implemented Scope

- `EmailSender` 인터페이스를 추가했다.
- `EmailMessage`, `EmailSendResult` 내부 DTO를 추가했다.
- `SesEmailSender`가 AWS SES v2 `SendEmail` API를 호출한다.
- `ConsoleEmailSender`를 추가해 local/test 환경에서는 실제 이메일을 보내지 않는다.
- `EmailProperty`로 발신자, Reply-To, configuration set 설정을 받는다.
- SES 발송 실패는 `EmailSendException`으로 감싼다.
- `SesEmailSenderTest`로 SES 요청 매핑과 예외 변환을 검증한다.

## Runtime Configuration

기본 설정은 `application.yml`에 있다.

```yaml
mait:
  email:
    provider: ${EMAIL_PROVIDER:ses}
    from-address: ${EMAIL_FROM_ADDRESS:no-reply@mait.kr}
    reply-to-address: ${EMAIL_REPLY_TO_ADDRESS:}
    configuration-set-name: ${AWS_SES_CONFIGURATION_SET_NAME:}
```

local/test는 기본값이 `console`이라 실제 발송하지 않는다.

```yaml
mait:
  email:
    provider: ${EMAIL_PROVIDER:console}
```

SES credential은 다음 순서로 동작한다.

- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`가 있으면 static credential 사용
- 값이 없으면 AWS SDK default credential chain 사용

운영 환경에서는 가능하면 access key보다 ECS/EC2/EKS IAM Role을 쓰는 것이 낫다.

## AWS Setup Needed

운영 발송 전에 AWS에서 필요한 작업은 다음과 같다.

1. SES 리전을 `ap-northeast-2`로 사용할지 확정한다.
2. SES verified identity로 `mait.kr` 도메인을 등록한다.
3. DNS에 SES DKIM 레코드를 추가한다.
4. SPF, DMARC 정책을 점검한다.
5. SES sandbox를 production access로 전환한다.
6. IAM 권한에 `ses:SendEmail`을 부여한다.
7. 가능하면 IAM condition으로 `ses:FromAddress = no-reply@mait.kr`만 허용한다.
8. bounce/complaint 추적이 필요하면 SES configuration set과 SNS/EventBridge/CloudWatch 연동을 만든다.

## Common Header And Footer

SES 자체에 모든 메일 본문에 공통 HTML header/footer를 자동 삽입하는 전역 설정은 없다.

공통 레이아웃은 애플리케이션 템플릿 계층에서 처리하는 쪽이 맞다.

```text
EmailTemplateRenderer
  -> layout 적용
  -> text/html body 생성
  -> EmailSender.send(...)
```

추후 메일 종류가 생기면 `EmailSender`를 직접 호출하지 말고, `EmailService` 또는 `EmailTemplateRenderer`를 앞단에 둔다.

## Server Separation

현재 요구사항에서는 이메일 발송 서버를 분리하지 않는다.

분리 기준은 다음 상황이 생겼을 때 다시 검토한다.

- 발송량이 커져 메인 API 응답 시간이나 리소스에 영향을 준다.
- 지연 발송, 예약 발송, 대량 발송이 필요하다.
- 재시도, 발송 이력, bounce/complaint 처리가 별도 도메인처럼 커진다.
- 여러 서비스가 공통 이메일 발송 기능을 사용한다.
- 메일 장애를 API 서버와 강하게 격리해야 한다.

그 전까지는 같은 백엔드 내부에서 다음 순서로 확장한다.

```text
1. EmailSender + SesEmailSender
2. EmailService + EmailTemplateRenderer
3. @TransactionalEventListener(AFTER_COMMIT) 기반 비동기 발송
4. email_outbox 테이블과 재시도 워커
5. 필요 시 outbox/queue consumer를 별도 서버로 분리
```

## Validation

이번 구현에서 실행한 검증은 다음과 같다.

```bash
./gradlew compileJava
./gradlew test --tests com.coniv.mait.global.email.service.SesEmailSenderTest
./gradlew checkstyleMain checkstyleTest
./gradlew test
```
