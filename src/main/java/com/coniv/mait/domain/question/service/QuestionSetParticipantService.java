package com.coniv.mait.domain.question.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetParticipantService {

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final QuestionSetParticipantRepository questionSetParticipantRepository;

	public List<ParticipantDto> getParticipants(final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("해당 문제 셋을 조회할 수 없음 id: " + questionSetId));

		return questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet).stream()
			.map(ParticipantDto::from)
			.toList();

	}
}
