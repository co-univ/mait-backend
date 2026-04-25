package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionSetStudyControlServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long QUESTION_SET_ID = 10L;
	private static final Long TEAM_ID = 100L;
	private static final MaitUser MAIT_USER = MaitUser.builder().id(USER_ID).build();

	@InjectMocks
	private QuestionSetStudyControlService questionSetStudyControlService;

	@Mock
	private QuestionSetReader questionSetReader;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Test
	@DisplayName("MAKER가 STUDY 모드 BEFORE 상태 문제 셋을 시작하면 ONGOING으로 전환된다")
	void startStudyQuestionSet_Success() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.STUDY)
			.status(QuestionSetStatus.BEFORE)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);

		// when
		questionSetStudyControlService.startStudyQuestionSet(MAIT_USER, QUESTION_SET_ID);

		// then
		assertThat(questionSet.getStatus()).isEqualTo(QuestionSetStatus.ONGOING);
		assertThat(questionSet.getStartTime()).isNotNull();
		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(TEAM_ID, USER_ID);
	}

	@Test
	@DisplayName("존재하지 않는 문제 셋 시작 시 EntityNotFoundException이 발생한다")
	void startStudyQuestionSet_QuestionSetNotFound_ThrowsException() {
		// given
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID))
			.thenThrow(new EntityNotFoundException(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() ->
			questionSetStudyControlService.startStudyQuestionSet(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class);
	}

	@Test
	@DisplayName("MAKER 권한이 없는 유저가 시작 요청 시 UserRoleException이 발생한다")
	void startStudyQuestionSet_NoMakerAuthority_ThrowsException() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.STUDY)
			.status(QuestionSetStatus.BEFORE)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		doThrow(new UserRoleException("문제 세트 생성 권한이 없습니다."))
			.when(teamRoleValidator).checkHasCreateQuestionSetAuthority(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() ->
			questionSetStudyControlService.startStudyQuestionSet(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(UserRoleException.class);
	}

	@Test
	@DisplayName("LIVE_TIME 모드 문제 셋에 STUDY 시작 요청 시 QuestionSetStatusException이 발생한다")
	void startStudyQuestionSet_WrongMode_ThrowsException() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.BEFORE)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);

		// when & then
		assertThatThrownBy(() ->
			questionSetStudyControlService.startStudyQuestionSet(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(QuestionSetStatusException.class);
	}

	@Test
	@DisplayName("ONGOING 상태 문제 셋을 시작 요청 시 QuestionSetStatusException이 발생한다")
	void startStudyQuestionSet_AlreadyOngoing_ThrowsException() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.STUDY)
			.status(QuestionSetStatus.ONGOING)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);

		// when & then
		assertThatThrownBy(() ->
			questionSetStudyControlService.startStudyQuestionSet(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(QuestionSetStatusException.class);
	}

	@Test
	@DisplayName("MAKER가 STUDY 모드 ONGOING 상태 문제 셋을 종료하면 AFTER로 전환된다")
	void endStudyQuestionSet_Success() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.STUDY)
			.status(QuestionSetStatus.ONGOING)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);

		// when
		questionSetStudyControlService.endStudyQuestionSet(MAIT_USER, QUESTION_SET_ID);

		// then
		assertThat(questionSet.getStatus()).isEqualTo(QuestionSetStatus.AFTER);
		assertThat(questionSet.getEndTime()).isNotNull();
		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(TEAM_ID, USER_ID);
	}

	@Test
	@DisplayName("존재하지 않는 문제 셋 종료 시 EntityNotFoundException이 발생한다")
	void endStudyQuestionSet_QuestionSetNotFound_ThrowsException() {
		// given
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID))
			.thenThrow(new EntityNotFoundException(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() ->
			questionSetStudyControlService.endStudyQuestionSet(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class);
	}

	@Test
	@DisplayName("MAKER 권한이 없는 유저가 종료 요청 시 UserRoleException이 발생한다")
	void endStudyQuestionSet_NoMakerAuthority_ThrowsException() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.STUDY)
			.status(QuestionSetStatus.ONGOING)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		doThrow(new UserRoleException("문제 세트 생성 권한이 없습니다."))
			.when(teamRoleValidator).checkHasCreateQuestionSetAuthority(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() ->
			questionSetStudyControlService.endStudyQuestionSet(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(UserRoleException.class);
	}

	@Test
	@DisplayName("LIVE_TIME 모드 문제 셋에 STUDY 종료 요청 시 QuestionSetStatusException이 발생한다")
	void endStudyQuestionSet_WrongMode_ThrowsException() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.ONGOING)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);

		// when & then
		assertThatThrownBy(() ->
			questionSetStudyControlService.endStudyQuestionSet(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(QuestionSetStatusException.class);
	}

	@Test
	@DisplayName("BEFORE 상태 문제 셋을 종료 요청 시 QuestionSetStatusException이 발생한다")
	void endStudyQuestionSet_NotOngoing_ThrowsException() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.STUDY)
			.status(QuestionSetStatus.BEFORE)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);

		// when & then
		assertThatThrownBy(() ->
			questionSetStudyControlService.endStudyQuestionSet(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(QuestionSetStatusException.class);
	}
}
