package com.coniv.mait.domain.team.event;

import com.coniv.mait.global.email.template.EmailLayoutTemplate;
import com.coniv.mait.global.email.util.HtmlEscapeUtils;

final class TeamMemberLeftEmailTemplate {

	private TeamMemberLeftEmailTemplate() {
	}

	static String subject(final TeamMemberLeftEvent event) {
		return String.format("%s님이 %s 팀을 탈퇴하였습니다", event.memberName(), event.teamName());
	}

	static String textBody(final TeamMemberLeftEvent event) {
		return String.format(
			"안녕하세요.%n%s님이 %s 팀을 탈퇴하였습니다.%n%n%s팀에 대한 접근 권한 및 데이터는 모두 삭제됩니다.%n%n감사합니다.",
			event.memberName(), event.teamName(), event.teamName()
		);
	}

	static String htmlBody(final TeamMemberLeftEvent event) {
		return EmailLayoutTemplate.wrap(contentHtml(event));
	}

	private static String contentHtml(final TeamMemberLeftEvent event) {
		String memberName = HtmlEscapeUtils.escape(event.memberName());
		String teamName = HtmlEscapeUtils.escape(event.teamName());
		return String.format("""
				<h1 style="margin:0 0 20px 0;font-size:20px;line-height:1.4;font-weight:700;color:#1e2124;">
				  %s님이 %s 팀을 탈퇴하였습니다
				</h1>
				<p style="margin:0 0 16px 0;font-size:15px;line-height:1.7;color:#33363d;">
				  안녕하세요.<br>
				  <strong style="color:#0b50d0;">%s</strong>님이 <strong style="color:#0b50d0;">%s</strong> 팀을 탈퇴하였습니다.
				</p>
				<div style="margin:20px 0;padding:16px 20px;background-color:#ecf2fe;border-left:3px solid #256ef4;border-radius:6px;">
				  <p style="margin:0;font-size:14px;line-height:1.6;color:#33363d;">
				    %s 팀에 대한 접근 권한 및 데이터는 모두 삭제됩니다.
				  </p>
				</div>
				<p style="margin:24px 0 0 0;font-size:14px;line-height:1.6;color:#6d7882;">
				  감사합니다.
				</p>
				""",
			memberName, teamName,
			memberName, teamName,
			teamName
		);
	}
}
