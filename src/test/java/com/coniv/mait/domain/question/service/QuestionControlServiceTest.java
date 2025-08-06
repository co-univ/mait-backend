package com.coniv.mait.domain.question.service;

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
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionControlServiceTest {

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
		//when(questionSetEntity.isOnLive()).thenReturn(true); //TODO: 비즈니스 익셉션 추가 후 주석 해제

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

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.getId()).thenReturn(questionSetId);
		//when(questionSetEntity.isOnLive()).thenReturn(true); //TODO: 비즈니스 익셉션 추가 후 주석 해제

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

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.empty());

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

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.empty());

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
		Long differentQuestionSetId = 2L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.getId()).thenReturn(differentQuestionSetId);

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
		Long differentQuestionSetId = 2L;

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(questionEntity));
		when(questionEntity.getQuestionSet()).thenReturn(questionSetEntity);
		when(questionSetEntity.getId()).thenReturn(differentQuestionSetId);

		// when & then
		assertThrows(ResourceNotBelongException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId));
	}

	//TODO: 비즈니스 익셉션 추가 후 주석 해제
	/*@Test
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
		assertThrows(IllegalArgumentException.class, () ->
			questionControlService.allowQuestionAccess(questionSetId, questionId));
	}*/

/*	@Test
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
		assertThrows(IllegalArgumentException.class, () ->
			questionControlService.allowQuestionSolve(questionSetId, questionId));
	}*/
}
