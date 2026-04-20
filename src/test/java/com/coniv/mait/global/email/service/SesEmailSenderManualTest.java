package com.coniv.mait.global.email.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Disabled;
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
	"mait.email.from-address=MAIT <no-reply@mait.kr>"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SES_TEST_TO_ADDRESS", matches = ".+")
class SesEmailSenderManualTest {

	@Autowired
	private EmailSender emailSender;

	@Test
	@Disabled
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
	@Disabled
	@DisplayName("팀 탈퇴 알림 이메일(OWNER용)을 실제 AWS SES로 발송한다 (수동 실행용)")
	void sendTeamMemberLeftOwnerEmail() {
		String toAddress = "boysoeng@g.hongik.ac.kr";

		TeamMemberLeftEvent event = TeamMemberLeftEvent.builder()
			.memberName("홍길동")
			.teamName("MAIT 개발팀")
			.memberEmail("leaver@example.com")
			.ownerEmail(toAddress)
			.build();

		EmailMessage message = EmailMessage.builder()
			.toAddresses(List.of(toAddress))
			.subject(TeamMemberLeftEmailTemplate.ownerSubject(event))
			.textBody(TeamMemberLeftEmailTemplate.ownerTextBody(event))
			.htmlBody(TeamMemberLeftEmailTemplate.ownerHtmlBody(event))
			.build();

		EmailSendResult result = emailSender.send(message);

		assertThat(result.messageId()).isNotBlank();
		System.out.println("[SES] 팀 탈퇴 메일(OWNER) 발송 성공 — messageId: " + result.messageId());
	}

	@Test
	@DisplayName("팀 탈퇴 완료 이메일(본인용)을 실제 AWS SES로 발송한다 (수동 실행용)")
	void sendTeamMemberLeftSelfEmail() {
		String toAddress = "boysoeng@g.hongik.ac.kr";

		TeamMemberLeftEvent event = TeamMemberLeftEvent.builder()
			.memberName("홍길동")
			.teamName("MAIT 개발팀")
			.memberEmail(toAddress)
			.ownerEmail(null)
			.build();

		EmailMessage message = EmailMessage.builder()
			.toAddresses(List.of(toAddress))
			.subject(TeamMemberLeftEmailTemplate.memberSubject(event))
			.textBody(TeamMemberLeftEmailTemplate.memberTextBody(event))
			.htmlBody(TeamMemberLeftEmailTemplate.memberHtmlBody(event))
			.build();

		EmailSendResult result = emailSender.send(message);

		assertThat(result.messageId()).isNotBlank();
		System.out.println("[SES] 팀 탈퇴 메일(본인) 발송 성공 — messageId: " + result.messageId());
	}
}
