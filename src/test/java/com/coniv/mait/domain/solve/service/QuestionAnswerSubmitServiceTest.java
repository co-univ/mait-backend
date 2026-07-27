package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.component.QuestionSetParticipantManager;
import com.coniv.mait.domain.solve.service.component.ScorerGenerator;
import com.coniv.mait.domain.solve.service.component.ScorerProcessor;
import com.coniv.mait.domain.solve.service.component.SubmitTimingProcessor;
import com.coniv.mait.domain.solve.service.dto.AnswerSubmitRecordDto;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitTimingDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerSubmitServiceTest {

	@InjectMocks
	private QuestionAnswerSubmitService questionAnswerSubmitService;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Mock
	private AnswerGrader answerGrader;

	@Mock
	private SubmitTimingProcessor submitTimingProcessor;

	@Mock
	private ScorerProcessor scorerProcessor;

	@Mock
	private ScorerGenerator scorerGenerator;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionSetParticipantManager questionSetParticipantManager;

	@Mock
	private ObjectMapper objectMapper;

	@Nested
	@DisplayName("submitAnswer 메서드")
	class SubmitAnswer {

		@Test
		@DisplayName("정상적으로 답안 제출 성공")
		void submitAnswer_Success() throws JsonProcessingException {
			// given
			Long questionSetId = 1L;
			Long questionId = 1L;
			Long userId = 1L;
			Long teamId = 1L;
			Long submitOrder = 1L;
			MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

			UserEntity mockUser = mock(UserEntity.class);
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			MultipleQuestionEntity mockQuestion = mock(MultipleQuestionEntity.class);

			when(mockUser.getId()).thenReturn(userId);
			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockQuestion.getQuestionSet()).thenReturn(mockQuestionSet);
			when(mockQuestion.canSolve()).thenReturn(true);
			when(mockQuestion.getId()).thenReturn(questionId);

			lenient().when(submitTimingProcessor.process(questionId))
				.thenReturn(new SubmitTimingDto(submitOrder, 0L));
			when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
			when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(mockQuestion);
			doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
			when(questionSetParticipantManager.isParticipating(mockUser, mockQuestionSet)).thenReturn(true);
			when(answerSubmitRecordEntityRepository.existsByUserIdAndQuestionIdAndIsCorrectTrue(userId, questionId))
				.thenReturn(false);
			when(answerGrader.gradeAnswer(mockQuestion, submitAnswer)).thenReturn(true);
			when(scorerProcessor.getScorer(questionId, userId, submitOrder)).thenReturn(userId);
			when(objectMapper.writeValueAsString(submitAnswer)).thenReturn("{\"submitAnswers\":[1]}");
			when(answerSubmitRecordEntityRepository.save(any(AnswerSubmitRecordEntity.class)))
				.thenAnswer(invocation -> {
					AnswerSubmitRecordEntity entity = invocation.getArgument(0);
					return AnswerSubmitRecordEntity.builder()
						.userId(entity.getUserId())
						.questionId(entity.getQuestionId())
						.isCorrect(entity.isCorrect())
						.submitOrder(entity.getSubmitOrder())
						.submittedAnswer(entity.getSubmittedAnswer())
						.build();
				});

			// when
			var result = questionAnswerSubmitService.submitAnswer(questionSetId, questionId, userId, submitAnswer);

			// then
			assertThat(result).isNotNull();
			assertThat(result.isCorrect()).isTrue();
			assertThat(result.getTimeGapMillis()).isEqualTo(0L);
			verify(answerSubmitRecordEntityRepository).save(any(AnswerSubmitRecordEntity.class));
			verify(scorerGenerator).updateScorer(questionId, userId, submitOrder);
		}

		@Test
		@DisplayName("문제를 풀 수 없는 상태인 경우 예외 발생")
		void submitAnswer_CannotSolve_ThrowsException() {
			// given
			Long questionSetId = 1L;
			Long questionId = 1L;
			Long userId = 1L;
			Long teamId = 1L;
			Long submitOrder = 1L;
			MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

			UserEntity mockUser = mock(UserEntity.class);
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			MultipleQuestionEntity mockQuestion = mock(MultipleQuestionEntity.class);

			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockQuestionSet.getVisibility()).thenReturn(QuestionSetVisibility.GROUP);
			when(mockQuestion.getQuestionSet()).thenReturn(mockQuestionSet);
			lenient().when(submitTimingProcessor.process(questionId))
				.thenReturn(new SubmitTimingDto(submitOrder, 0L));
			when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
			when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(mockQuestion);
			doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
			when(questionSetParticipantManager.isParticipating(mockUser, mockQuestionSet)).thenReturn(true);
			when(mockQuestion.canSolve()).thenReturn(false);

			// when & then
			assertThatThrownBy(() -> questionAnswerSubmitService.submitAnswer(
				questionSetId, questionId, userId, submitAnswer))
				.isInstanceOf(QuestionSolvingException.class)
				.hasMessage(QuestionSolveExceptionCode.CANNOT_SOLVE.getMessage());

			verify(answerGrader, never()).gradeAnswer(any(), any());
			verify(answerSubmitRecordEntityRepository, never()).save(any());
		}

		@Test
		@DisplayName("팀에서 문제 풀이 권한이 없는 경우 예외 발생")
		void submitAnswer_NoTeamAuthority_ThrowsException() {
			// given
			Long questionSetId = 1L;
			Long questionId = 1L;
			Long userId = 1L;
			Long teamId = 1L;
			Long submitOrder = 1L;
			MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

			UserEntity mockUser = mock(UserEntity.class);
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			MultipleQuestionEntity mockQuestion = mock(MultipleQuestionEntity.class);

			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockQuestion.getQuestionSet()).thenReturn(mockQuestionSet);

			lenient().when(submitTimingProcessor.process(questionId))
				.thenReturn(new SubmitTimingDto(submitOrder, 0L));
			when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
			when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(mockQuestion);
			doThrow(new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다."))
				.when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);

			// when & then
			assertThatThrownBy(() -> questionAnswerSubmitService.submitAnswer(
				questionSetId, questionId, userId, submitAnswer))
				.isInstanceOf(UserRoleException.class);

			verify(answerGrader, never()).gradeAnswer(any(), any());
			verify(answerSubmitRecordEntityRepository, never()).save(any());
		}

		@Test
		@DisplayName("문제 풀이에 참여하지 않은 유저인 경우 예외 발생")
		void submitAnswer_NotParticipated_ThrowsException() {
			// given
			Long questionSetId = 1L;
			Long questionId = 1L;
			Long userId = 1L;
			Long teamId = 1L;
			Long submitOrder = 1L;
			MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

			UserEntity mockUser = mock(UserEntity.class);
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			MultipleQuestionEntity mockQuestion = mock(MultipleQuestionEntity.class);

			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockQuestionSet.getVisibility()).thenReturn(QuestionSetVisibility.GROUP);
			when(mockQuestion.getQuestionSet()).thenReturn(mockQuestionSet);

			lenient().when(submitTimingProcessor.process(questionId))
				.thenReturn(new SubmitTimingDto(submitOrder, 0L));
			when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
			when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(mockQuestion);
			doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
			when(questionSetParticipantManager.isParticipating(mockUser, mockQuestionSet)).thenReturn(false);

			// when & then
			assertThatThrownBy(() -> questionAnswerSubmitService.submitAnswer(
				questionSetId, questionId, userId, submitAnswer))
				.isInstanceOf(QuestionSolvingException.class)
				.hasMessage(QuestionSolveExceptionCode.NOT_PARTICIPATED.getMessage());

			verify(answerGrader, never()).gradeAnswer(any(), any());
			verify(answerSubmitRecordEntityRepository, never()).save(any());
		}

		@Test
		@DisplayName("문제 셋이 PRIVATE인 경우 NEED_OPEN 예외 발생")
		void submitAnswer_QuestionSetPrivate_ThrowsException() {
			// given
			Long questionSetId = 1L;
			Long questionId = 1L;
			Long userId = 1L;
			Long teamId = 1L;
			Long submitOrder = 1L;
			MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

			UserEntity mockUser = mock(UserEntity.class);
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			MultipleQuestionEntity mockQuestion = mock(MultipleQuestionEntity.class);

			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockQuestionSet.getVisibility()).thenReturn(QuestionSetVisibility.PRIVATE);
			when(mockQuestion.getQuestionSet()).thenReturn(mockQuestionSet);

			lenient().when(submitTimingProcessor.process(questionId))
				.thenReturn(new SubmitTimingDto(submitOrder, 0L));
			when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
			when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(mockQuestion);
			doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);

			// when & then
			assertThatThrownBy(() -> questionAnswerSubmitService.submitAnswer(
				questionSetId, questionId, userId, submitAnswer))
				.isInstanceOf(QuestionSetStatusException.class)
				.extracting(ex -> ((QuestionSetStatusException)ex).getExceptionCode())
				.isEqualTo(QuestionSetStatusExceptionCode.NEED_OPEN);

			verify(questionSetParticipantManager, never()).isParticipating(any(), any());
			verify(answerGrader, never()).gradeAnswer(any(), any());
			verify(answerSubmitRecordEntityRepository, never()).save(any());
		}

		@Test
		@DisplayName("이미 정답을 제출한 경우 예외 발생")
		void submitAnswer_AlreadySubmittedCorrectAnswer_ThrowsException() {
			// given
			Long questionSetId = 1L;
			Long questionId = 1L;
			Long userId = 1L;
			Long teamId = 1L;
			Long submitOrder = 1L;
			MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

			UserEntity mockUser = mock(UserEntity.class);
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			MultipleQuestionEntity mockQuestion = mock(MultipleQuestionEntity.class);

			when(mockUser.getId()).thenReturn(userId);
			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockQuestionSet.getVisibility()).thenReturn(QuestionSetVisibility.GROUP);
			when(mockQuestion.getQuestionSet()).thenReturn(mockQuestionSet);
			when(mockQuestion.canSolve()).thenReturn(true);

			lenient().when(submitTimingProcessor.process(questionId))
				.thenReturn(new SubmitTimingDto(submitOrder, 0L));
			when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
			when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(mockQuestion);
			doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
			when(questionSetParticipantManager.isParticipating(mockUser, mockQuestionSet)).thenReturn(true);
			when(answerSubmitRecordEntityRepository.existsByUserIdAndQuestionIdAndIsCorrectTrue(userId, questionId))
				.thenReturn(true);

			// when & then
			assertThatThrownBy(() -> questionAnswerSubmitService.submitAnswer(
				questionSetId, questionId, userId, submitAnswer))
				.isInstanceOf(QuestionSolvingException.class)
				.hasMessage(QuestionSolveExceptionCode.ALREADY.getMessage());

			verify(answerGrader, never()).gradeAnswer(any(), any());
			verify(answerSubmitRecordEntityRepository, never()).save(any());
			verify(scorerGenerator, never()).updateScorer(any(), any(), any());
		}

		@Test
		@DisplayName("오답 제출 시 시간차 반환하고 scorer 업데이트 안됨")
		void submitAnswer_WrongAnswer_NoScorerUpdate() throws JsonProcessingException {
			// given
			Long questionSetId = 1L;
			Long questionId = 1L;
			Long userId = 1L;
			Long teamId = 1L;
			Long submitOrder = 2L;
			MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

			UserEntity mockUser = mock(UserEntity.class);
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			MultipleQuestionEntity mockQuestion = mock(MultipleQuestionEntity.class);

			when(mockUser.getId()).thenReturn(userId);
			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockQuestion.getQuestionSet()).thenReturn(mockQuestionSet);
			when(mockQuestion.canSolve()).thenReturn(true);
			when(mockQuestion.getId()).thenReturn(questionId);

			lenient().when(submitTimingProcessor.process(questionId))
				.thenReturn(new SubmitTimingDto(submitOrder, 1500L));
			when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
			when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(mockQuestion);
			doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
			when(questionSetParticipantManager.isParticipating(mockUser, mockQuestionSet)).thenReturn(true);
			when(answerSubmitRecordEntityRepository.existsByUserIdAndQuestionIdAndIsCorrectTrue(userId, questionId))
				.thenReturn(false);
			when(answerGrader.gradeAnswer(mockQuestion, submitAnswer)).thenReturn(false);
			when(objectMapper.writeValueAsString(submitAnswer)).thenReturn("{\"submitAnswers\":[1]}");
			when(answerSubmitRecordEntityRepository.save(any(AnswerSubmitRecordEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

			// when
			var result = questionAnswerSubmitService.submitAnswer(questionSetId, questionId, userId, submitAnswer);

			// then
			assertThat(result).isNotNull();
			assertThat(result.isCorrect()).isFalse();
			assertThat(result.getTimeGapMillis()).isEqualTo(1500L);
			verify(answerSubmitRecordEntityRepository).save(any(AnswerSubmitRecordEntity.class));
			verify(scorerGenerator, never()).updateScorer(any(), any(), any());
		}
	}

	@Nested
	@DisplayName("getSubmitRecords 메서드")
	class GetSubmitRecords {

		@Test
		@DisplayName("학습모드 기록은 생성 시각순으로 조회하고 미응답 답안은 null로 반환한다")
		void getSubmitRecords_StudyModeRecords_SortsByCreatedAtAndAllowsNullAnswer() {
			// given
			Long questionSetId = 1L;
			Long questionId = 10L;
			Long firstUserId = 100L;
			Long secondUserId = 200L;

			UserEntity firstUser = mock(UserEntity.class);
			UserEntity secondUser = mock(UserEntity.class);
			AnswerSubmitRecordEntity laterRecord = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity earlierRecord = mock(AnswerSubmitRecordEntity.class);

			when(firstUser.getId()).thenReturn(firstUserId);
			when(secondUser.getId()).thenReturn(secondUserId);

			when(laterRecord.getId()).thenReturn(2L);
			when(laterRecord.getUserId()).thenReturn(secondUserId);
			when(laterRecord.getQuestionId()).thenReturn(questionId);
			when(laterRecord.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 27, 20, 0));
			when(laterRecord.getSubmittedAnswer()).thenReturn(null);

			when(earlierRecord.getId()).thenReturn(1L);
			when(earlierRecord.getUserId()).thenReturn(firstUserId);
			when(earlierRecord.getQuestionId()).thenReturn(questionId);
			when(earlierRecord.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 27, 19, 59));
			when(earlierRecord.getSubmittedAnswer())
				.thenReturn("{\"type\":\"MULTIPLE\",\"submitAnswers\":[1]}");

			when(answerSubmitRecordEntityRepository.findAllByQuestionId(questionId))
				.thenReturn(List.of(laterRecord, earlierRecord));
			when(userEntityRepository.findAllById(List.of(secondUserId, firstUserId)))
				.thenReturn(List.of(firstUser, secondUser));

			// when
			List<AnswerSubmitRecordDto> result =
				questionAnswerSubmitService.getSubmitRecords(questionSetId, questionId);

			// then
			assertThat(result).extracting(AnswerSubmitRecordDto::getId)
				.containsExactly(1L, 2L);
			assertThat(result.get(0).getSubmittedAnswer()).isNotNull();
			assertThat(result.get(1).getSubmittedAnswer()).isNull();
		}
	}
}
