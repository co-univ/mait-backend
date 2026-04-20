package com.coniv.mait.domain.team.event;

import com.coniv.mait.global.email.template.EmailLayoutTemplate;
import com.coniv.mait.global.email.util.HtmlEscapeUtils;

public final class TeamMemberLeftEmailTemplate {

	private TeamMemberLeftEmailTemplate() {
	}

	public static String ownerSubject(final TeamMemberLeftEvent event) {
		return EmailLayoutTemplate.subject(
			String.format("%s님이 %s 팀을 탈퇴하였습니다", event.memberName(), event.teamName())
		);
	}

	public static String ownerTextBody(final TeamMemberLeftEvent event) {
		return String.format(
			"안녕하세요.%n%s님이 %s 팀을 탈퇴하였습니다.%n%n%s 팀에 대한 접근 권한이 삭제됩니다.%n%n감사합니다.",
			event.memberName(), event.teamName(), event.teamName()
		);
	}

	public static String ownerHtmlBody(final TeamMemberLeftEvent event) {
		return EmailLayoutTemplate.wrap(ownerContentHtml(event));
	}

	public static String memberSubject(final TeamMemberLeftEvent event) {
		return EmailLayoutTemplate.subject(
			String.format("%s 팀 탈퇴가 완료되었습니다", event.teamName())
		);
	}

	public static String memberTextBody(final TeamMemberLeftEvent event) {
		return String.format(
			"안녕하세요.%n%s 팀 탈퇴가 완료되었습니다.%n%n%s 팀에 대한 접근 권한이 삭제됩니다.%n%n감사합니다.",
			event.teamName(), event.teamName()
		);
	}

	public static String memberHtmlBody(final TeamMemberLeftEvent event) {
		return EmailLayoutTemplate.wrap(memberContentHtml(event));
	}

	private static String ownerContentHtml(final TeamMemberLeftEvent event) {
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
				    %s 팀에 대한 접근 권한이 삭제됩니다.
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

	private static String memberContentHtml(final TeamMemberLeftEvent event) {
		String teamName = HtmlEscapeUtils.escape(event.teamName());
		return String.format("""
				<h1 style="margin:0 0 20px 0;font-size:20px;line-height:1.4;font-weight:700;color:#1e2124;">
				  %s 팀 탈퇴가 완료되었습니다
				</h1>
				<p style="margin:0 0 16px 0;font-size:15px;line-height:1.7;color:#33363d;">
				  안녕하세요.<br>
				  <strong style="color:#0b50d0;">%s</strong> 팀 탈퇴가 정상적으로 완료되었습니다.
				</p>
				<div style="margin:20px 0;padding:16px 20px;background-color:#ecf2fe;border-left:3px solid #256ef4;border-radius:6px;">
				  <p style="margin:0;font-size:14px;line-height:1.6;color:#33363d;">
				    %s 팀에 대한 접근 권한이 삭제됩니다.
				  </p>
				</div>
				<p style="margin:24px 0 0 0;font-size:14px;line-height:1.6;color:#6d7882;">
				  감사합니다.
				</p>
				""",
			teamName,
			teamName,
			teamName
		);
	}
}
