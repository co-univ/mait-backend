package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;

@ExtendWith(MockitoExtension.class)
class QuestionSetReaderTest {

	@InjectMocks
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Test
	@DisplayName("LIVE_TIME 모드이면서 AFTER 상태인 문제셋 목록을 반환한다")
	void getFinishedLiveQuestionSetsInTeam_Success() {
		// given
		Long teamId = 1L;
		QuestionSetEntity qs1 = mock(QuestionSetEntity.class);
		QuestionSetEntity qs2 = mock(QuestionSetEntity.class);

		when(questionSetEntityRepository.findAllByTeamIdAndDeliveryModeAndOngoingStatus(
			teamId, DeliveryMode.LIVE_TIME, QuestionSetOngoingStatus.AFTER))
			.thenReturn(List.of(qs1, qs2));

		// when
		List<QuestionSetEntity> result = questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId);

		// then
		assertThat(result).hasSize(2);
		verify(questionSetEntityRepository).findAllByTeamIdAndDeliveryModeAndOngoingStatus(
			teamId, DeliveryMode.LIVE_TIME, QuestionSetOngoingStatus.AFTER);
	}

	@Test
	@DisplayName("조건에 맞는 문제셋이 없으면 빈 리스트를 반환한다")
	void getFinishedLiveQuestionSetsInTeam_Empty() {
		// given
		Long teamId = 1L;

		when(questionSetEntityRepository.findAllByTeamIdAndDeliveryModeAndOngoingStatus(
			teamId, DeliveryMode.LIVE_TIME, QuestionSetOngoingStatus.AFTER))
			.thenReturn(List.of());

		// when
		List<QuestionSetEntity> result = questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId);

		// then
		assertThat(result).isEmpty();
	}
}
