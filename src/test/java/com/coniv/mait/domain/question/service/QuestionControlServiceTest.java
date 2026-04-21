package com.coniv.mait.domain.question.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

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
import com.coniv.mait.global.exception.custom.QuestionSetLiveException;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionControlServiceTest {

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private QuestionReader questionReader;

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

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(questionEntity);
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(List.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.isOnLive()).thenReturn(true);

		// when
		questionControlService.allowQuestionAccess(questionSetId, questionId);

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

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(questionEntity);
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(List.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.isOnLive()).thenReturn(true);
		when(questionEntity.getQuestionStatus()).thenReturn(QuestionStatusType.ACCESS_PERMISSION);

		// when
		questionControlService.allowQuestionSolve(questionSetId, questionId);

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

		when(questionReader.getQuestion(questionId, questionSetId))
			.thenThrow(new EntityNotFoundException("문제를 찾을 수 없습니다."));

		// when & then
		assertThrows(EntityNotFoundException.class, () ->
			questionControlService.allowQuestionAccess(questionSetId, questionId));
	}

	@Test
	@DisplayName("문제 풀이 허용 실패 - 존재하지 않는 문제")
	void allowQuestionSolve_QuestionNotFound() {
		// given
		Long questionSetId = 1L;
		Long questionId = 999L;

		when(questionReader.getQuestion(questionId, questionSetId))
			.thenThrow(new EntityNotFoundException("문제를 찾을 수 없습니다."));

		// when & then
		assertThrows(EntityNotFoundException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId));
	}

	@Test
	@DisplayName("문제 접근 허용 실패 - 문제가 다른 QuestionSet에 속함")
	void allowQuestionAccess_QuestionNotBelongsToSet() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionReader.getQuestion(questionId, questionSetId))
			.thenThrow(new ResourceNotBelongException("문제가 해당 문제 세트에 속하지 않습니다."));

		// when & then
		assertThrows(ResourceNotBelongException.class, () ->
			questionControlService.allowQuestionAccess(questionSetId, questionId));
	}

	@Test
	@DisplayName("문제 풀이 허용 실패 - 문제가 다른 QuestionSet에 속함")
	void allowQuestionSolve_QuestionNotBelongsToSet() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionReader.getQuestion(questionId, questionSetId))
			.thenThrow(new ResourceNotBelongException("문제가 해당 문제 세트에 속하지 않습니다."));

		// when & then
		assertThrows(ResourceNotBelongException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId));
	}

	@Test
	@DisplayName("문제 접근 허용 실패 - QuestionSet이 라이브 상태가 아님")
	void allowQuestionAccess_QuestionSetNotOnLive() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(questionEntity);
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.isOnLive()).thenReturn(false);

		// when & then
		assertThrows(QuestionSetLiveException.class, () ->
			questionControlService.allowQuestionAccess(questionSetId, questionId));
	}

	@Test
	@DisplayName("문제 풀이 허용 실패 - QuestionSet이 라이브 상태가 아님")
	void allowQuestionSolve_QuestionSetNotOnLive() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(questionEntity);
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.isOnLive()).thenReturn(false);

		// when & then
		assertThrows(QuestionSetLiveException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId));
	}

	@Test
	@DisplayName("문제 풀이 허용 실패 - 문제가 ACCESS_PERMISSION 상태가 아님")
	void allowQuestionSolve_QuestionNotInAccessPermission() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(questionEntity);
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.isOnLive()).thenReturn(true);
		when(questionEntity.getQuestionStatus()).thenReturn(QuestionStatusType.NOT_OPEN);
		
		// when & then
		assertThrows(QuestionSetLiveException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId));
	}
}
