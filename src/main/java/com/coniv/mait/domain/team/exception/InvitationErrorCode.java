package com.coniv.mait.domain.team.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvitationErrorCode {
	EXPIRED_CODE("초대 코드가 만료되었습니다."),
	ALREADY_MEMBER("이미 팀의 멤버입니다."),
	NOT_FOUND_CODE("초대 토큰을 찾을 수 없습니다."),
	CANNOT_CREATE_WITH_OWNER_ROLE("OWNER 역할로 초대 코드를 생성할 수 없습니다."),
	CANNOT_SET_TO_PENDING("신청 상태를 PENDING으로 변경할 수 없습니다."),
	APPLICATION_NOT_BELONG_TEAM("신청이 해당 팀에 속하지 않습니다."),
	APPLICATION_ALREADY_PROCESSED("해당 신청은 이미 처리되었습니다."),
	ONLY_OWNER_OR_MAKER_APPROVE("팀 소유자나 메이커만 신청을 승인할 수 있습니다."),
	TOKEN_NOT_BELONG_TEAM("초대 토큰이 해당 팀에 속하지 않습니다."),
	USER_ALREADY_APPLIED("해당 초대 코드로 이미 신청한 사용자입니다."),
	CANT_CREATE_INVITE("초대 코드를 생성 권한이 없습니다.");

	private final String description;
}
