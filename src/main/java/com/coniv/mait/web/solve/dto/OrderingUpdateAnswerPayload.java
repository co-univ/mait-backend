package com.coniv.mait.web.solve.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record OrderingUpdateAnswerPayload(
	@NotEmpty List<@Valid OptionOrderPatch> optionOrders
) implements UpdateAnswerPayload {

}

