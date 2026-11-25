package com.coniv.mait.domain.question.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.util.LexoRank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionRankRebalanceJob {

	private static final int PAGE_SIZE = 100;

	private final QuestionSetEntityRepository questionSets;
	private final QuestionEntityRepository questions;
	private final QuestionRankRebalanceService service;

	@Scheduled(cron = "${mait.question.rank-rebalance-cron:0 0 4 * * *}", zone = "Asia/Seoul")
	public void run() {
		long afterId = 0;
		List<QuestionSetEntity> page;
		do {
			page = questionSets.findAllByIdGreaterThanAndStatusInOrderByIdAsc(afterId,
				QuestionRankRebalanceService.EDITABLE_STATUSES, PageRequest.ofSize(PAGE_SIZE));
			for (QuestionSetEntity questionSet : page) {
				Long questionSetId = questionSet.getId();
				try {
					List<String> ranks = questions.findAllByQuestionSetId(questionSetId).stream()
						.map(QuestionEntity::getLexoRank).toList();
					if (LexoRank.needsRebalance(ranks)) {
						service.rebalance(questionSetId);
					}
				} catch (RuntimeException ex) {
					log.error("문제 순서 키 재배치 실패: questionSetId={}", questionSetId, ex);
				}
				afterId = questionSetId;
			}
		} while (page.size() == PAGE_SIZE);
	}
}
