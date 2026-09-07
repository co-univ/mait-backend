package com.coniv.mait.migration.question;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.migration.MigrationJob;

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

	private static final String ACTUAL_ANSWER_COUNT = """
		(SELECT COUNT(*) FROM multiple_choices c
		WHERE c.question_id = questions.id AND c.is_correct = true)
		""";

	private static final String MISMATCH_CONDITION = "question_type = 'multiple'"
		+ " AND (answer_count IS NULL OR answer_count <> " + ACTUAL_ANSWER_COUNT + ")";

	private final JdbcTemplate jdbcTemplate;

	@Override
	@Transactional
	public void migrate() {
		long mismatchCount = countMismatches();
		log.info("[{}] 보정 전 정답 개수 불일치: {}건", getName(), mismatchCount);
		if (mismatchCount == 0) {
			return;
		}

		int updatedCount = jdbcTemplate.update("UPDATE questions SET answer_count = " + ACTUAL_ANSWER_COUNT
			+ " WHERE " + MISMATCH_CONDITION);

		long remainingCount = countMismatches();
		if (remainingCount != 0) {
			throw new IllegalStateException("객관식 정답 개수 보정 후 불일치가 남아 있습니다: " + remainingCount);
		}
		log.info("[{}] 정답 개수 보정 완료: {}건, 남은 불일치: {}건", getName(), updatedCount, remainingCount);
	}

	private long countMismatches() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM questions WHERE " + MISMATCH_CONDITION, Long.class);
	}
}
