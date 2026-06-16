package com.coniv.mait.domain.statistic.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.statistic.service.component.QuestionSetStatisticCalculator;
import com.coniv.mait.domain.statistic.service.dto.CategoryCorrectRateDto;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CategoryStatisticServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long TEAM_ID = 100L;
	private static final Long CATEGORY_ID = 10L;
	private static final MaitUser MAIT_USER = MaitUser.builder().id(USER_ID).build();

	@InjectMocks
	private CategoryStatisticService categoryStatisticService;

	@Mock
	private QuestionSetCategoryEntityRepository questionSetCategoryEntityRepository;

	@Mock
	private QuestionSetCategoryLinkEntityRepository questionSetCategoryLinkEntityRepository;

	@Mock
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionSetStatisticCalculator questionSetStatisticCalculator;

	@Test
	@DisplayName("getCategoryCorrectRates - 카테고리별 내 정답률을 높은 순으로 정렬하고, 미응시 카테고리는 null로 맨 뒤에 둔다")
	void getCategoryCorrectRates_success() {
		// given
		QuestionSetCategoryEntity java = mockCategory(1L, "Java");
		QuestionSetCategoryEntity spring = mockCategory(2L, "Spring");
		QuestionSetCategoryEntity db = mockCategory(3L, "DB");
		when(questionSetCategoryEntityRepository.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(TEAM_ID))
			.thenReturn(List.of(java, spring, db));

		// Java: 종료 셋 2개, Spring: 1개, DB: 0개
		Map<Long, List<QuestionSetEntity>> finishedByCategory = Map.of(
			1L, List.of(mockQuestionSet(101L), mockQuestionSet(102L)),
			2L, List.of(mockQuestionSet(201L)),
			3L, List.of());
		when(questionSetReader.getFinishedQuestionSetsByCategoryId(anyList())).thenReturn(finishedByCategory);
		when(questionSetReader.getQuestionsByQuestionSetId(any())).thenReturn(Map.of());

		Map<Long, Double> myRates = new HashMap<>();
		myRates.put(1L, 80.0);
		myRates.put(2L, 90.0);
		myRates.put(3L, null);
		when(questionSetStatisticCalculator.calculateUserCorrectRates(eq(USER_ID), anyMap())).thenReturn(myRates);
		when(questionSetStatisticCalculator.calculateOverallCorrectRates(anyMap())).thenReturn(Map.of(
			1L, 70.0, 2L, 60.0, 3L, 0.0));

		// when
		List<CategoryCorrectRateDto> result = categoryStatisticService.getCategoryCorrectRates(MAIT_USER, TEAM_ID);

		// then
		assertThat(result).hasSize(3);
		// 내 정답률 내림차순: Spring(90.0) > Java(80.0) > DB(null)
		assertThat(result.get(0).getCategoryId()).isEqualTo(2L);
		assertThat(result.get(0).getCategoryName()).isEqualTo("Spring");
		assertThat(result.get(0).getQuestionSetCount()).isEqualTo(1);
		assertThat(result.get(0).getMyCorrectRate()).isEqualTo(90.0);
		assertThat(result.get(0).getAverageCorrectRate()).isEqualTo(60.0);

		assertThat(result.get(1).getCategoryId()).isEqualTo(1L);
		assertThat(result.get(1).getQuestionSetCount()).isEqualTo(2);
		assertThat(result.get(1).getMyCorrectRate()).isEqualTo(80.0);

		assertThat(result.get(2).getCategoryId()).isEqualTo(3L);
		assertThat(result.get(2).getQuestionSetCount()).isEqualTo(0);
		assertThat(result.get(2).getMyCorrectRate()).isNull();
		assertThat(result.get(2).getAverageCorrectRate()).isEqualTo(0.0);
	}

	@Test
	@DisplayName("getCategoryCorrectRates - 내 정답률이 동률이면 카테고리 이름 오름차순으로 정렬한다")
	void getCategoryCorrectRates_sameRate_sortedByNameAsc() {
		// given
		QuestionSetCategoryEntity banana = mockCategory(1L, "Banana");
		QuestionSetCategoryEntity apple = mockCategory(2L, "Apple");
		when(questionSetCategoryEntityRepository.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(TEAM_ID))
			.thenReturn(List.of(banana, apple));

		Map<Long, List<QuestionSetEntity>> finishedByCategory = Map.of(
			1L, List.of(mockQuestionSet(101L)),
			2L, List.of(mockQuestionSet(201L)));
		when(questionSetReader.getFinishedQuestionSetsByCategoryId(anyList())).thenReturn(finishedByCategory);
		when(questionSetReader.getQuestionsByQuestionSetId(any())).thenReturn(Map.of());
		when(questionSetStatisticCalculator.calculateUserCorrectRates(eq(USER_ID), anyMap()))
			.thenReturn(Map.of(1L, 50.0, 2L, 50.0));
		when(questionSetStatisticCalculator.calculateOverallCorrectRates(anyMap()))
			.thenReturn(Map.of(1L, 50.0, 2L, 50.0));

		// when
		List<CategoryCorrectRateDto> result = categoryStatisticService.getCategoryCorrectRates(MAIT_USER, TEAM_ID);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getCategoryName()).isEqualTo("Apple");
		assertThat(result.get(1).getCategoryName()).isEqualTo("Banana");
	}

	@Test
	@DisplayName("getCategoryCorrectRates - 팀에 카테고리가 없으면 빈 목록을 반환하고 종료 셋을 조회하지 않는다")
	void getCategoryCorrectRates_emptyCategories_returnsEmpty() {
		// given
		when(questionSetCategoryEntityRepository.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(TEAM_ID))
			.thenReturn(List.of());

		// when
		List<CategoryCorrectRateDto> result = categoryStatisticService.getCategoryCorrectRates(MAIT_USER, TEAM_ID);

		// then
		assertThat(result).isEmpty();
		verify(questionSetReader, never()).getFinishedQuestionSetsByCategoryId(any());
	}

	@Test
	@DisplayName("getCategoryCorrectRates - 팀 풀이 권한이 없으면 UserRoleException이 발생한다")
	void getCategoryCorrectRates_noAuthority_throwsUserRoleException() {
		// given
		doThrow(new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다."))
			.when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> categoryStatisticService.getCategoryCorrectRates(MAIT_USER, TEAM_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("해당 문제를 풀 수 있는 권한이 없습니다.");
	}

	@Test
	@DisplayName("getCategoryCorrectRate - 카테고리의 종료 셋 문제를 모아 내 정답률과 평균 정답률을 반환한다")
	void getCategoryCorrectRate_success() {
		// given
		QuestionSetCategoryEntity category = mockCategory(CATEGORY_ID, "Java");
		when(questionSetCategoryEntityRepository.findByIdAndTeamIdAndDeletedAtIsNull(CATEGORY_ID, TEAM_ID))
			.thenReturn(Optional.of(category));
		when(questionSetCategoryLinkEntityRepository.findAllByCategoryId(CATEGORY_ID))
			.thenReturn(List.of(QuestionSetCategoryLinkEntity.of(101L, CATEGORY_ID)));
		QuestionSetEntity finishedSet = mockQuestionSet(101L);
		when(questionSetReader.getFinishedQuestionSets(List.of(101L))).thenReturn(List.of(finishedSet));

		List<QuestionEntity> questions = List.of(mock(QuestionEntity.class));
		when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(101L))).thenReturn(questions);
		when(questionSetStatisticCalculator.calculateUserCorrectRate(USER_ID, questions)).thenReturn(80.0);
		when(questionSetStatisticCalculator.calculateOverallCorrectRate(questions)).thenReturn(70.0);

		// when
		CategoryCorrectRateDto result =
			categoryStatisticService.getCategoryCorrectRate(MAIT_USER, TEAM_ID, CATEGORY_ID);

		// then
		assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
		assertThat(result.getCategoryName()).isEqualTo("Java");
		assertThat(result.getQuestionSetCount()).isEqualTo(1);
		assertThat(result.getMyCorrectRate()).isEqualTo(80.0);
		assertThat(result.getAverageCorrectRate()).isEqualTo(70.0);
	}

	@Test
	@DisplayName("getCategoryCorrectRate - 종료된 문제 셋이 없으면 0건/정답률 null을 반환하고 정답률 계산을 하지 않는다")
	void getCategoryCorrectRate_noFinishedSets_returnsEmptyStat() {
		// given
		QuestionSetCategoryEntity category = mockCategory(CATEGORY_ID, "Java");
		when(questionSetCategoryEntityRepository.findByIdAndTeamIdAndDeletedAtIsNull(CATEGORY_ID, TEAM_ID))
			.thenReturn(Optional.of(category));
		when(questionSetCategoryLinkEntityRepository.findAllByCategoryId(CATEGORY_ID))
			.thenReturn(List.of(QuestionSetCategoryLinkEntity.of(101L, CATEGORY_ID)));
		when(questionSetReader.getFinishedQuestionSets(List.of(101L))).thenReturn(List.of());

		// when
		CategoryCorrectRateDto result =
			categoryStatisticService.getCategoryCorrectRate(MAIT_USER, TEAM_ID, CATEGORY_ID);

		// then
		assertThat(result.getQuestionSetCount()).isZero();
		assertThat(result.getMyCorrectRate()).isNull();
		assertThat(result.getAverageCorrectRate()).isEqualTo(0.0);
		verify(questionSetStatisticCalculator, never()).calculateUserCorrectRate(anyLong(), anyList());
	}

	@Test
	@DisplayName("getCategoryCorrectRate - 팀에 속하지 않은 카테고리이면 EntityNotFoundException이 발생한다")
	void getCategoryCorrectRate_categoryNotFound_throwsEntityNotFoundException() {
		// given
		when(questionSetCategoryEntityRepository.findByIdAndTeamIdAndDeletedAtIsNull(CATEGORY_ID, TEAM_ID))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> categoryStatisticService.getCategoryCorrectRate(MAIT_USER, TEAM_ID, CATEGORY_ID))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 카테고리를 찾을 수 없습니다.");
	}

	private QuestionSetCategoryEntity mockCategory(final Long id, final String name) {
		QuestionSetCategoryEntity category = mock(QuestionSetCategoryEntity.class);
		lenient().when(category.getId()).thenReturn(id);
		lenient().when(category.getName()).thenReturn(name);
		return category;
	}

	private QuestionSetEntity mockQuestionSet(final Long id) {
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		lenient().when(questionSet.getId()).thenReturn(id);
		return questionSet;
	}
}
