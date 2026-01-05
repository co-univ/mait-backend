package com.coniv.mait.migration.question;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.migration.MigrationJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile({"dev", "local", "prod"})
@Component
@RequiredArgsConstructor
public class ShortQuestionAnswerCountMigrationJob implements MigrationJob {

	private final QuestionEntityRepository questionEntityRepository;

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Transactional
	@Override
	public void migrate() {
		List<QuestionEntity> shortQuestions = questionEntityRepository.findAllByQuestionType(QuestionType.SHORT);

		for (QuestionEntity question : shortQuestions) {
			ShortQuestionEntity shortQuestionEntity = (ShortQuestionEntity)question;
			int mainAnswerCount = shortAnswerEntityRepository
				.countByShortQuestionIdAndIsMainTrue(shortQuestionEntity.getId());
			shortQuestionEntity.updateAnswerCount(mainAnswerCount);
			log.info("[{}] id={}의 answerCount {} 업데이트", getName(), shortQuestionEntity.getId(), mainAnswerCount);
		}

		log.info("[{}]. 총 {}개의 주관식 문제가 처리 완료.", getName(), shortQuestions.size());
	}
}
