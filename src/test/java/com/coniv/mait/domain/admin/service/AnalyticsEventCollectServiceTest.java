package com.coniv.mait.domain.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.repository.AnalyticsEventRepository;

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

	@Test
	@DisplayName("동일 조합이 존재하지 않으면 이벤트를 저장한다")
	void collect_savesWhenNotDuplicate() {
		// given
		given(analyticsEventRepository.existsByFeatureKeyAndEventNameAndUserIdAndSessionId(
			FEATURE_KEY, EVENT_NAME, USER_ID, SESSION_ID)).willReturn(false);

		// when
		analyticsEventCollectService.collect(USER_ID, FEATURE_KEY, EVENT_NAME, SESSION_ID, 2, "context");

		// then
		ArgumentCaptor<AnalyticsEventEntity> captor = ArgumentCaptor.forClass(AnalyticsEventEntity.class);
		then(analyticsEventRepository).should().save(captor.capture());
		AnalyticsEventEntity saved = captor.getValue();
		assertThat(saved.getFeatureKey()).isEqualTo(FEATURE_KEY);
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
		given(analyticsEventRepository.existsByFeatureKeyAndEventNameAndUserIdAndSessionId(
			FEATURE_KEY, EVENT_NAME, USER_ID, SESSION_ID)).willReturn(true);

		// when
		analyticsEventCollectService.collect(USER_ID, FEATURE_KEY, EVENT_NAME, SESSION_ID, 2, null);

		// then
		then(analyticsEventRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("step 이 null 이면 0 으로 저장한다")
	void collect_defaultsStepToZeroWhenNull() {
		// given
		given(analyticsEventRepository.existsByFeatureKeyAndEventNameAndUserIdAndSessionId(
			FEATURE_KEY, EVENT_NAME, USER_ID, SESSION_ID)).willReturn(false);

		// when
		analyticsEventCollectService.collect(USER_ID, FEATURE_KEY, EVENT_NAME, SESSION_ID, null, null);

		// then
		ArgumentCaptor<AnalyticsEventEntity> captor = ArgumentCaptor.forClass(AnalyticsEventEntity.class);
		then(analyticsEventRepository).should().save(captor.capture());
		assertThat(captor.getValue().getStep()).isZero();
	}
}
