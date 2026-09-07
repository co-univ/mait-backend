package com.coniv.mait.migration.question;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.persistence.EntityManager;

@Import(MultipleQuestionAnswerCountMigrationIntegrationTest.MigrationTestConfig.class)
class MultipleQuestionAnswerCountMigrationIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MultipleQuestionAnswerCountMigrationJob migration;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("객관식 정답 개수를 보정하고 재실행해도 다른 유형과 선택지는 유지한다")
	void migrate_CorrectsCountsAndCanRunAgain() {
		// given
		MultipleQuestionEntity incorrect = saveQuestion(7, "a");
		MultipleQuestionEntity allIncorrect = saveQuestion(4, "b");
		MultipleQuestionEntity empty = saveQuestion(4, "c");
		MultipleQuestionEntity consistent = saveQuestion(1, "d");
		ShortQuestionEntity shortQuestion = questionEntityRepository.save(ShortQuestionEntity.builder()
			.lexoRank("e").answerCount(3).build());
		multipleChoiceEntityRepository.saveAll(List.of(
			choice(incorrect, 1, true), choice(incorrect, 2, false), choice(incorrect, 3, true),
			choice(allIncorrect, 1, false), choice(consistent, 1, true)
		));
		entityManager.flush();
		entityManager.clear();

		// when
		migration.migrate();
		migration.migrate();

		// then
		assertThat(getAnswerCount(incorrect.getId())).isEqualTo(2);
		assertThat(getAnswerCount(allIncorrect.getId())).isZero();
		assertThat(getAnswerCount(empty.getId())).isZero();
		assertThat(getAnswerCount(consistent.getId())).isEqualTo(1);
		ShortQuestionEntity reloadedShort = (ShortQuestionEntity)questionEntityRepository
			.findById(shortQuestion.getId()).orElseThrow();
		assertThat(reloadedShort.getAnswerCount()).isEqualTo(3);
		assertThat(multipleChoiceEntityRepository.findAllByQuestionId(incorrect.getId()))
			.extracting(MultipleChoiceEntity::getNumber, MultipleChoiceEntity::isCorrect)
			.containsExactlyInAnyOrder(tuple(1, true), tuple(2, false), tuple(3, true));
	}

	private MultipleQuestionEntity saveQuestion(int answerCount, String rank) {
		return questionEntityRepository.save(MultipleQuestionEntity.builder()
			.lexoRank(rank).answerCount(answerCount).build());
	}

	private MultipleChoiceEntity choice(MultipleQuestionEntity question, int number, boolean correct) {
		return MultipleChoiceEntity.builder().question(question).number(number).isCorrect(correct).build();
	}

	private int getAnswerCount(Long questionId) {
		return ((MultipleQuestionEntity)questionEntityRepository.findById(questionId).orElseThrow()).getAnswerCount();
	}

	@TestConfiguration
	static class MigrationTestConfig {

		@Bean
		MultipleQuestionAnswerCountMigrationJob multipleQuestionAnswerCountMigrationJob(JdbcTemplate jdbcTemplate) {
			return new MultipleQuestionAnswerCountMigrationJob(jdbcTemplate);
		}
	}
}
