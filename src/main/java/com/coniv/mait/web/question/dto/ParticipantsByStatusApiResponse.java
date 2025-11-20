package com.coniv.mait.web.question.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.enums.ParticipantStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ParticipantsByStatusApiResponse(
	@Schema(description = "현재 진행 중인 문제 풀이에 풀이가 가능한 인원", requiredMode = Schema.RequiredMode.REQUIRED)
	List<ParticipantInfoApiResponse> activeParticipants,
	@Schema(description = "현재 풀이가 불가능한 인원 목록 조회", requiredMode = Schema.RequiredMode.REQUIRED)
	List<ParticipantInfoApiResponse> eliminatedParticipants
) {
	public static ParticipantsByStatusApiResponse from(List<ParticipantDto> participants) {
		Map<ParticipantStatus, List<ParticipantInfoApiResponse>> participantsByStatus = participants.stream()
			.map(ParticipantInfoApiResponse::from)
			.collect(Collectors.groupingBy(ParticipantInfoApiResponse::status));

		return ParticipantsByStatusApiResponse.builder()
			.activeParticipants(participantsByStatus.getOrDefault(ParticipantStatus.ACTIVE, List.of()))
			.eliminatedParticipants(participantsByStatus.getOrDefault(ParticipantStatus.ELIMINATED, List.of()))
			.build();
	}
}
