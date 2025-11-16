package com.coniv.mait.web.solve.dto;

import jakarta.validation.constraints.NotNull;

public record OptionOrderPatch(
	@NotNull Long optionId,
	int answerOrder
) {
}

