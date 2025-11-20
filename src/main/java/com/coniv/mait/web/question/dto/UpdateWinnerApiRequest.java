package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.dto.ParticipantDto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateWinnerApiRequest(
	@NotNull(message = "우승자 정보를 입력해주세요")
	List<ParticipantDto> winners
) {
}
