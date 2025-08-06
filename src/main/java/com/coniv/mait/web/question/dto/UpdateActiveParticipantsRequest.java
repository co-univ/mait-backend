package com.coniv.mait.web.question.dto;

import java.util.List;

public record UpdateActiveParticipantsRequest(
	List<Long> activeUserIds
) {
}
