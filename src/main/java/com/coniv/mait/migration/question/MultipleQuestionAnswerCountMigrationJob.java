package com.coniv.mait.migration.question;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.migration.MigrationJob;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 문제 쓰기 요청을 중지한 상태에서 한 인스턴스로 실행한다.
 */
@Slf4j
@Profile({"dev", "local", "prod"})
@Component
@RequiredArgsConstructor
public class MultipleQuestionAnswerCountMigrationJob implements MigrationJob {

	private final QuestionEntityRepository questionEntityRepository;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	private final EntityManager entityManager;

	@Override
	@Transactional
	public void migrate() {
		List<MultipleQuestionEntity> questions = questionEntityRepository.findAllByQuestionType(QuestionType.MULTIPLE)
			.stream()
			.map(MultipleQuestionEntity.class::cast)
			.toList();

		int updatedCount = 0;
		for (MultipleQuestionEntity question : questions) {
			int actualAnswerCount = multipleChoiceEntityRepository.countByQuestionIdAndIsCorrectTrue(question.getId());
			if (question.getAnswerCount() != actualAnswerCount) {
				question.updateAnswerCount(actualAnswerCount);
				updatedCount++;
			}
		}
		log.info("[{}] 정답 개수 불일치: {}건", getName(), updatedCount);
		if (updatedCount == 0) {
			return;
		}

		questionEntityRepository.flush();
		long remainingCount = countMismatches(questions);
		if (remainingCount != 0) {
			throw new IllegalStateException("객관식 정답 개수 보정 후 불일치가 남아 있습니다: " + remainingCount);
		}
		log.info("[{}] 정답 개수 보정 완료: {}건, 남은 불일치: {}건", getName(), updatedCount, remainingCount);
	}

	private long countMismatches(List<MultipleQuestionEntity> questions) {
		long mismatchCount = 0;
		for (MultipleQuestionEntity question : questions) {
			entityManager.refresh(question);
			int actualAnswerCount = multipleChoiceEntityRepository.countByQuestionIdAndIsCorrectTrue(question.getId());
			if (question.getAnswerCount() != actualAnswerCount) {
				mismatchCount++;
			}
		}
		return mismatchCount;
	}
}
