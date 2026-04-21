package com.coniv.mait.domain.team.event;

import com.coniv.mait.global.email.template.EmailLayoutTemplate;
import com.coniv.mait.global.email.util.HtmlEscapeUtils;

public final class TeamDeletedEmailTemplate {

	private TeamDeletedEmailTemplate() {
	}

	public static String subject(final TeamDeletedEvent event) {
		return EmailLayoutTemplate.subject(
			String.format("%s 팀이 삭제되었습니다", event.teamName())
		);
	}

	public static String textBody(final TeamDeletedEvent event, final MemberEmailInfo recipient) {
		return String.format(
			"안녕하세요, %s님.%n%s 팀이 삭제되었습니다.%n%n"
				+ "팀과 연결된 초대, 문제 풀이, 실시간 참여 기능은 더 이상 사용할 수 없습니다.%n"
				+ "삭제 후 7일 이내 복구가 필요하면 MAIT 운영팀에 문의해주세요.%n%n감사합니다.",
			recipient.name(), event.teamName()
		);
	}

	public static String htmlBody(final TeamDeletedEvent event, final MemberEmailInfo recipient) {
		return EmailLayoutTemplate.wrap(contentHtml(event, recipient));
	}

	private static String contentHtml(final TeamDeletedEvent event, final MemberEmailInfo recipient) {
		String memberName = HtmlEscapeUtils.escape(recipient.name());
		String teamName = HtmlEscapeUtils.escape(event.teamName());
		return String.format("""
			<h1 style="margin:0 0 20px 0;font-size:20px;line-height:1.4;font-weight:700;color:#1e2124;">
				%s 팀이 삭제되었습니다
			</h1>
			<p style="margin:0 0 16px 0;font-size:15px;line-height:1.7;color:#33363d;">
				안녕하세요, <strong style="color:#0b50d0;">%s</strong>님.<br>
				<strong style="color:#0b50d0;">%s</strong> 팀이 삭제되었습니다.
			</p>
			<div
				style="margin:20px 0;padding:16px 20px;background-color:#ecf2fe;
				border-left:3px solid #256ef4;border-radius:6px;">
				<p style="margin:0;font-size:14px;line-height:1.6;color:#33363d;">
					팀과 연결된 초대, 문제 풀이, 실시간 참여 기능은 더 이상 사용할 수 없습니다.
				</p>
			</div>
			<p style="margin:0;font-size:14px;line-height:1.6;color:#6d7882;">
				삭제 후 7일 이내 복구가 필요하면 MAIT 운영팀에 문의해주세요.
			</p>
			<p style="margin:24px 0 0 0;font-size:14px;line-height:1.6;color:#6d7882;">
				감사합니다.
			</p>
			""",
			teamName,
			memberName,
			teamName
		);
	}
}
