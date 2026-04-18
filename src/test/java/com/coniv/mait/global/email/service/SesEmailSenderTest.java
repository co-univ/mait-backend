package com.coniv.mait.global.email.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.global.component.dto.EmailMessage;
import com.coniv.mait.global.component.dto.EmailSendResult;
import com.coniv.mait.global.config.property.EmailProperty;
import com.coniv.mait.global.exception.custom.EmailSendException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class SesEmailSenderTest {

	@Mock
	private SesV2Client sesV2Client;

	private EmailProperty emailProperty;

	private SesEmailSender sesEmailSender;

	@BeforeEach
	void setUp() {
		emailProperty = new EmailProperty();
		emailProperty.setFromAddress("no-reply@mait.kr");
		sesEmailSender = new SesEmailSender(emailProperty, sesV2Client);
	}

	@Test
	@DisplayName("SES SendEmail 요청으로 내부 이메일 메시지를 매핑한다")
	void send() {
		// given
		when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
			.thenReturn(SendEmailResponse.builder()
				.messageId("ses-message-id")
				.build());

		EmailMessage message = EmailMessage.builder()
			.toAddresses(List.of("user@mait.kr"))
			.ccAddresses(List.of("manager@mait.kr"))
			.subject("가입 인증")
			.textBody("인증 코드를 입력해주세요.")
			.htmlBody("<p>인증 코드를 입력해주세요.</p>")
			.build();

		// when
		EmailSendResult result = sesEmailSender.send(message);

		// then
		ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(sesV2Client).sendEmail(captor.capture());

		SendEmailRequest request = captor.getValue();
		assertThat(result.messageId()).isEqualTo("ses-message-id");
		assertThat(request.fromEmailAddress()).isEqualTo("no-reply@mait.kr");
		assertThat(request.destination().toAddresses()).containsExactly("user@mait.kr");
		assertThat(request.destination().ccAddresses()).containsExactly("manager@mait.kr");
		assertThat(request.content().simple().subject().data()).isEqualTo("가입 인증");
		assertThat(request.content().simple().body().text().data()).isEqualTo("인증 코드를 입력해주세요.");
		assertThat(request.content().simple().body().html().data()).isEqualTo("<p>인증 코드를 입력해주세요.</p>");
	}

	@Test
	@DisplayName("기본 Reply-To와 Configuration Set을 SES 요청에 포함한다")
	void sendWithDefaultReplyToAndConfigurationSet() {
		// given
		emailProperty.setReplyToAddress("support@mait.kr");
		emailProperty.setConfigurationSetName("mait-email-events");

		when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
			.thenReturn(SendEmailResponse.builder()
				.messageId("ses-message-id")
				.build());

		EmailMessage message = EmailMessage.html(
			"user@mait.kr",
			"가입 인증",
			"인증 코드를 입력해주세요.",
			"<p>인증 코드를 입력해주세요.</p>"
		);

		// when
		sesEmailSender.send(message);

		// then
		ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(sesV2Client).sendEmail(captor.capture());

		SendEmailRequest request = captor.getValue();
		assertThat(request.replyToAddresses()).containsExactly("support@mait.kr");
		assertThat(request.configurationSetName()).isEqualTo("mait-email-events");
	}

	@Test
	@DisplayName("SES 호출 실패를 이메일 발송 예외로 변환한다")
	void sendFail() {
		// given
		when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
			.thenThrow(SdkClientException.create("timeout"));

		EmailMessage message = EmailMessage.html(
			"user@mait.kr",
			"가입 인증",
			"인증 코드를 입력해주세요.",
			"<p>인증 코드를 입력해주세요.</p>"
		);

		// when & then
		assertThatThrownBy(() -> sesEmailSender.send(message))
			.isInstanceOf(EmailSendException.class)
			.hasMessage("이메일 발송에 실패했습니다.");
	}
}
