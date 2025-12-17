package com.coniv.mait.domain.solve.service.component;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionSetParticipantManager {

	private final QuestionSetParticipantRepository questionSetParticipantRepository;

	public boolean isParticipating(final UserEntity user, final QuestionSetEntity questionSet) {
		return questionSetParticipantRepository.existsByQuestionSetIdAndUserIdAndStatus(questionSet.getId(),
			user.getId(), ParticipantStatus.ACTIVE);
	}
}
