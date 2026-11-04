package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.dto.QuestionStatusMessage;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.exception.custom.QuestionSetLiveException;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionControlServiceTest {

	private static final Long USER_ID = 1L;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionReader questionReader;

	private static final Long TEAM_ID = 10L;

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@InjectMocks
	private QuestionControlService questionControlService;

	@Mock
	private QuestionEntity questionEntity;

	@Mock
	private QuestionSetEntity questionSetEntity;

	@Test
	@DisplayName("문제 접근 허용 - 성공")
	void allowQuestionAccess_Success() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.getId()).thenReturn(questionSetId);
		when(questionSetEntity.isOnLive()).thenReturn(true);

		// when
		questionControlService.allowQuestionAccess(questionSetId, questionId, USER_ID);

		// then
		verify(questionEntity).updateQuestionStatus(QuestionStatusType.ACCESS_PERMISSION);
		verify(questionWebSocketSender).broadcastQuestionStatus(eq(questionSetId),
			any(QuestionStatusMessage.class));
	}

	@Test
	@DisplayName("문제 풀이 허용 - 성공")
	void allowQuestionSolve_Success() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.getId()).thenReturn(questionSetId);
		when(questionSetEntity.isOnLive()).thenReturn(true);
		when(questionEntity.getQuestionStatus()).thenReturn(QuestionStatusType.ACCESS_PERMISSION);

		// when
		questionControlService.allowQuestionSolve(questionSetId, questionId, USER_ID);

		// then
		verify(questionEntity).updateQuestionStatus(QuestionStatusType.SOLVE_PERMISSION);
		verify(questionWebSocketSender).broadcastQuestionStatus(eq(questionSetId),
			any(QuestionStatusMessage.class));
	}

	@Test
	@DisplayName("문제 접근 허용 실패 - 존재하지 않는 문제")
	void allowQuestionAccess_QuestionNotFound() {
		// given
		Long questionSetId = 1L;
		Long questionId = 999L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.empty());

		// when & then
		assertThrows(EntityNotFoundException.class, () ->
			questionControlService.allowQuestionAccess(questionSetId, questionId, USER_ID));
	}

	@Test
	@DisplayName("문제 풀이 허용 실패 - 존재하지 않는 문제")
	void allowQuestionSolve_QuestionNotFound() {
		// given
		Long questionSetId = 1L;
		Long questionId = 999L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.empty());

		// when & then
		assertThrows(EntityNotFoundException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId, USER_ID));
	}

	@Test
	@DisplayName("문제 접근 허용 실패 - 문제가 다른 QuestionSet에 속함")
	void allowQuestionAccess_QuestionNotBelongsToSet() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long differentQuestionSetId = 2L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.isOnLive()).thenReturn(true);
		when(questionSetEntity.getId()).thenReturn(differentQuestionSetId);

		// when & then
		assertThrows(ResourceNotBelongException.class, () ->
			questionControlService.allowQuestionAccess(questionSetId, questionId, USER_ID));
	}

	@Test
	@DisplayName("문제 풀이 허용 실패 - 문제가 다른 QuestionSet에 속함")
	void allowQuestionSolve_QuestionNotBelongsToSet() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long differentQuestionSetId = 2L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.isOnLive()).thenReturn(true);
		when(questionSetEntity.getId()).thenReturn(differentQuestionSetId);

		// when & then
		assertThrows(ResourceNotBelongException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId, USER_ID));
	}

	@Test
	@DisplayName("문제 접근 허용 실패 - QuestionSet이 라이브 상태가 아님")
	void allowQuestionAccess_QuestionSetNotOnLive() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.getId()).thenReturn(questionSetId);
		when(questionSetEntity.isOnLive()).thenReturn(false);

		// when & then
		assertThrows(QuestionSetLiveException.class, () ->
			questionControlService.allowQuestionAccess(questionSetId, questionId, USER_ID));
	}

	@Test
	@DisplayName("문제 풀이 허용 실패 - QuestionSet이 라이브 상태가 아님")
	void allowQuestionSolve_QuestionSetNotOnLive() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.getId()).thenReturn(questionSetId);
		when(questionSetEntity.isOnLive()).thenReturn(false);

		// when & then
		assertThrows(QuestionSetLiveException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId, USER_ID));
	}

	@Test
	@DisplayName("문제 풀이 허용 실패 - 문제가 ACCESS_PERMISSION 상태가 아님")
	void allowQuestionSolve_QuestionNotInAccessPermission() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.getId()).thenReturn(questionSetId);
		when(questionSetEntity.isOnLive()).thenReturn(true);
		when(questionEntity.getQuestionStatus()).thenReturn(QuestionStatusType.NOT_OPEN);

		// when & then
		assertThrows(QuestionSetLiveException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId, USER_ID));
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 문제 접근을 허용할 수 없다")
	void allowQuestionAccess_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(question));
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionControlService.allowQuestionAccess(questionSetId, questionId, USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verifyNoInteractions(questionWebSocketSender);
		verify(question, never()).updateQuestionStatus(any());
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 문제 풀이를 허용할 수 없다")
	void allowQuestionSolve_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(question));
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionControlService.allowQuestionSolve(questionSetId, questionId, USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verifyNoInteractions(questionWebSocketSender);
		verify(question, never()).updateQuestionStatus(any());
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 문제 상태를 변경할 수 없다")
	void updateQuestionStatus_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionControlService.updateQuestionStatus(questionSetId, questionId,
			QuestionStatusType.ACCESS_PERMISSION, USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verifyNoInteractions(questionWebSocketSender);
		verify(question, never()).updateQuestionStatus(any());
	}
}
