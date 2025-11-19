package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.dto.AnswerRankDto;
import com.coniv.mait.domain.user.service.dto.UserDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CorrectorRanksApiResponse(
	@Schema(description = "문제 셋 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema
	List<AnswerRankApiResponse> ranksGroup
) {

	@Builder
	record AnswerRankApiResponse(
		@Schema(description = "맞춘 정답 개수", requiredMode = Schema.RequiredMode.REQUIRED)
		long answerCount,
		@Schema(description = "유저 정보")
		List<UserApiResponse> users
	) {
		static AnswerRankApiResponse from(AnswerRankDto answerRanks) {
			return AnswerRankApiResponse.builder()
				.answerCount(answerRanks.getCount())
				.users(answerRanks.getUsers().stream().map(UserApiResponse::from).toList())
				.build();
		}
	}

	@Builder
	record UserApiResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
		Long userId,
		@Schema(description = "유저 이름")
		String name,
		@Schema(description = "일반 닉네임")
		String nickName,
		@Schema(description = "코드를 포함한 닉네임", example = "신유승#0319")
		String fullNickname
	) {
		static UserApiResponse from(UserDto userDto) {
			return UserApiResponse.builder()
				.userId(userDto.getId())
				.name(userDto.getName())
				.nickName(userDto.getNickname())
				.fullNickname(userDto.getNickname())
				.build();
		}
	}

	public static CorrectorRanksApiResponse of(final Long questionSetId, final List<AnswerRankDto> answerRanks) {
		return CorrectorRanksApiResponse.builder()
			.questionSetId(questionSetId)
			.ranksGroup(answerRanks.stream().map(AnswerRankApiResponse::from).toList())
			.build();
	}
}
