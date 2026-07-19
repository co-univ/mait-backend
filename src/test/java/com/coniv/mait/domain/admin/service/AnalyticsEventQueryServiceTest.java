package com.coniv.mait.domain.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;
import com.coniv.mait.domain.admin.repository.AnalyticsEventRepository;
import com.coniv.mait.domain.admin.repository.AnalyticsFeatureRepository;
import com.coniv.mait.domain.admin.service.dto.AnalyticsEventStatsDto;
import com.coniv.mait.domain.admin.service.dto.AnalyticsEventStepCountDto;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventQueryServiceTest {

	private static final Long FEATURE_ID = 1L;
	private static final String FEATURE_KEY = "onboarding";

	@InjectMocks
	private AnalyticsEventQueryService analyticsEventQueryService;

	@Mock
	private AnalyticsFeatureRepository analyticsFeatureRepository;

	@Mock
	private AnalyticsEventRepository analyticsEventRepository;

	@Test
	@DisplayName("등록된 feature 마스터 전체를 조회한다")
	void getFeatures_returnsAllFeatures() {
		// given
		AnalyticsFeatureEntity onboarding = AnalyticsFeatureEntity.of("onboarding");
		AnalyticsFeatureEntity solving = AnalyticsFeatureEntity.of("solving");
		given(analyticsFeatureRepository.findAll()).willReturn(List.of(onboarding, solving));

		// when
		List<AnalyticsFeatureEntity> result = analyticsEventQueryService.getFeatures();

		// then
		assertThat(result).containsExactly(onboarding, solving);
	}

	@Test
	@DisplayName("이벤트를 (event_name, step) 단위로 집계하며 event_name·step 오름차순으로 정렬한다")
	void getEventStats_aggregatesByEventNameAndStep() {
		// given
		AnalyticsFeatureEntity feature = AnalyticsFeatureEntity.of(FEATURE_KEY);
		given(analyticsFeatureRepository.findById(FEATURE_ID)).willReturn(Optional.of(feature));
		given(analyticsEventRepository.findAllByFeature(feature)).willReturn(List.of(
			event(feature, "player_set_list_view", 0),
			event(feature, "player_set_list_view", 0),
			event(feature, "player_set_list_exit", 3),
			event(feature, "player_set_list_exit", 3),
			event(feature, "player_set_list_exit", 1)));

		// when
		AnalyticsEventStatsDto result = analyticsEventQueryService.getEventStats(FEATURE_ID);

		// then
		assertThat(result.featureKey()).isEqualTo(FEATURE_KEY);
		assertThat(result.stepCounts()).containsExactly(
			new AnalyticsEventStepCountDto("player_set_list_exit", 1, 1),
			new AnalyticsEventStepCountDto("player_set_list_exit", 3, 2),
			new AnalyticsEventStepCountDto("player_set_list_view", 0, 2));
	}

	@Test
	@DisplayName("존재하지 않는 featureId 면 EntityNotFoundException 을 던진다")
	void getEventStats_throwsWhenFeatureNotFound() {
		// given
		given(analyticsFeatureRepository.findById(FEATURE_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> analyticsEventQueryService.getEventStats(FEATURE_ID))
			.isInstanceOf(EntityNotFoundException.class);
		then(analyticsEventRepository).should(never()).findAllByFeature(any());
	}

	private AnalyticsEventEntity event(final AnalyticsFeatureEntity feature, final String eventName, final int step) {
		return AnalyticsEventEntity.builder()
			.feature(feature)
			.eventName(eventName)
			.step(step)
			.build();
	}
}
