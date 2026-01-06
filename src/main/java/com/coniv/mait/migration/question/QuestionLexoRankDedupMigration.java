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

	/**
	 * @return 업데이트된 row 수(estimate). JPA dirty-checking으로 flush 시점에 반영된다.
	 */
	private int reassignRanksInPlace(List<QuestionEntity> sortedQuestions) {
		int updated = 0;
		String lastAssigned = null;

		int i = 0;
		while (i < sortedQuestions.size()) {
			QuestionEntity base = sortedQuestions.get(i);
			String rank = base.getLexoRank();

			// null/blank는 뒤쪽에 몰려있다고 가정(nullsLast 정렬), 마지막 rank 이후로 순차 부여
			if (rank == null || rank.isBlank()) {
				String newRank = (lastAssigned == null) ? LexoRank.middle() : LexoRank.nextAfter(lastAssigned);
				base.updateRank(newRank);
				lastAssigned = newRank;
				updated++;
				i++;
				continue;
			}

			// 동일 rank 그룹 찾기
			int j = i + 1;
			while (j < sortedQuestions.size() && Objects.equals(rank, sortedQuestions.get(j).getLexoRank())) {
				j++;
			}

			// 그룹이 1개면 그대로 두고 진행
			if (j - i == 1) {
				lastAssigned = rank;
				i = j;
				continue;
			}

			// 다음 distinct rank (없거나 blank/null이면 upperBound=null)
			String upperBound = null;
			if (j < sortedQuestions.size()) {
				String next = sortedQuestions.get(j).getLexoRank();
				if (next != null && !next.isBlank()) {
					upperBound = next;
				}
			}

			// 첫 번째는 유지 (단, lastAssigned 업데이트)
			lastAssigned = rank;

			// 나머지는 between(...)으로 순서 보존하며 유니크하게 재부여
			for (int k = i + 1; k < j; k++) {
				QuestionEntity dup = sortedQuestions.get(k);
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

			i = j;
		}

		return updated;
	}
}


