package com.coniv.mait.migration.question;

import java.util.Comparator;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.util.LexoRank;
import com.coniv.mait.migration.MigrationJob;

import lombok.RequiredArgsConstructor;

@Profile({"dev", "local", "prod"})
@Component
@RequiredArgsConstructor
public class QuestionRankInitializationMigration implements MigrationJob {

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	@Override
	@Transactional
	public void migrate() {
		List<Long> questionSetIds = questionSetEntityRepository.findAll().stream()
			.map(QuestionSetEntity::getId)
			.toList();

		for (Long questionSetId : questionSetIds) {
			List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetId(questionSetId).stream()
				.sorted(Comparator.comparingLong(QuestionEntity::getNumber))
				.toList();
			boolean needsInit = questions.stream().anyMatch(q -> q.getLexoRank() == null || q.getLexoRank().isBlank());
			if (!needsInit) {
				continue;
			}
			String prev = null;
			for (QuestionEntity q : questions) {
				if (q.getLexoRank() == null || q.getLexoRank().isBlank()) {
					String rank = prev == null ? LexoRank.middle() : LexoRank.nextAfter(prev);
					q.updateRank(rank);
					prev = rank;
				} else {
					prev = q.getLexoRank();
				}
			}
		}
	}
}


