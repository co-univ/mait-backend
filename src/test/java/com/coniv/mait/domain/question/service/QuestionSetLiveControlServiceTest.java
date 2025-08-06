package com.coniv.mait.domain.question.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionSetLiveControlServiceTest {

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Mock
	private TeamUserEntityRepository teamUserRepository;

	@Mock
	private QuestionSetParticipantRepository questionSetParticipantRepository;

	@InjectMocks
	private QuestionSetLiveControlService questionSetLiveControlService;

	@Mock
	private QuestionSetEntity questionSetEntity;

	@Mock
	private QuestionSetParticipantEntity activeParticipant1;

	@Mock
	private QuestionSetParticipantEntity activeParticipant2;

	@Mock
	private QuestionSetParticipantEntity eliminatedParticipant;

	@Mock
	private UserEntity user1;

	@Mock
	private UserEntity user2;

	@Mock
	private UserEntity user3;

	@Test
	@DisplayName("활성 참가자 조회 - 성공")
	void getActiveParticipants_Success() {
		// given
		Long questionSetId = 1L;

		when(questionSetEntityRepository.findById(questionSetId))
			.thenReturn(Optional.of(questionSetEntity));

		// 참가자 목록 설정 (활성 2명, 탈락 1명)
		List<QuestionSetParticipantEntity> participants = Arrays.asList(
			activeParticipant1, activeParticipant2, eliminatedParticipant
		);
		when(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSetEntity))
			.thenReturn(participants);

		// 활성 참가자들 설정
		when(activeParticipant1.getStatus()).thenReturn(ParticipantStatus.ACTIVE);
		when(activeParticipant1.getId()).thenReturn(101L);
		when(activeParticipant1.getUser()).thenReturn(user1);
		when(activeParticipant1.getParticipantName()).thenReturn("김철수");
		when(user1.getId()).thenReturn(1L);

		when(activeParticipant2.getStatus()).thenReturn(ParticipantStatus.ACTIVE);
		when(activeParticipant2.getId()).thenReturn(102L);
		when(activeParticipant2.getUser()).thenReturn(user2);
		when(activeParticipant2.getParticipantName()).thenReturn("이영희");
		when(user2.getId()).thenReturn(2L);

		// 탈락한 참가자 설정
		when(eliminatedParticipant.getStatus()).thenReturn(ParticipantStatus.ELIMINATED);

		// when
		List<ParticipantDto> result = questionSetLiveControlService.getActiveParticipants(questionSetId);

		// then
		assertEquals(2, result.size());

		ParticipantDto participant1 = result.get(0);
		assertEquals(101L, participant1.getParticipantId());
		assertEquals(1L, participant1.getUserId());
		assertEquals("김철수", participant1.getParticipantName());

		ParticipantDto participant2 = result.get(1);
		assertEquals(102L, participant2.getParticipantId());
		assertEquals(2L, participant2.getUserId());
		assertEquals("이영희", participant2.getParticipantName());

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository).findAllByQuestionSetWithFetchJoinUser(questionSetEntity);
	}

	@Test
	@DisplayName("활성 참가자 조회 - QuestionSet이 존재하지 않음")
	void getActiveParticipants_QuestionSetNotFound() {
		// given
		Long questionSetId = 999L;

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

		// when & then
		assertThrows(EntityNotFoundException.class, () ->
			questionSetLiveControlService.getActiveParticipants(questionSetId));

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository, never()).findAllByQuestionSetWithFetchJoinUser(any());
	}

	@Test
	@DisplayName("활성 참가자 조회 - 참가자가 없는 경우")
	void getActiveParticipants_NoParticipants() {
		// given
		Long questionSetId = 1L;

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));
		when(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSetEntity))
			.thenReturn(List.of());

		// when
		List<ParticipantDto> result = questionSetLiveControlService.getActiveParticipants(questionSetId);

		// then
		assertTrue(result.isEmpty());

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository).findAllByQuestionSetWithFetchJoinUser(questionSetEntity);
	}

	@Test
	@DisplayName("활성 참가자 조회 - 모든 참가자가 탈락한 경우")
	void getActiveParticipants_AllParticipantsEliminated() {
		// given
		Long questionSetId = 1L;

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		List<QuestionSetParticipantEntity> participants = Arrays.asList(eliminatedParticipant);
		when(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSetEntity))
			.thenReturn(participants);

		when(eliminatedParticipant.getStatus()).thenReturn(ParticipantStatus.ELIMINATED);

		// when
		List<ParticipantDto> result = questionSetLiveControlService.getActiveParticipants(questionSetId);

		// then
		assertTrue(result.isEmpty());

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository).findAllByQuestionSetWithFetchJoinUser(questionSetEntity);
	}
}