package com.coniv.mait.global.email.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.coniv.mait.domain.team.event.TeamMemberLeftEmailTemplate;
import com.coniv.mait.domain.team.event.TeamMemberLeftEvent;
import com.coniv.mait.global.component.EmailSender;
import com.coniv.mait.global.component.dto.EmailMessage;
import com.coniv.mait.global.component.dto.EmailSendResult;

@SpringBootTest(properties = {
	"mait.email.provider=ses",
	"mait.email.from-address=no-reply@mait.kr"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SES_TEST_TO_ADDRESS", matches = ".+")
class SesEmailSenderManualTest {

	@Autowired
	private EmailSender emailSender;

	@Test
	@DisplayName("실제 AWS SES로 이메일을 발송한다 (수동 실행용)")
	void sendRealEmail() {
		String toAddress = System.getenv("SES_TEST_TO_ADDRESS");

		EmailMessage message = EmailMessage.html(
			toAddress,
			"[MAIT] SES 연동 테스트",
			"이 메일은 MAIT 백엔드 SES 연동 확인용 텍스트 본문입니다.",
			"<html><body>"
				+ "<h2>MAIT SES 연동 테스트</h2>"
				+ "<p>이 메일이 도착했다면 SES 연동이 정상입니다.</p>"
				+ "</body></html>"
		);

		EmailSendResult result = emailSender.send(message);

		assertThat(result.messageId()).isNotBlank();
		System.out.println("[SES] 발송 성공 — messageId: " + result.messageId());
	}

	@Test
	@DisplayName("팀 탈퇴 알림 이메일을 실제 AWS SES로 발송한다 (수동 실행용)")
	void sendTeamMemberLeftEmail() {
		String toAddress = "boysoeng@naver.com";

		TeamMemberLeftEvent event = TeamMemberLeftEvent.builder()
			.memberName("홍길동")
			.teamName("MAIT 개발팀")
			.recipientEmails(List.of(toAddress))
			.build();

		EmailMessage message = EmailMessage.builder()
			.toAddresses(List.of(toAddress))
			.subject(TeamMemberLeftEmailTemplate.subject(event))
			.textBody(TeamMemberLeftEmailTemplate.textBody(event))
			.htmlBody(TeamMemberLeftEmailTemplate.htmlBody(event))
			.build();

		EmailSendResult result = emailSender.send(message);

		assertThat(result.messageId()).isNotBlank();
		System.out.println("[SES] 팀 탈퇴 메일 발송 성공 — messageId: " + result.messageId());
	}
}
