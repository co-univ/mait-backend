package com.coniv.mait.domain.solve.service.dto;

import com.coniv.mait.domain.user.service.dto.UserDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RankDto implements Comparable<RankDto> {

	private UserDto user;
	@Schema(description = "등수", requiredMode = Schema.RequiredMode.REQUIRED)
	private int rank;
	@Schema(description = "맞춘 문제의 개수", requiredMode = Schema.RequiredMode.REQUIRED)
	private long count;

	@Override
	public int compareTo(RankDto o) {
		if (this.count == o.count) {
			return this.getUser().getName().compareTo(o.getUser().getName());
		}
		return (int)(o.count - this.count);
	}
}
