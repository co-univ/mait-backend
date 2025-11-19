package com.coniv.mait.domain.team.service.dto;

import com.coniv.mait.domain.team.enums.JoinedImmediate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamInvitationResultDto {
	private JoinedImmediate joinedImmediate;

	public static TeamInvitationResultDto from(final JoinedImmediate joinedImmediate) {
		return TeamInvitationResultDto.builder()
			.joinedImmediate(joinedImmediate)
			.build();
	}
}
