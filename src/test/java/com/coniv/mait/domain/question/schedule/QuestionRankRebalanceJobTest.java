package com.coniv.mait.domain.question.schedule;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.QuestionRankRebalanceService;

@ExtendWith(MockitoExtension.class)
class QuestionRankRebalanceJobTest {

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;
	@Mock
	private QuestionEntityRepository questionEntityRepository;
	@Mock
	private QuestionRankRebalanceService questionRankRebalanceService;
	@InjectMocks
	private QuestionRankRebalanceJob questionRankRebalanceJob;

	@Test
	@DisplayName("페이지의 마지막 문제셋 처리에 실패해도 다음 페이지의 문제셋을 재배치한다")
	void run_processesNextPageAfterFailure() {
		List<QuestionSetEntity> firstPage = LongStream.rangeClosed(1, 100)
			.mapToObj(id -> QuestionSetEntity.builder().id(id).build())
			.toList();
		QuestionSetEntity nextQuestionSet = QuestionSetEntity.builder().id(101L).build();
		PageRequest pageRequest = PageRequest.ofSize(100);
		when(questionSetEntityRepository.findAllByIdGreaterThanAndStatusInOrderByIdAsc(
			0L, QuestionRankRebalanceService.EDITABLE_STATUSES, pageRequest)).thenReturn(firstPage);
		when(questionSetEntityRepository.findAllByIdGreaterThanAndStatusInOrderByIdAsc(
			100L, QuestionRankRebalanceService.EDITABLE_STATUSES, pageRequest)).thenReturn(List.of(nextQuestionSet));
		when(questionEntityRepository.findAllByQuestionSetId(100L)).thenThrow(new IllegalStateException());
		when(questionEntityRepository.findAllByQuestionSetId(101L))
			.thenReturn(List.of(QuestionEntity.createDefaultQuestion(nextQuestionSet, "0")));

		questionRankRebalanceJob.run();

		verify(questionRankRebalanceService, only()).rebalance(101L);
	}
}
