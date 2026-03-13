package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionSetReader {

	private final QuestionSetEntityRepository questionSetEntityRepository;

	public List<QuestionSetEntity> getFinishedLiveQuestionSetsInTeam(final Long teamId) {
		return questionSetEntityRepository.findAllByTeamIdAndDeliveryModeAndOngoingStatus(teamId,
			DeliveryMode.LIVE_TIME, QuestionSetOngoingStatus.AFTER);
	}
}
