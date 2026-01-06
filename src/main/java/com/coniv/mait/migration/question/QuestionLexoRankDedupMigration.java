package com.coniv.mait.migration.question;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
import lombok.extern.slf4j.Slf4j;

@Profile({"dev", "local", "prod"})
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionLexoRankDedupMigration implements MigrationJob {

	private final QuestionEntityRepository questionEntityRepository;
	private final QuestionSetEntityRepository questionSetEntityRepository;

	@Override
	@Transactional
	public void migrate() {
		List<Long> questionSetIds = questionSetEntityRepository.findAll().stream()
			.map(QuestionSetEntity::getId)
			.toList();

		int totalUpdated = 0;

		for (Long questionSetId : questionSetIds) {
			List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetId(questionSetId);
			if (questions.isEmpty()) {
				continue;
			}

			questions.sort(Comparator
				.comparing(QuestionEntity::getLexoRank, Comparator.nullsLast(String::compareTo))
				.thenComparing(QuestionEntity::getId));

			if (!needsFix(questions)) {
				continue;
			}

			int updatedForSet = reassignRanksInPlace(questions);
			totalUpdated += updatedForSet;

			log.info("[Migration][QuestionLexoRankDedupMigration] questionSetId={} updated={}", questionSetId,
				updatedForSet);
		}

		log.info("[Migration][QuestionLexoRankDedupMigration] totalUpdated={}", totalUpdated);
	}

	private boolean needsFix(List<QuestionEntity> sortedQuestions) {
		String prevRank = null;
		for (QuestionEntity q : sortedQuestions) {
			String rank = q.getLexoRank();
			if (rank == null || rank.isBlank()) {
				return true;
			}
			if (Objects.equals(rank, prevRank)) {
				return true;
			}
			prevRank = rank;
		}
		return false;
	}

	private int reassignRanksInPlace(List<QuestionEntity> sortedQuestions) {
		int updated = 0;
		String lastAssigned = null;

		int index = 0;
		while (index < sortedQuestions.size()) {
			QuestionEntity base = sortedQuestions.get(index);
			String rank = base.getLexoRank();

			if (rank == null || rank.isBlank()) {
				String newRank = (lastAssigned == null) ? LexoRank.middle() : LexoRank.nextAfter(lastAssigned);
				base.updateRank(newRank);
				lastAssigned = newRank;
				updated++;
				index++;
				continue;
			}

			int groupEnd = index + 1;
			while (groupEnd < sortedQuestions.size()
				&& Objects.equals(rank, sortedQuestions.get(groupEnd).getLexoRank())) {
				groupEnd++;
			}

			if (groupEnd - index == 1) {
				lastAssigned = rank;
				index = groupEnd;
				continue;
			}

			// 다음 distinct rank (없거나 blank/null이면 upperBound=null)
			String upperBound = null;
			if (groupEnd < sortedQuestions.size()) {
				String next = sortedQuestions.get(groupEnd).getLexoRank();
				if (next != null && !next.isBlank()) {
					upperBound = next;
				}
			}

			lastAssigned = rank;

			// 나머지는 between(...)으로 순서 보존하며 유니크하게 재부여
			for (int dupIndex = index + 1; dupIndex < groupEnd; dupIndex++) {
				QuestionEntity dup = sortedQuestions.get(dupIndex);
				String newRank = LexoRank.between(lastAssigned, upperBound);
				if (Objects.equals(newRank, lastAssigned) || Objects.equals(newRank, upperBound)) {
					throw new IllegalStateException(
						"Failed to generate unique LexoRank between bounds. last=" + lastAssigned + ", upper="
							+ upperBound);
				}
				dup.updateRank(newRank);
				lastAssigned = newRank;
				updated++;
			}

			index = groupEnd;
		}

		return updated;
	}
}


