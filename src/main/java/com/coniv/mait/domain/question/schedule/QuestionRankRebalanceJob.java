package com.coniv.mait.domain.question.schedule;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.QuestionRankRebalanceService;
import com.coniv.mait.domain.question.util.LexoRank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionRankRebalanceJob {

	private static final int PAGE_SIZE = 100;

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionEntityRepository questionEntityRepository;
	private final QuestionRankRebalanceService questionRankRebalanceService;

	@Scheduled(cron = "${mait.question.rank-rebalance-cron:0 0 4 * * *}", zone = "Asia/Seoul")
	public void run() {
		long afterId = 0;
		PageRequest pageRequest = PageRequest.ofSize(PAGE_SIZE);
		while (true) {
			List<QuestionSetEntity> page = questionSetEntityRepository.findAllByIdGreaterThanAndStatusInOrderByIdAsc(
				afterId, QuestionRankRebalanceService.EDITABLE_STATUSES, pageRequest);
			page.stream()
				.map(QuestionSetEntity::getId)
				.forEach(this::rebalanceIfNeeded);

			if (page.size() < PAGE_SIZE) {
				return;
			}
			afterId = page.getLast().getId();
		}
	}

	private void rebalanceIfNeeded(Long questionSetId) {
		try {
			List<String> ranks = questionEntityRepository.findAllByQuestionSetId(questionSetId).stream()
				.map(QuestionEntity::getLexoRank)
				.toList();
			if (LexoRank.needsRebalance(ranks)) {
				questionRankRebalanceService.rebalance(questionSetId);
			}
		} catch (RuntimeException ex) {
			log.error("문제 순서 키 재배치 실패: questionSetId={}", questionSetId, ex);
		}
	}
}
