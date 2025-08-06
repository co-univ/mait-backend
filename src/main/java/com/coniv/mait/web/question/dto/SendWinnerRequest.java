package com.coniv.mait.web.question.dto;

import java.util.List;

public record SendWinnerRequest(
	List<Long> winnerUserIds
) {
}
