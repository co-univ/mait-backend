package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.lock.DistributedLock;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetCopyService {

	private final QuestionSetReader questionSetReader;
	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final TeamRoleValidator teamRoleValidator;

	@DistributedLock(key = "'question-set-copy:' + #userId + ':' + #questionSetId + ':' + #targetTeamId")
	@Transactional
	public QuestionSetDto copyQuestionSet(final Long questionSetId, final Long targetTeamId, final Long userId) {
		QuestionSetEntity source = questionSetReader.getQuestionSet(questionSetId);

		teamRoleValidator.checkIsTeamMember(source.getTeamId(), userId);
		teamRoleValidator.checkHasCreateQuestionSetAuthority(targetTeamId, userId);

		QuestionSetEntity copied = questionSetEntityRepository.save(
			QuestionSetEntity.copyOf(source, targetTeamId, userId));

		return QuestionSetDto.from(copied);
	}
}
