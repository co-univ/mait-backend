package com.coniv.mait.web.team.dto.enums;

import com.coniv.mait.domain.team.enums.InvitationApplicationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InvitationApplicationApiStatus {
	PENDING("대기 중"),

	APPROVED("승인"),

	REJECTED("거절"),

	NOT_APPLIED("신청 안함");

	private String description;

	public static InvitationApplicationApiStatus toApiStatus(InvitationApplicationStatus status) {
		if (status == null) {
			return NOT_APPLIED;
		}
		return InvitationApplicationApiStatus.valueOf(status.name());
	}
}
