package com.coniv.mait.domain.team.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.team.entity.TeamInvitationApplicantEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamApplicantDto {
	private Long applicantId;

	private Long userId;

	private String name;

	private String nickname;

	private TeamUserRole role;

	private LocalDateTime appliedAt;

	public static TeamApplicantDto of(final TeamInvitationApplicantEntity applicant, final UserEntity user) {
		return TeamApplicantDto.builder()
			.applicantId(applicant.getId())
			.userId(user.getId())
			.name(user.getName())
			.nickname(user.getNickname())
			.role(applicant.getRole())
			.appliedAt(applicant.getAppliedAt())
			.build();
	}
}
