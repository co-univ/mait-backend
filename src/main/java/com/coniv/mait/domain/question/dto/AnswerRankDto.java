package com.coniv.mait.domain.question.dto;

import java.util.List;

import com.coniv.mait.domain.user.service.dto.UserDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnswerRankDto {
	private long count;

	private List<UserDto> users;
}
