package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

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
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.event.NewParticipantEvent;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
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
	private QuestionWebSocketSender questionWebSocketSender;

	@Mock
	private MaitEventPublisher maitEventPublisher;

	@Test
	@DisplayName("진출자 선정 전이면 ACTIVE 상태로 참가한다")
	void participateLiveQuestionSet_AdvancementNotSelected_Active() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.ONGOING)
			.build();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(questionSetParticipantRepository.findByQuestionSetAndUserId(questionSet, userId))
			.willReturn(Optional.empty());
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));
		given(questionSetParticipantRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		// when
		ParticipantDto result = questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);

		// then
		ArgumentCaptor<QuestionSetParticipantEntity> captor = ArgumentCaptor.forClass(
			QuestionSetParticipantEntity.class);
		then(questionSetParticipantRepository).should().save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ParticipantStatus.ACTIVE);
		assertThat(result.getStatus()).isEqualTo(ParticipantStatus.ACTIVE);
	}

	@Test
	@DisplayName("진출자 선정 후이면 ELIMINATED 상태로 참가한다")
	void participateLiveQuestionSet_AdvancementSelected_Eliminated() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.ONGOING)
			.build();
		questionSet.markAdvancementSelected();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(questionSetParticipantRepository.findByQuestionSetAndUserId(questionSet, userId))
			.willReturn(Optional.empty());
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));
		given(questionSetParticipantRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		// when
		ParticipantDto result = questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);

		// then
		ArgumentCaptor<QuestionSetParticipantEntity> captor = ArgumentCaptor.forClass(
			QuestionSetParticipantEntity.class);
		then(questionSetParticipantRepository).should().save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ParticipantStatus.ELIMINATED);
		assertThat(result.getStatus()).isEqualTo(ParticipantStatus.ELIMINATED);
	}

	@Test
	@DisplayName("LIVE_TIME 모드가 아니면 QuestionSetStatusException을 던진다")
	void participateLiveQuestionSet_NotLiveMode_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.solveMode(QuestionSetSolveMode.STUDY)
			.status(QuestionSetStatus.ONGOING)
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
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.ONGOING)
			.build();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(questionSetParticipantRepository.findByQuestionSetAndUserId(questionSet, userId))
			.willReturn(Optional.empty());
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));
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
	@DisplayName("이미 참가한 유저면 기존 상태를 반환하고 이벤트를 발행하지 않는다")
	void participateLiveQuestionSet_AlreadyParticipated_ReturnsExistingStatus() {
		// given
		Long questionSetId = 1L;
		Long userId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.ONGOING)
			.build();

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		QuestionSetParticipantEntity existingParticipant = QuestionSetParticipantEntity.builder()
			.questionSet(questionSet)
			.user(user)
			.status(ParticipantStatus.ELIMINATED)
			.build();

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(questionSetParticipantRepository.findByQuestionSetAndUserId(questionSet, userId))
			.willReturn(Optional.of(existingParticipant));

		// when
		ParticipantDto result = questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);

		// then
		then(maitEventPublisher).should(never()).publishEvent(any());
		then(questionSetParticipantRepository).should(never()).save(any());
		assertThat(result.getStatus()).isEqualTo(ParticipantStatus.ELIMINATED);
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

	@Test
	@DisplayName("진출자 선정 시 advancementSelected 플래그가 true로 변경된다")
	void updateParticipantsStatus_MarksAdvancementSelected() {
		// given
		Long questionSetId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.ONGOING)
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
	@DisplayName("참가자 상태 업데이트 시 모든 참가자에게 개인 큐로 상태 메시지를 전송한다")
	void updateParticipantsStatus_SendsStatusToAllParticipants() {
		// given
		Long questionSetId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.ONGOING)
			.build();

		UserEntity user1 = mock(UserEntity.class);
		UserEntity user2 = mock(UserEntity.class);
		UserEntity user3 = mock(UserEntity.class);
		given(user1.getId()).willReturn(1L);
		given(user2.getId()).willReturn(2L);
		given(user3.getId()).willReturn(3L);

		QuestionSetParticipantEntity participant1 = QuestionSetParticipantEntity.builder()
			.questionSet(questionSet).user(user1).status(ParticipantStatus.ACTIVE).build();
		QuestionSetParticipantEntity participant2 = QuestionSetParticipantEntity.builder()
			.questionSet(questionSet).user(user2).status(ParticipantStatus.ACTIVE).build();
		QuestionSetParticipantEntity participant3 = QuestionSetParticipantEntity.builder()
			.questionSet(questionSet).user(user3).status(ParticipantStatus.ACTIVE).build();

		given(questionSetEntityRepository.findById(questionSetId)).willReturn(Optional.of(questionSet));
		given(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet))
			.willReturn(List.of(participant1, participant2, participant3));

		List<ParticipantDto> activeUsers = List.of(
			ParticipantDto.builder().userId(1L).build());
		List<ParticipantDto> eliminatedUsers = List.of(
			ParticipantDto.builder().userId(2L).build(),
			ParticipantDto.builder().userId(3L).build());

		// when
		questionSetParticipantService.updateParticipantsStatus(questionSetId, activeUsers, eliminatedUsers);

		// then
		then(questionWebSocketSender).should()
			.sendParticipantStatusChange(1L, questionSetId, ParticipantStatus.ACTIVE);
		then(questionWebSocketSender).should()
			.sendParticipantStatusChange(2L, questionSetId, ParticipantStatus.ELIMINATED);
		then(questionWebSocketSender).should()
			.sendParticipantStatusChange(3L, questionSetId, ParticipantStatus.ELIMINATED);
		then(questionWebSocketSender).should(times(3))
			.sendParticipantStatusChange(anyLong(), eq(questionSetId), any());
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
