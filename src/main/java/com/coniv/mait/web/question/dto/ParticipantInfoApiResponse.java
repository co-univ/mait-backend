package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.enums.ParticipantStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ParticipantInfoApiResponse(
	@Schema(description = "진출 유저 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long userId,
	@Schema(description = "참여자 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long participantId,
	@Schema(description = "참여자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String participantName,

	@Schema(description = "유저 닉네임")
	String userNickname,

	@Schema(description = "해당 유저가 우승자인지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean winner,

	@Schema(description = "진행 상태", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	ParticipantStatus status
) {

	public static ParticipantInfoApiResponse from(ParticipantDto participant) {
		return ParticipantInfoApiResponse.builder()
			.participantId(participant.getParticipantId())
			.userId(participant.getUserId())
			.participantName(participant.getParticipantName())
			.userNickname(participant.getUserNickname())
			.status(participant.getStatus())
			.winner(participant.isWinner())
			.build();
	}
}
