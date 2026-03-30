package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.event.NewParticipantEvent;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.event.MaitEventPublisher;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionSetParticipantServiceTest {

	@InjectMocks
	private QuestionSetParticipantService questionSetParticipantService;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionSetParticipantRepository questionSetParticipantRepository;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private MaitEventPublisher maitEventPublisher;

	@Test
	@DisplayName("진출자 선정 전이면 ACTIVE 상태로 참가한다")
	void participateLiveQuestionSet_AdvancementNotSelected_Active() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.deliveryMode(DeliveryMode.LIVE_TIME)
			.ongoingStatus(QuestionSetOngoingStatus.ONGOING)
			.build();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));
		given(questionSetParticipantRepository.existsByQuestionSetAndUserId(questionSet, userId)).willReturn(false);
		given(questionSetParticipantRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		// when
		questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);

		// then
		ArgumentCaptor<QuestionSetParticipantEntity> captor = ArgumentCaptor.forClass(
			QuestionSetParticipantEntity.class);
		then(questionSetParticipantRepository).should().save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ParticipantStatus.ACTIVE);
	}

	@Test
	@DisplayName("진출자 선정 후이면 ELIMINATED 상태로 참가한다")
	void participateLiveQuestionSet_AdvancementSelected_Eliminated() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.deliveryMode(DeliveryMode.LIVE_TIME)
			.ongoingStatus(QuestionSetOngoingStatus.ONGOING)
			.build();
		questionSet.markAdvancementSelected();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));
		given(questionSetParticipantRepository.existsByQuestionSetAndUserId(questionSet, userId)).willReturn(false);
		given(questionSetParticipantRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		// when
		questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);

		// then
		ArgumentCaptor<QuestionSetParticipantEntity> captor = ArgumentCaptor.forClass(
			QuestionSetParticipantEntity.class);
		then(questionSetParticipantRepository).should().save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ParticipantStatus.ELIMINATED);
	}

	@Test
	@DisplayName("LIVE_TIME 모드가 아니면 QuestionSetStatusException을 던진다")
	void participateLiveQuestionSet_NotLiveMode_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.deliveryMode(DeliveryMode.STUDY)
			.ongoingStatus(QuestionSetOngoingStatus.ONGOING)
			.build();

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));

		// when & then
		assertThatThrownBy(
			() -> questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId))
			.isInstanceOf(QuestionSetStatusException.class);
	}

	@Test
	@DisplayName("참가 성공 시 NewParticipantEvent를 발행한다")
	void participateLiveQuestionSet_Success_PublishesNewParticipantEvent() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.deliveryMode(DeliveryMode.LIVE_TIME)
			.ongoingStatus(QuestionSetOngoingStatus.ONGOING)
			.build();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));
		given(questionSetParticipantRepository.existsByQuestionSetAndUserId(questionSet, userId)).willReturn(false);
		given(questionSetParticipantRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		// when
		questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);

		// then
		ArgumentCaptor<NewParticipantEvent> eventCaptor = ArgumentCaptor.forClass(NewParticipantEvent.class);
		then(maitEventPublisher).should().publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().questionSetId()).isEqualTo(questionSetId);
		assertThat(eventCaptor.getValue().participant()).isNotNull();
	}

	@Test
	@DisplayName("이미 참가한 유저면 이벤트를 발행하지 않는다")
	void participateLiveQuestionSet_AlreadyParticipated_DoesNotPublishEvent() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.deliveryMode(DeliveryMode.LIVE_TIME)
			.ongoingStatus(QuestionSetOngoingStatus.ONGOING)
			.build();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));
		given(questionSetParticipantRepository.existsByQuestionSetAndUserId(questionSet, userId)).willReturn(true);

		// when
		questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);

		// then
		then(maitEventPublisher).should(never()).publishEvent(any());
	}

	@Test
	@DisplayName("이미 참가한 유저면 중복 저장하지 않는다")
	void participateLiveQuestionSet_AlreadyParticipated_Skip() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.deliveryMode(DeliveryMode.LIVE_TIME)
			.ongoingStatus(QuestionSetOngoingStatus.ONGOING)
			.build();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));
		given(questionSetParticipantRepository.existsByQuestionSetAndUserId(questionSet, userId)).willReturn(true);

		// when
		questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);

		// then
		then(questionSetParticipantRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("존재하지 않는 문제셋이면 EntityNotFoundException을 던진다")
	void participateLiveQuestionSet_QuestionSetNotFound_ThrowsException() {
		// given
		Long questionSetId = 999L;
		Long userId = 1L;

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(
			() -> questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId))
			.isInstanceOf(EntityNotFoundException.class);
	}

	// === updateParticipantsStatus 테스트 ===

	@Test
	@DisplayName("진출자 선정 시 advancementSelected 플래그가 true로 변경된다")
	void updateParticipantsStatus_MarksAdvancementSelected() {
		// given
		Long questionSetId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.deliveryMode(DeliveryMode.LIVE_TIME)
			.ongoingStatus(QuestionSetOngoingStatus.ONGOING)
			.build();

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet))
			.willReturn(List.of());

		// when
		questionSetParticipantService.updateParticipantsStatus(questionSetId, List.of(), List.of());

		// then
		assertThat(questionSet.isAdvancementSelected()).isTrue();
	}

	@Test
	@DisplayName("active와 eliminated 목록에 중복 유저가 있으면 예외를 던진다")
	void updateParticipantsStatus_DuplicateUser_ThrowsException() {
		// given
		Long questionSetId = 1L;

		ParticipantDto duplicateUser = ParticipantDto.builder().userId(1L).build();
		List<ParticipantDto> activeUsers = List.of(duplicateUser);
		List<ParticipantDto> eliminatedUsers = List.of(duplicateUser);

		// when & then
		assertThatThrownBy(
			() -> questionSetParticipantService.updateParticipantsStatus(
				questionSetId, activeUsers, eliminatedUsers))
			.isInstanceOf(Exception.class);
	}
}
