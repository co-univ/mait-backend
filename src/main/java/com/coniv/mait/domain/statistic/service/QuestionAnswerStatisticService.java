package com.coniv.mait.domain.statistic.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.statistic.service.dto.QuestionAnswerDistributionDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionAnswerStatisticService {

	private final QuestionService questionService;
	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Transactional(readOnly = true)
	public QuestionAnswerDistributionDto getAnswerDistribution(final Long questionSetId, final Long questionId,
		final boolean firstSubmitOnly) {
		// MANAGING 모드는 실제 정답이 노출되므로(answerVisible), 문제 정보·해설·정답을 그대로 활용한다.
		QuestionDto question = questionService.getQuestion(questionSetId, questionId, DeliveryMode.MANAGING);

		List<AnswerSubmitRecordEntity> records = answerSubmitRecordEntityRepository.findAllByQuestionId(questionId);

		return QuestionAnswerDistributionDto.of(questionSetId, question, records, firstSubmitOnly);
	}
}
