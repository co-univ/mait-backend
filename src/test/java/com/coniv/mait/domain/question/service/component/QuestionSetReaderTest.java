package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;

@ExtendWith(MockitoExtension.class)
class QuestionSetReaderTest {

	@InjectMocks
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private QuestionSetCategoryLinkEntityRepository questionSetCategoryLinkEntityRepository;

	@Test
	@DisplayName("지정한 풀이 모드이면서 AFTER 또는 REVIEW 상태인 문제셋 목록을 반환한다")
	void getFinishedLiveQuestionSetsBySolveModeInTeam_Success() {
		// given
		Long teamId = 1L;
		QuestionSetSolveMode solveMode = QuestionSetSolveMode.LIVE_TIME;
		QuestionSetEntity qs1 = mock(QuestionSetEntity.class);
		QuestionSetEntity qs2 = mock(QuestionSetEntity.class);

		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, solveMode, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW)))
			.thenReturn(List.of(qs1, qs2));

		// when
		List<QuestionSetEntity> result = questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(teamId,
			solveMode);

		// then
		assertThat(result).hasSize(2);
		verify(questionSetEntityRepository).findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, solveMode, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW));
	}

	@Test
	@DisplayName("조건에 맞는 문제셋이 없으면 빈 리스트를 반환한다")
	void getFinishedLiveQuestionSetsBySolveModeInTeam_Empty() {
		// given
		Long teamId = 1L;
		QuestionSetSolveMode solveMode = QuestionSetSolveMode.LIVE_TIME;

		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, solveMode, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW)))
			.thenReturn(List.of());

		// when
		List<QuestionSetEntity> result = questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(teamId,
			solveMode);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("getFinishedQuestionSetsByCategoryId - 카테고리별로 연결된 종료 문제 셋만 묶어 반환한다")
	void getFinishedQuestionSetsByCategoryId_Success() {
		// given
		QuestionSetCategoryEntity category1 = mockCategory(1L);
		QuestionSetCategoryEntity category2 = mockCategory(2L);

		// category1 → 문제 셋 10(종료), 11(미종료) / category2 → 문제 셋 20(종료)
		when(questionSetCategoryLinkEntityRepository.findAllByCategoryIdIn(List.of(1L, 2L))).thenReturn(List.of(
			QuestionSetCategoryLinkEntity.of(10L, 1L),
			QuestionSetCategoryLinkEntity.of(11L, 1L),
			QuestionSetCategoryLinkEntity.of(20L, 2L)));

		QuestionSetEntity qs10 = mockQuestionSet(10L);
		QuestionSetEntity qs20 = mockQuestionSet(20L);
		when(questionSetEntityRepository.findAllByIdInAndStatusIn(anyList(),
			eq(List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW))))
			.thenReturn(List.of(qs10, qs20));

		// when
		Map<Long, List<QuestionSetEntity>> result =
			questionSetReader.getFinishedQuestionSetsByCategoryId(List.of(category1, category2));

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(1L)).containsExactly(qs10);
		assertThat(result.get(2L)).containsExactly(qs20);
	}

	@Test
	@DisplayName("getFinishedQuestionSetsByCategoryId - 카테고리가 없으면 빈 맵을 반환하고 조회하지 않는다")
	void getFinishedQuestionSetsByCategoryId_emptyCategories() {
		// when
		Map<Long, List<QuestionSetEntity>> result = questionSetReader.getFinishedQuestionSetsByCategoryId(List.of());

		// then
		assertThat(result).isEmpty();
		verify(questionSetCategoryLinkEntityRepository, never()).findAllByCategoryIdIn(any());
	}

	@Test
	@DisplayName("getQuestionsByQuestionSetId - 문제를 문제 셋 ID별로 묶어 반환한다")
	void getQuestionsByQuestionSetId_Success() {
		// given
		QuestionEntity q1 = mockQuestion(10L);
		QuestionEntity q2 = mockQuestion(10L);
		QuestionEntity q3 = mockQuestion(20L);
		when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(10L, 20L))).thenReturn(List.of(q1, q2, q3));

		// when
		Map<Long, List<QuestionEntity>> result = questionSetReader.getQuestionsByQuestionSetId(List.of(10L, 20L));

		// then
		assertThat(result.get(10L)).containsExactly(q1, q2);
		assertThat(result.get(20L)).containsExactly(q3);
	}

	@Test
	@DisplayName("getQuestionsByQuestionSetId - 문제 셋 ID가 없으면 빈 맵을 반환한다")
	void getQuestionsByQuestionSetId_empty() {
		// when
		Map<Long, List<QuestionEntity>> result = questionSetReader.getQuestionsByQuestionSetId(List.of());

		// then
		assertThat(result).isEmpty();
		verify(questionEntityRepository, never()).findAllByQuestionSetIdIn(anyList());
	}

	private QuestionSetCategoryEntity mockCategory(final Long id) {
		QuestionSetCategoryEntity category = mock(QuestionSetCategoryEntity.class);
		when(category.getId()).thenReturn(id);
		return category;
	}

	private QuestionSetEntity mockQuestionSet(final Long id) {
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		lenient().when(questionSet.getId()).thenReturn(id);
		return questionSet;
	}

	private QuestionEntity mockQuestion(final Long questionSetId) {
		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);
		when(question.getQuestionSet()).thenReturn(questionSet);
		return question;
	}
}
