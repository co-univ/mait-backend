package com.coniv.mait.domain.statistic.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.service.component.AnswerSubmitRecordReader;
import com.coniv.mait.domain.statistic.service.dto.QuestionStatisticDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionStatisticService {

	private final TeamRoleValidator teamRoleValidator;
	private final QuestionSetReader questionSetReader;
	private final QuestionReader questionReader;
	private final AnswerSubmitRecordReader answerSubmitRecordReader;

	@Transactional(readOnly = true)
	public List<QuestionStatisticDto> getWrongRates(final Long questionSetId, final MaitUser maitUser) {
		QuestionSetEntity questionSet = questionSetReader.getQuestionSet(questionSetId);

		teamRoleValidator.checkIsTeamMember(questionSet.getTeamId(), maitUser.id());

		List<QuestionEntity> questions = questionReader.getQuestionsByQuestionSet(questionSet);
		if (questions.isEmpty()) {
			return List.of();
		}

		Map<Long, List<AnswerSubmitRecordEntity>> firstSubmitsByQuestionId =
			answerSubmitRecordReader.getFirstSubmitsByQuestionId(questions);

		return questions.stream()
			.map(question -> QuestionStatisticDto.of(question, firstSubmitsByQuestionId.get(question.getId())))
			.sorted(Comparator.comparing(QuestionStatisticDto::getWrongRate,
					Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(QuestionStatisticDto::getQuestionNumber,
					Comparator.nullsLast(Comparator.naturalOrder())))
			.toList();
	}
}
