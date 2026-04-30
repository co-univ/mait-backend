package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.UserStudyStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftId;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.event.StudySessionCompletedEvent;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.repository.StudyAnswerDraftEntityRepository;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.component.StudyAnswerDraftFactory;
import com.coniv.mait.domain.solve.service.dto.ShortQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.StudyAnswerDraftDto;
import com.coniv.mait.domain.solve.service.dto.StudyGradeResultDto;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.event.MaitEventPublisher;
import com.coniv.mait.web.question.dto.StudyQuestionSetGroup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class StudyModeServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long QUESTION_SET_ID = 10L;
	private static final Long TEAM_ID = 100L;
	private static final MaitUser MAIT_USER = MaitUser.builder().id(USER_ID).build();

	@InjectMocks
	private StudyModeService studyModeService;

	@Mock
	private UserReader userReader;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionSetReader questionSetReader;

	@Mock
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@Mock
	private StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository;

	@Mock
	private StudyAnswerDraftFactory studyAnswerDraftFactory;

	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private AnswerGrader answerGrader;

	@Mock
	private MaitEventPublisher maitEventPublisher;

	@Mock
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("getStudyQuestionSets - 유저 풀이 세션 기준으로 학습 문제 셋을 그룹화한다")
	void getStudyQuestionSets_GroupsByUserSolvingSession() {
		// given
		LocalDateTime now = LocalDateTime.now();
		QuestionSetEntity notStartedSet = mockStudyQuestionSet(1L, "아직 안 푼 문제", QuestionSetStatus.BEFORE,
			now.minusDays(2));
		QuestionSetEntity progressingSet = mockStudyQuestionSet(2L, "풀고 있는 문제", QuestionSetStatus.ONGOING,
			now.minusDays(1));
		QuestionSetEntity completedSet = mockStudyQuestionSet(3L, "채점 완료 문제", QuestionSetStatus.AFTER, now);

		SolvingSessionEntity progressingSession = mockStudySession(100L, 2L, SolvingStatus.PROGRESSING);
		SolvingSessionEntity completedSession = mockStudySession(101L, 3L, SolvingStatus.COMPLETE);

		when(solvingSessionEntityRepository.findAllByUserIdAndSolveModeAndQuestionSetTeamId(
			USER_ID, QuestionSetSolveMode.STUDY, TEAM_ID))
			.thenReturn(List.of(progressingSession, completedSession));
		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			eq(TEAM_ID), eq(QuestionSetSolveMode.STUDY), anyList()))
			.thenReturn(List.of(notStartedSet, progressingSet, completedSet));

		// when
		StudyQuestionSetGroup result = studyModeService.getStudyQuestionSets(TEAM_ID, MAIT_USER);

		// then
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE)).hasSize(1);
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE).get(0).getId()).isEqualTo(1L);
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE).get(0).getSolvingSessionId()).isNull();

		assertThat(result.questionSets().get(UserStudyStatus.ONGOING)).hasSize(1);
		assertThat(result.questionSets().get(UserStudyStatus.ONGOING).get(0).getId()).isEqualTo(2L);
		assertThat(result.questionSets().get(UserStudyStatus.ONGOING).get(0).getSolvingSessionId()).isEqualTo(100L);

		assertThat(result.questionSets().get(UserStudyStatus.AFTER)).hasSize(1);
		assertThat(result.questionSets().get(UserStudyStatus.AFTER).get(0).getId()).isEqualTo(3L);
		assertThat(result.questionSets().get(UserStudyStatus.AFTER).get(0).getSolvingSessionId()).isEqualTo(101L);

		verify(teamRoleValidator).checkIsTeamMember(TEAM_ID, USER_ID);
		verify(solvingSessionEntityRepository)
			.findAllByUserIdAndSolveModeAndQuestionSetTeamId(USER_ID, QuestionSetSolveMode.STUDY, TEAM_ID);
		verify(questionSetEntityRepository)
			.findAllByTeamIdAndSolveModeAndStatusIn(eq(TEAM_ID), eq(QuestionSetSolveMode.STUDY), anyList());
	}

	@Test
	@DisplayName("getStudyQuestionSets - 유저 세션이 없으면 BEFORE로 표시한다")
	void getStudyQuestionSets_NoSession_DisplaysAsBefore() {
		// given
		QuestionSetEntity questionSet = mockStudyQuestionSet(1L, "아직 안 푼 문제", QuestionSetStatus.ONGOING,
			LocalDateTime.now());

		when(solvingSessionEntityRepository.findAllByUserIdAndSolveModeAndQuestionSetTeamId(
			USER_ID, QuestionSetSolveMode.STUDY, TEAM_ID))
			.thenReturn(List.of());
		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			eq(TEAM_ID), eq(QuestionSetSolveMode.STUDY), anyList()))
			.thenReturn(List.of(questionSet));

		// when
		StudyQuestionSetGroup result = studyModeService.getStudyQuestionSets(TEAM_ID, MAIT_USER);

		// then
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE)).hasSize(1);
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE).get(0).getId()).isEqualTo(1L);
		assertThat(result.questionSets().get(UserStudyStatus.ONGOING)).isEmpty();
		assertThat(result.questionSets().get(UserStudyStatus.AFTER)).isEmpty();
	}

	@Test
	@DisplayName("startStudyMode - 기존 세션이 없으면 새 세션을 생성하고 draft를 생성한다")
	void startStudyMode_createNewSession_WhenNoExistingSession() {
		// given
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity savedSession = mock(SolvingSessionEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.empty());
		when(solvingSessionEntityRepository.save(any(SolvingSessionEntity.class))).thenReturn(savedSession);
		when(savedSession.getUser()).thenReturn(mockUser);
		when(savedSession.getQuestionSet()).thenReturn(mockQuestionSet);

		// when
		studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID);

		// then
		verify(solvingSessionEntityRepository).save(any(SolvingSessionEntity.class));
		verify(studyAnswerDraftFactory).createDrafts(savedSession, QUESTION_SET_ID);
	}

	@Test
	@DisplayName("startStudyMode - 기존 세션이 있으면 저장 없이 바로 반환한다")
	void startStudyMode_returnExistingSession_WhenSessionAlreadyExists() {
		// given
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity existingSession = mock(SolvingSessionEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.of(existingSession));
		when(existingSession.getUser()).thenReturn(mockUser);
		when(existingSession.getQuestionSet()).thenReturn(mockQuestionSet);

		// when
		studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID);

		// then
		verify(solvingSessionEntityRepository, never()).save(any());
		verify(studyAnswerDraftFactory, never()).createDrafts(any(), any());
	}

	@Test
	@DisplayName("startStudyMode - 존재하지 않는 문제 셋이면 EntityNotFoundException이 발생한다")
	void startStudyMode_throwException_WhenQuestionSetNotFound() {
		// given
		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class);

		verify(solvingSessionEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("startStudyMode - 존재하지 않는 유저이면 EntityNotFoundException이 발생한다")
	void startStudyMode_throwException_WhenUserNotFound() {
		// given
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenThrow(new EntityNotFoundException("해당 유저를 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class);

		verify(solvingSessionEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("startStudyMode - 팀 내 문제 풀기 권한이 없으면 UserRoleException이 발생한다")
	void startStudyMode_throwException_WhenNoAuthorityInTeam() {
		// given
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		doThrow(new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다."))
			.when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(UserRoleException.class);

		verify(solvingSessionEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("getStudyAnswerDrafts - 학습 세션의 draft 목록을 DTO로 반환한다")
	void getStudyAnswerDrafts_returnsDraftDtos() {
		// given
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity existingSession = mock(SolvingSessionEntity.class);

		Long solvingSessionId = 1000L;
		StudyAnswerDraftEntity draft1 = StudyAnswerDraftEntity.builder()
			.id(new StudyAnswerDraftId(solvingSessionId, 101L))
			.submittedAnswer("{\"a\":1}")
			.submitted(true)
			.build();
		StudyAnswerDraftEntity draft2 = StudyAnswerDraftEntity.builder()
			.id(new StudyAnswerDraftId(solvingSessionId, 102L))
			.submittedAnswer(null)
			.submitted(false)
			.build();

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.of(existingSession));
		when(existingSession.getId()).thenReturn(solvingSessionId);
		when(studyAnswerDraftFactory.getDraftsBySolvingSessionId(solvingSessionId)).thenReturn(List.of(draft1, draft2));

		// when
		List<StudyAnswerDraftDto> result = studyModeService.getStudyAnswerDrafts(MAIT_USER, QUESTION_SET_ID);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getSolvingSessionId()).isEqualTo(solvingSessionId);
		assertThat(result.getFirst().getQuestionId()).isEqualTo(101L);
		assertThat(result.get(0).getSubmittedAnswer()).isEqualTo("{\"a\":1}");
		assertThat(result.get(0).isSubmitted()).isTrue();
		assertThat(result.get(1).getQuestionId()).isEqualTo(102L);
		assertThat(result.get(1).getSubmittedAnswer()).isNull();
		assertThat(result.get(1).isSubmitted()).isFalse();
	}

	@Test
	@DisplayName("getStudyAnswerDrafts - 학습 세션이 없으면 EntityNotFoundException이 발생한다")
	void getStudyAnswerDrafts_throwsException_WhenSessionNotFound() {
		// given
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> studyModeService.getStudyAnswerDrafts(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 학습 세션 입니다.");
	}

	@Test
	@DisplayName("gradeStudySession - 제출/미제출 답안을 채점하고 결과를 반환한다")
	void gradeStudySession_success() {
		// given
		Long solvingSessionId = 1000L;
		Long questionId1 = 101L;
		Long questionId2 = 102L;
		String submittedAnswer = "{\"type\":\"SHORT\",\"submitAnswers\":[\"답안\"]}";

		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity mockSession = mock(SolvingSessionEntity.class);
		QuestionEntity mockQuestion = mock(QuestionEntity.class);

		StudyAnswerDraftEntity submittedDraft = StudyAnswerDraftEntity.builder()
			.id(new StudyAnswerDraftId(solvingSessionId, questionId1))
			.solvingSession(mockSession)
			.submittedAnswer(submittedAnswer)
			.submitted(true)
			.build();

		StudyAnswerDraftEntity unsubmittedDraft = StudyAnswerDraftEntity.builder()
			.id(new StudyAnswerDraftId(solvingSessionId, questionId2))
			.solvingSession(mockSession)
			.submitted(false)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(mockQuestionSet);
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.of(mockSession));
		when(mockSession.getId()).thenReturn(solvingSessionId);
		when(mockSession.getStatus()).thenReturn(SolvingStatus.PROGRESSING);
		when(mockSession.getQuestionSet()).thenReturn(mockQuestionSet);
		when(studyAnswerDraftFactory.getDraftsBySolvingSessionId(solvingSessionId))
			.thenReturn(List.of(submittedDraft, unsubmittedDraft));
		when(questionReader.getQuestion(questionId1)).thenReturn(mockQuestion);
		when(answerGrader.gradeAnswer(eq(mockQuestion), any(SubmitAnswerDto.class))).thenReturn(true);

		// when
		StudyGradeResultDto result = studyModeService.gradeStudySession(MAIT_USER, QUESTION_SET_ID);

		// then
		assertThat(result.getTotalCount()).isEqualTo(2);
		assertThat(result.getCorrectCount()).isEqualTo(1);
		assertThat(result.getSolvingSessionId()).isEqualTo(solvingSessionId);
		assertThat(result.getResults()).hasSize(2);
		assertThat(result.getResults().get(0).getQuestionId()).isEqualTo(questionId1);
		assertThat(result.getResults().get(0).isCorrect()).isTrue();
		assertThat(result.getResults().get(0).getSubmittedAnswer()).isEqualTo(submittedAnswer);
		assertThat(result.getResults().get(1).getQuestionId()).isEqualTo(questionId2);
		assertThat(result.getResults().get(1).isCorrect()).isFalse();
		assertThat(result.getResults().get(1).getSubmittedAnswer()).isNull();

		verify(answerSubmitRecordEntityRepository).saveAll(anyList());
		verify(mockSession).submit(2, 1);
		verify(maitEventPublisher).publishEvent(any(StudySessionCompletedEvent.class));
	}

	@Test
	@DisplayName("gradeStudySession - 문제셋이 없으면 EntityNotFoundException이 발생한다")
	void gradeStudySession_throwsException_WhenQuestionSetNotFound() {
		// given
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID))
			.thenThrow(new EntityNotFoundException(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> studyModeService.gradeStudySession(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("gradeStudySession - 학습 세션이 없으면 EntityNotFoundException이 발생한다")
	void gradeStudySession_throwsException_WhenSessionNotFound() {
		// given
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(mockQuestionSet);
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> studyModeService.gradeStudySession(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 학습 세션 입니다.");
	}

	@Test
	@DisplayName("gradeStudySession - 이미 채점된 세션이면 QuestionSolvingException이 발생한다")
	void gradeStudySession_throwsException_WhenAlreadyGraded() {
		// given
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity mockSession = mock(SolvingSessionEntity.class);

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(mockQuestionSet);
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.of(mockSession));
		when(mockSession.getStatus()).thenReturn(SolvingStatus.COMPLETE);

		// when & then
		assertThatThrownBy(() -> studyModeService.gradeStudySession(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(QuestionSolvingException.class)
			.satisfies(ex -> assertThat(((QuestionSolvingException)ex).getExceptionCode())
				.isEqualTo(QuestionSolveExceptionCode.ALREADY_GRADED));

		verify(answerSubmitRecordEntityRepository, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("updateStudyAnswerDraft - 답안 초안을 업데이트하고 DTO를 반환한다")
	void updateStudyAnswerDraft_success() throws JsonProcessingException {
		// given
		Long questionId = 101L;
		Long solvingSessionId = 1000L;
		SubmitAnswerDto<?> submitAnswer = new ShortQuestionSubmitAnswer(List.of("답안"));
		String serializedAnswer = "{\"type\":\"SHORT\",\"submitAnswers\":[\"답안\"]}";

		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity mockSession = mock(SolvingSessionEntity.class);

		StudyAnswerDraftId draftId = new StudyAnswerDraftId(solvingSessionId, questionId);
		StudyAnswerDraftEntity draft = StudyAnswerDraftEntity.builder()
			.id(draftId)
			.solvingSession(mockSession)
			.build();

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(mockQuestionSet);
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.of(mockSession));
		when(mockSession.getId()).thenReturn(solvingSessionId);
		when(studyAnswerDraftEntityRepository.findById(draftId)).thenReturn(Optional.of(draft));
		when(objectMapper.writeValueAsString(submitAnswer)).thenReturn(serializedAnswer);

		// when
		StudyAnswerDraftDto result = studyModeService.updateStudyAnswerDraft(MAIT_USER, QUESTION_SET_ID, questionId,
			submitAnswer);

		// then
		assertThat(result.getSubmittedAnswer()).isEqualTo(serializedAnswer);
		assertThat(result.isSubmitted()).isTrue();
		assertThat(result.getQuestionId()).isEqualTo(questionId);
		assertThat(result.getSolvingSessionId()).isEqualTo(solvingSessionId);
	}

	@Test
	@DisplayName("updateStudyAnswerDraft - 문제셋이 없으면 EntityNotFoundException이 발생한다")
	void updateStudyAnswerDraft_throwsException_WhenQuestionSetNotFound() {
		// given
		SubmitAnswerDto<?> submitAnswer = new ShortQuestionSubmitAnswer(List.of("답안"));
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID))
			.thenThrow(new EntityNotFoundException(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(
			() -> studyModeService.updateStudyAnswerDraft(MAIT_USER, QUESTION_SET_ID, 101L, submitAnswer))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("updateStudyAnswerDraft - 학습 세션이 없으면 EntityNotFoundException이 발생한다")
	void updateStudyAnswerDraft_throwsException_WhenSessionNotFound() {
		// given
		SubmitAnswerDto<?> submitAnswer = new ShortQuestionSubmitAnswer(List.of("답안"));
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(mockQuestionSet);
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(
			() -> studyModeService.updateStudyAnswerDraft(MAIT_USER, QUESTION_SET_ID, 101L, submitAnswer))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 학습 세션 입니다.");
	}

	@Test
	@DisplayName("updateStudyAnswerDraft - 답안 초안이 없으면 EntityNotFoundException이 발생한다")
	void updateStudyAnswerDraft_throwsException_WhenDraftNotFound() {
		// given
		Long questionId = 101L;
		Long solvingSessionId = 1000L;
		SubmitAnswerDto<?> submitAnswer = new ShortQuestionSubmitAnswer(List.of("답안"));

		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity mockSession = mock(SolvingSessionEntity.class);

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(mockQuestionSet);
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, QUESTION_SET_ID,
			QuestionSetSolveMode.STUDY)).thenReturn(Optional.of(mockSession));
		when(mockSession.getId()).thenReturn(solvingSessionId);

		StudyAnswerDraftId draftId = new StudyAnswerDraftId(solvingSessionId, questionId);
		when(studyAnswerDraftEntityRepository.findById(draftId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(
			() -> studyModeService.updateStudyAnswerDraft(MAIT_USER, QUESTION_SET_ID, questionId, submitAnswer))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 답안 초안 입니다.");
	}

	private QuestionSetEntity mockStudyQuestionSet(final Long id, final String subject,
		final QuestionSetStatus status, final LocalDateTime modifiedAt) {
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(id);
		when(questionSet.getSubject()).thenReturn(subject);
		when(questionSet.getStatus()).thenReturn(status);
		when(questionSet.getModifiedAt()).thenReturn(modifiedAt);
		return questionSet;
	}

	private SolvingSessionEntity mockStudySession(final Long id, final Long questionSetId,
		final SolvingStatus status) {
		SolvingSessionEntity session = mock(SolvingSessionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(session.getId()).thenReturn(id);
		when(session.getStatus()).thenReturn(status);
		when(session.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.getId()).thenReturn(questionSetId);
		return session;
	}
}
