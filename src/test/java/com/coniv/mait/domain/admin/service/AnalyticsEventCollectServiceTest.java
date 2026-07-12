package com.coniv.mait.domain.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;
import com.coniv.mait.domain.admin.repository.AnalyticsEventRepository;
import com.coniv.mait.domain.admin.repository.AnalyticsFeatureRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventCollectServiceTest {

	private static final Long USER_ID = 1L;
	private static final String FEATURE_KEY = "onboarding";
	private static final String EVENT_NAME = "player_set_list_view";
	private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

	@InjectMocks
	private AnalyticsEventCollectService analyticsEventCollectService;

	@Mock
	private AnalyticsEventRepository analyticsEventRepository;

	@Mock
	private AnalyticsFeatureRepository analyticsFeatureRepository;

	@Test
	@DisplayName("동일 조합이 존재하지 않으면 이벤트를 저장한다")
	void collect_savesWhenNotDuplicate() {
		// given
		AnalyticsFeatureEntity feature = AnalyticsFeatureEntity.of(FEATURE_KEY);
		given(analyticsFeatureRepository.findByFeatureKey(FEATURE_KEY)).willReturn(Optional.of(feature));
		given(analyticsEventRepository.existsByFeatureAndEventNameAndUserIdAndSessionId(
			feature, EVENT_NAME, USER_ID, SESSION_ID)).willReturn(false);

		// when
		analyticsEventCollectService.collect(USER_ID, FEATURE_KEY, EVENT_NAME, SESSION_ID, 2, "context");

		// then
		ArgumentCaptor<AnalyticsEventEntity> captor = ArgumentCaptor.forClass(AnalyticsEventEntity.class);
		then(analyticsEventRepository).should().save(captor.capture());
		AnalyticsEventEntity saved = captor.getValue();
		assertThat(saved.getFeature()).isEqualTo(feature);
		assertThat(saved.getEventName()).isEqualTo(EVENT_NAME);
		assertThat(saved.getUserId()).isEqualTo(USER_ID);
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getStep()).isEqualTo(2);
		assertThat(saved.getMetadata()).isEqualTo("context");
		assertThat(saved.getOccurredAt()).isNotNull();
	}

	@Test
	@DisplayName("동일 조합이 이미 존재하면 저장하지 않는다")
	void collect_skipsWhenDuplicate() {
		// given
		AnalyticsFeatureEntity feature = AnalyticsFeatureEntity.of(FEATURE_KEY);
		given(analyticsFeatureRepository.findByFeatureKey(FEATURE_KEY)).willReturn(Optional.of(feature));
		given(analyticsEventRepository.existsByFeatureAndEventNameAndUserIdAndSessionId(
			feature, EVENT_NAME, USER_ID, SESSION_ID)).willReturn(true);

		// when
		analyticsEventCollectService.collect(USER_ID, FEATURE_KEY, EVENT_NAME, SESSION_ID, 2, null);

		// then
		then(analyticsEventRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("step 이 null 이면 0 으로 저장한다")
	void collect_defaultsStepToZeroWhenNull() {
		// given
		AnalyticsFeatureEntity feature = AnalyticsFeatureEntity.of(FEATURE_KEY);
		given(analyticsFeatureRepository.findByFeatureKey(FEATURE_KEY)).willReturn(Optional.of(feature));
		given(analyticsEventRepository.existsByFeatureAndEventNameAndUserIdAndSessionId(
			feature, EVENT_NAME, USER_ID, SESSION_ID)).willReturn(false);

		// when
		analyticsEventCollectService.collect(USER_ID, FEATURE_KEY, EVENT_NAME, SESSION_ID, null, null);

		// then
		ArgumentCaptor<AnalyticsEventEntity> captor = ArgumentCaptor.forClass(AnalyticsEventEntity.class);
		then(analyticsEventRepository).should().save(captor.capture());
		assertThat(captor.getValue().getStep()).isZero();
	}

	@Test
	@DisplayName("등록되지 않은 feature_key 면 EntityNotFoundException 을 던진다")
	void collect_throwsWhenFeatureNotRegistered() {
		// given
		given(analyticsFeatureRepository.findByFeatureKey(FEATURE_KEY)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() ->
			analyticsEventCollectService.collect(USER_ID, FEATURE_KEY, EVENT_NAME, SESSION_ID, 2, null))
			.isInstanceOf(EntityNotFoundException.class);
		then(analyticsEventRepository).should(never()).save(any());
	}
}
