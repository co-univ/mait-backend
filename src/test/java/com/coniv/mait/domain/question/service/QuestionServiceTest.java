package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.external.AiCreateApiService;
import com.coniv.mait.domain.question.repository.AiRequestStatusManager;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.FillBlankQuestionFactory;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.component.OrderingQuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.component.ShortQuestionFactory;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.domain.question.util.LexoRank;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long TEAM_ID = 10L;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	private QuestionService questionService;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private MultipleQuestionFactory multipleQuestionFactory;

	@Mock
	private ShortQuestionFactory shortQuestionFactory;

	@Mock
	private OrderingQuestionFactory orderingQuestionFactory;
	@Mock
	private FillBlankQuestionFactory fillBlankQuestionFactory;

	@Mock
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Mock
	private QuestionImageService questionImageService;

	@Mock
	private AiCreateApiService aiCreateApiService;

	@Mock
	private AiRequestStatusManager aiRequestStatusManager;

	@BeforeEach
	void setUp() {
		// QuestionFactory들의 getQuestionType() 메서드 모킹 (QuestionService 생성자에서 호출됨)
		// 이 부분만 lenient 적용 - 생성자에서 Map 생성을 위해 모든 factory의 getQuestionType()이 호출됨
		lenient().when(multipleQuestionFactory.getQuestionType()).thenReturn(QuestionType.MULTIPLE);
		lenient().when(shortQuestionFactory.getQuestionType()).thenReturn(QuestionType.SHORT);
		lenient().when(orderingQuestionFactory.getQuestionType()).thenReturn(QuestionType.ORDERING);
		lenient().when(fillBlankQuestionFactory.getQuestionType()).thenReturn(QuestionType.FILL_BLANK);

		// QuestionService 수동 생성 (factory 리스트 전달)
		List<QuestionFactory<?>> factories = List.of(
			multipleQuestionFactory,
			shortQuestionFactory,
			orderingQuestionFactory,
			fillBlankQuestionFactory);

		questionService = new QuestionService(
			factories,
			questionEntityRepository,
			questionSetEntityRepository,
			multipleChoiceEntityRepository,
			questionImageService,
			aiCreateApiService,
			aiRequestStatusManager,
			teamRoleValidator,
			questionSetReader,
			questionReader,
			solvingSessionEntityRepository);
	}

	@Test
	@DisplayName("순서 변경 - prev=null 이면 맨 앞으로 이동 (next 이전 키 생성)")
	void changeOrder_MoveToFront_WhenPrevNull() {
		// given
		final Long questionSetId = 1L;
		final Long sourceQuestionId = 10L;
		final Long nextQuestionId = 20L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);

		QuestionEntity source = mock(QuestionEntity.class);
		when(source.getQuestionSet()).thenReturn(questionSet);

		QuestionEntity next = mock(QuestionEntity.class);
		when(next.getQuestionSet()).thenReturn(questionSet);
		when(next.getLexoRank()).thenReturn("B");

		when(questionEntityRepository.findById(sourceQuestionId)).thenReturn(Optional.of(source));
		when(questionEntityRepository.findById(nextQuestionId)).thenReturn(Optional.of(next));

		String expected = LexoRank.prevBefore("B");

		// when
		questionService.changeQuestionOrder(questionSetId, sourceQuestionId, null, nextQuestionId, USER_ID);

		// then
		verify(source).updateRank(expected);
	}

	@Test
	@DisplayName("순서 변경 - next=null 이면 맨 뒤로 이동 (prev 다음 키 생성)")
	void changeOrder_MoveToEnd_WhenNextNull() {
		// given
		final Long questionSetId = 1L;
		final Long sourceQuestionId = 11L;
		final Long prevQuestionId = 21L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);

		QuestionEntity source = mock(QuestionEntity.class);
		when(source.getQuestionSet()).thenReturn(questionSet);

		QuestionEntity prev = mock(QuestionEntity.class);
		when(prev.getQuestionSet()).thenReturn(questionSet);
		when(prev.getLexoRank()).thenReturn("A");

		when(questionEntityRepository.findById(sourceQuestionId)).thenReturn(Optional.of(source));
		when(questionEntityRepository.findById(prevQuestionId)).thenReturn(Optional.of(prev));

		String expected = LexoRank.nextAfter("A");

		// when
		questionService.changeQuestionOrder(questionSetId, sourceQuestionId, prevQuestionId, null, USER_ID);

		// then
		verify(source).updateRank(expected);
	}

	@Test
	@DisplayName("순서 변경 - prev와 next 사이로 이동 (between 생성)")
	void changeOrder_MoveBetween_WhenPrevAndNextPresent() {
		// given
		final Long questionSetId = 1L;
		final Long sourceQuestionId = 12L;
		final Long prevQuestionId = 22L;
		final Long nextQuestionId = 23L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);

		QuestionEntity source = mock(QuestionEntity.class);
		when(source.getQuestionSet()).thenReturn(questionSet);

		QuestionEntity prev = mock(QuestionEntity.class);
		when(prev.getQuestionSet()).thenReturn(questionSet);
		when(prev.getLexoRank()).thenReturn("A");

		QuestionEntity next = mock(QuestionEntity.class);
		when(next.getQuestionSet()).thenReturn(questionSet);
		when(next.getLexoRank()).thenReturn("C");

		when(questionEntityRepository.findById(sourceQuestionId)).thenReturn(Optional.of(source));
		when(questionEntityRepository.findById(prevQuestionId)).thenReturn(Optional.of(prev));
		when(questionEntityRepository.findById(nextQuestionId)).thenReturn(Optional.of(next));

		String expected = LexoRank.between("A", "C");

		// when
		questionService.changeQuestionOrder(questionSetId, sourceQuestionId, prevQuestionId, nextQuestionId, USER_ID);

		// then
		verify(source).updateRank(expected);
	}

	@DisplayName("문제 생성 성공 - 적절한 팩토리 호출 확인")
	void createQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		MultipleQuestionDto questionDto = mock(MultipleQuestionDto.class);

		// when
		questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto, USER_ID);

		// then
		verify(questionSetEntityRepository).findById(questionSetId);
		verify(multipleQuestionFactory).save(questionDto, questionSetEntity);

		// 다른 팩토리는 호출되지 않아야 함
		verify(shortQuestionFactory, never()).save(any(), any());
		verify(orderingQuestionFactory, never()).save(any(), any());
		verify(fillBlankQuestionFactory, never()).save(any(), any());
	}

	@Test
	@DisplayName("문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createQuestion_QuestionSetNotFound() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

		MultipleQuestionDto questionDto = mock(MultipleQuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto, USER_ID));

		verify(questionSetEntityRepository).findById(questionSetId);
		// 어떤 팩토리도 호출되지 않아야 함
		verify(multipleQuestionFactory, never()).save(any(), any());
		verify(shortQuestionFactory, never()).save(any(), any());
		verify(orderingQuestionFactory, never()).save(any(), any());
		verify(fillBlankQuestionFactory, never()).save(any(), any());
	}

	@Test
	@DisplayName("문제 조회 성공 - 적절한 팩토리 호출 확인")
	void getQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetReader.getQuestionSet(questionSetId)).thenReturn(questionSetEntity);


		MultipleQuestionEntity multipleQuestion = mock(MultipleQuestionEntity.class);
		when(multipleQuestion.getType()).thenReturn(QuestionType.MULTIPLE);

		MultipleQuestionDto expectedDto = mock(MultipleQuestionDto.class);
		when(multipleQuestionFactory.getQuestion(multipleQuestion, true)).thenReturn(expectedDto);

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(multipleQuestion);

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW, USER_ID);

		// then
		assertNotNull(result);
		assertEquals(expectedDto, result);
		verify(questionReader).getQuestion(questionId, questionSetId);
		verify(multipleQuestionFactory).getQuestion(multipleQuestion, true);

		// 다른 팩토리는 호출되지 않아야 함
		verify(shortQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(orderingQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(fillBlankQuestionFactory, never()).getQuestion(any(), anyBoolean());
	}

	@Test
	@DisplayName("문제 조회 실패 - 존재하지 않는 문제")
	void getQuestion_QuestionNotFound() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 999L;

		when(questionReader.getQuestion(questionId, questionSetId)).thenThrow(new EntityNotFoundException());

		// when & then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW, USER_ID));

		verify(questionReader).getQuestion(questionId, questionSetId);
		verify(multipleQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(shortQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(orderingQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(fillBlankQuestionFactory, never()).getQuestion(any(), anyBoolean());
	}

	@Test
	@DisplayName("문제 조회 실패 - 해당 문제셋에 속하지 않는 문제")
	void getQuestion_QuestionNotBelongToQuestionSet() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;
		when(questionReader.getQuestion(questionId, questionSetId))
			.thenThrow(new ResourceNotBelongException("문제가 해당 문제 세트에 속하지 않습니다."));

		// when & then
		ResourceNotBelongException exception = assertThrows(ResourceNotBelongException.class,
			() -> questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW, USER_ID));

		assertEquals("문제가 해당 문제 세트에 속하지 않습니다.", exception.getMessage());
		verify(questionReader).getQuestion(questionId, questionSetId);
		verify(multipleQuestionFactory, never()).getQuestion(any(), anyBoolean());
	}

	@ParameterizedTest
	@NullSource
	@EnumSource(value = DeliveryMode.class, names = "MAKING")
	@DisplayName("목록은 mode를 생략해도 제작 권한을 검사하고 lexo rank 순으로 정렬한다")
	void getQuestions_Success(DeliveryMode mode) {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetReader.getQuestionSet(questionSetId)).thenReturn(questionSetEntity);


		MultipleQuestionEntity multipleQuestion = mock(MultipleQuestionEntity.class);
		when(multipleQuestion.getLexoRank()).thenReturn("cccc"); // lexoRank: 3번째
		when(multipleQuestion.getType()).thenReturn(QuestionType.MULTIPLE);

		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestion.getLexoRank()).thenReturn("aaaa"); // lexoRank: 1번째
		when(shortQuestion.getType()).thenReturn(QuestionType.SHORT);

		OrderingQuestionEntity orderingQuestion = mock(OrderingQuestionEntity.class);
		when(orderingQuestion.getLexoRank()).thenReturn("bbbb"); // lexoRank: 2번째
		when(orderingQuestion.getType()).thenReturn(QuestionType.ORDERING);

		FillBlankQuestionEntity fillBlankQuestion = mock(FillBlankQuestionEntity.class);
		when(fillBlankQuestion.getLexoRank()).thenReturn("dddd"); // lexoRank: 4번째
		when(fillBlankQuestion.getType()).thenReturn(QuestionType.FILL_BLANK);

		// Repository에서 lexoRank와 다른 순서로 반환 (뒤섞인 상태)
		List<QuestionEntity> questions = List.of(fillBlankQuestion, multipleQuestion, shortQuestion, orderingQuestion);
		when(questionReader.getQuestionsByQuestionSet(questionSetEntity)).thenReturn(questions);

		// 각 factory의 getQuestion 메서드 모킹
		MultipleQuestionDto multipleDto = mock(MultipleQuestionDto.class);
		when(multipleDto.getId()).thenReturn(3L);

		ShortQuestionDto shortDto = mock(ShortQuestionDto.class);
		when(shortDto.getId()).thenReturn(1L);

		OrderingQuestionDto orderingDto = mock(OrderingQuestionDto.class);
		when(orderingDto.getId()).thenReturn(2L);

		FillBlankQuestionDto fillBlankDto = mock(FillBlankQuestionDto.class);
		when(fillBlankDto.getId()).thenReturn(4L);

		when(multipleQuestionFactory.getQuestion(multipleQuestion, true)).thenReturn(multipleDto);
		when(shortQuestionFactory.getQuestion(shortQuestion, true)).thenReturn(shortDto);
		when(orderingQuestionFactory.getQuestion(orderingQuestion, true)).thenReturn(orderingDto);
		when(fillBlankQuestionFactory.getQuestion(fillBlankQuestion, true)).thenReturn(fillBlankDto);

		// when
		List<QuestionDto> result = questionService.getQuestions(questionSetId, mode, USER_ID);

		// then
		assertNotNull(result);
		assertEquals(4, result.size());

		// lexoRank 순으로 정렬되었는지 ID로 확인 (aaaa, bbbb, cccc, dddd)
		assertEquals(1L, result.get(0).getId()); // shortQuestion (lexoRank: aaaa)
		assertEquals(2L, result.get(1).getId()); // orderingQuestion (lexoRank: bbbb)
		assertEquals(3L, result.get(2).getId()); // multipleQuestion (lexoRank: cccc)
		assertEquals(4L, result.get(3).getId()); // fillBlankQuestion (lexoRank: dddd)

		// factory 메서드들이 올바르게 호출되었는지 확인
		verify(teamRoleValidator).checkQuestionReadAuthority(questionSetEntity, USER_ID, DeliveryMode.MAKING);
		verify(questionSetEntity).validateDeliveryMode(DeliveryMode.MAKING);
		verify(questionReader).getQuestionsByQuestionSet(questionSetEntity);
		verify(shortQuestionFactory).getQuestion(shortQuestion, true);
		verify(orderingQuestionFactory).getQuestion(orderingQuestion, true);
		verify(multipleQuestionFactory).getQuestion(multipleQuestion, true);
		verify(fillBlankQuestionFactory).getQuestion(fillBlankQuestion, true);
	}

	@Test
	@DisplayName("PLAYER의 실시간 풀이 조회에서는 정답을 숨긴다")
	void getQuestion_PlayerLive_HidesAnswers() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetReader.getQuestionSet(questionSetId)).thenReturn(questionSetEntity);


		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestion.getType()).thenReturn(QuestionType.SHORT);

		ShortQuestionDto expectedDto = mock(ShortQuestionDto.class);
		when(shortQuestionFactory.getQuestion(shortQuestion, false)).thenReturn(expectedDto);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(shortQuestion);

		// when - LIVE_TIME 모드 (답안 숨김)
		questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME, USER_ID);

		// then - answerVisible = false로 호출되어야 함
		verify(shortQuestionFactory).getQuestion(shortQuestion, false);
		verifyNoInteractions(solvingSessionEntityRepository);
	}

	@Test
	@DisplayName("진행 중이지 않은 실시간 문제 셋의 문제 조회 시 예외 발생")
	void getQuestion_DeliveryMode_LiveTime_NotLive() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetReader.getQuestionSet(questionSetId)).thenReturn(questionSetEntity);
		doThrow(new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME))
			.when(questionSetEntity).validateDeliveryMode(DeliveryMode.LIVE_TIME);


		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(shortQuestion);

		// when
		QuestionSetStatusException questionSetStatusException = assertThrows(QuestionSetStatusException.class,
			() -> questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME, USER_ID));

		// then
		assertEquals(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME, questionSetStatusException.getExceptionCode());
		assertEquals("실시간 상태의 문제 셋만 처리가 가능합니다.", questionSetStatusException.getMessage());
		verify(questionReader).getQuestion(questionId, questionSetId);
		verify(shortQuestionFactory, never()).getQuestion(any(), anyBoolean());
	}

	@ParameterizedTest
	@NullSource
	@EnumSource(value = DeliveryMode.class, names = {"MAKING", "MANAGING"})
	@DisplayName("단건 조회는 기본 및 관리 모드의 권한을 검사한 뒤 정답을 공개한다")
	void getQuestion_Manager_SeesAnswers(DeliveryMode mode) {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetReader.getQuestionSet(questionSetId)).thenReturn(questionSetEntity);


		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestion.getType()).thenReturn(QuestionType.SHORT);

		ShortQuestionDto expectedDto = mock(ShortQuestionDto.class);
		when(shortQuestionFactory.getQuestion(shortQuestion, true)).thenReturn(expectedDto);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(shortQuestion);

		// when - 관리자 조회 (답안 표시)
		questionService.getQuestion(questionSetId, questionId, mode, USER_ID);

		// then - answerVisible = true로 호출되어야 함
		DeliveryMode expectedMode = mode == null ? DeliveryMode.MAKING : mode;
		verify(teamRoleValidator).checkQuestionReadAuthority(questionSetEntity, USER_ID, expectedMode);
		verify(questionSetEntity).validateDeliveryMode(expectedMode);
		verifyNoInteractions(solvingSessionEntityRepository);
		verify(shortQuestionFactory).getQuestion(shortQuestion, true);
	}

	@ParameterizedTest
	@CsvSource({"false, PROGRESSING, false", "true, PROGRESSING, false", "true, COMPLETE, true"})
	@DisplayName("학습 조회는 본인 채점 완료 여부를 단건과 목록의 정답 공개에 동일하게 적용한다")
	void getStudyQuestions_answerVisibility(boolean exists, SolvingStatus status, boolean visible) {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder().id(2L).teamId(TEAM_ID)
			.solveMode(QuestionSetSolveMode.STUDY).status(QuestionSetStatus.ONGOING).build();
		ShortQuestionEntity question = mock(ShortQuestionEntity.class);
		when(question.getType()).thenReturn(QuestionType.SHORT);
		when(questionSetReader.getQuestionSet(2L)).thenReturn(questionSet);
		when(questionReader.getQuestion(3L, 2L)).thenReturn(question);
		when(questionReader.getQuestionsByQuestionSet(questionSet)).thenReturn(List.of(question));
		SolvingSessionEntity session = mock(SolvingSessionEntity.class);
		if (exists) {
			when(session.getStatus()).thenReturn(status);
		}
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(USER_ID, 2L,
			QuestionSetSolveMode.STUDY)).thenReturn(exists ? Optional.of(session) : Optional.empty());
		ShortQuestionDto dto = mock(ShortQuestionDto.class);
		when(shortQuestionFactory.getQuestion(question, visible)).thenReturn(dto);

		// when
		QuestionDto single = questionService.getQuestion(2L, 3L, DeliveryMode.STUDY, USER_ID);
		List<QuestionDto> list = questionService.getQuestions(2L, DeliveryMode.STUDY, USER_ID);

		// then
		assertSame(dto, single);
		assertEquals(List.of(dto), list);
		verify(teamRoleValidator, times(2)).checkQuestionReadAuthority(questionSet, USER_ID, DeliveryMode.STUDY);
		verify(shortQuestionFactory, times(2)).getQuestion(question, visible);
	}

	@Test
	@DisplayName("문제 수정 성공 - 같은 유형으로 변경")
	void updateQuestion_SameType_Success() {
		// given
		final Long questionId = 1L;
		final Long questionSetId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		MultipleQuestionEntity existingQuestion = mock(MultipleQuestionEntity.class);
		lenient().when(existingQuestion.getId()).thenReturn(questionId);
		when(existingQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(existingQuestion.getType()).thenReturn(QuestionType.MULTIPLE);

		MultipleQuestionDto questionDto = mock(MultipleQuestionDto.class);
		when(questionDto.getType()).thenReturn(QuestionType.MULTIPLE);
		when(questionDto.getContent()).thenReturn("수정된 문제 내용");
		when(questionDto.getExplanation()).thenReturn("수정된 해설");
		when(questionDto.getImageId()).thenReturn(100L);

		MultipleQuestionDto expectedResult = mock(MultipleQuestionDto.class);
		when(multipleQuestionFactory.getQuestion(existingQuestion, true)).thenReturn(expectedResult);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(existingQuestion));

		// when
		QuestionDto result = questionService.updateQuestion(questionSetId, questionId, questionDto, USER_ID);

		// then
		assertNotNull(result);
		assertEquals(expectedResult, result);

		// 같은 타입 수정 로직 검증
		verify(questionEntityRepository).findById(questionId);
		verify(existingQuestion).updateContent(questionDto.getContent());
		verify(existingQuestion).updateExplanation(questionDto.getExplanation());
		verify(multipleQuestionFactory).deleteSubEntities(existingQuestion);
		verify(multipleQuestionFactory).createSubEntities(questionDto, existingQuestion);
		verify(multipleQuestionFactory).getQuestion(existingQuestion, true);
		verify(questionImageService).unUseExistImage(existingQuestion.getImageId());

		// delete와 save는 호출되지 않아야 함
		verify(questionEntityRepository, never()).delete(any());
		verify(multipleQuestionFactory, never()).save(any(), any());
	}

	@Test
	@DisplayName("문제 수정 성공 - 다른 유형으로 변경")
	void updateQuestion_DifferentType_Success() {
		// given
		final Long questionId = 1L;
		final Long questionSetId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		MultipleQuestionEntity existingQuestion = mock(MultipleQuestionEntity.class);
		lenient().when(existingQuestion.getId()).thenReturn(questionId);
		when(existingQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(existingQuestion.getType()).thenReturn(QuestionType.MULTIPLE);

		ShortQuestionDto questionDto = mock(ShortQuestionDto.class);
		when(questionDto.getType()).thenReturn(QuestionType.SHORT);
		when(questionDto.getImageId()).thenReturn(200L);

		ShortQuestionEntity newQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestionFactory.create(questionDto, questionSetEntity)).thenReturn(newQuestion);

		ShortQuestionDto expectedResult = mock(ShortQuestionDto.class);
		when(shortQuestionFactory.getQuestion(newQuestion, true)).thenReturn(expectedResult);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(existingQuestion));

		// when
		QuestionDto result = questionService.updateQuestion(questionSetId, questionId, questionDto, USER_ID);

		// then
		assertNotNull(result);
		assertEquals(expectedResult, result);

		// 다른 타입 수정 로직 검증
		verify(questionEntityRepository).findById(questionId);
		verify(multipleQuestionFactory).deleteSubEntities(existingQuestion); // 기존 타입의 팩토리
		verify(questionEntityRepository).delete(existingQuestion);
		verify(shortQuestionFactory).create(questionDto, questionSetEntity); // 새로운 타입의 팩토리
		verify(shortQuestionFactory).getQuestion(newQuestion, true);

		verify(questionImageService).unUseExistImage(newQuestion.getImageId());
		// 같은 타입 수정 메서드들은 호출되지 않아야 함
		verify(existingQuestion, never()).updateContent(anyString());
		verify(existingQuestion, never()).updateExplanation(anyString());
		verify(multipleQuestionFactory, never()).createSubEntities(any(), any());
	}

	@Test
	@DisplayName("문제 수정 실패 - 존재하지 않는 문제")
	void updateQuestion_QuestionNotFound() {
		// given
		final Long questionId = 999L;
		final Long questionSetId = 1L;

		MultipleQuestionDto questionDto = mock(MultipleQuestionDto.class);
		lenient().when(questionDto.getType()).thenReturn(QuestionType.MULTIPLE);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.empty());

		// when & then
		EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
			() -> questionService.updateQuestion(questionSetId, questionId, questionDto, USER_ID));

		assertEquals("Question not found with id: " + questionId, exception.getMessage());

		verify(questionEntityRepository).findById(questionId);

		// 다른 작업들은 호출되지 않아야 함
		verify(multipleQuestionFactory, never()).deleteSubEntities(any());
		verify(multipleQuestionFactory, never()).createSubEntities(any(), any());
		verify(multipleQuestionFactory, never()).save(any(), any());
		verify(multipleQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(questionEntityRepository, never()).delete(any());
	}

	@Test
	@DisplayName("문제 수정 실패 - 해당 문제셋에 속하지 않는 문제")
	void updateQuestion_QuestionNotBelongToQuestionSet() {
		// given
		final Long questionId = 1L;
		final Long questionSetId = 1L;
		final Long differentQuestionSetId = 2L;

		QuestionSetEntity differentQuestionSet = mock(QuestionSetEntity.class);
		when(differentQuestionSet.getId()).thenReturn(differentQuestionSetId);

		MultipleQuestionEntity existingQuestion = mock(MultipleQuestionEntity.class);
		when(existingQuestion.getQuestionSet()).thenReturn(differentQuestionSet);

		MultipleQuestionDto questionDto = mock(MultipleQuestionDto.class);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(existingQuestion));

		// when & then
		ResourceNotBelongException exception = assertThrows(ResourceNotBelongException.class,
			() -> questionService.updateQuestion(questionSetId, questionId, questionDto, USER_ID));

		assertEquals("해당 문제 셋에 속한 문제가 아닙니다.", exception.getMessage());

		verify(questionEntityRepository).findById(questionId);

		// 다른 작업들은 호출되지 않아야 함
		verify(multipleQuestionFactory, never()).deleteSubEntities(any());
		verify(multipleQuestionFactory, never()).createSubEntities(any(), any());
		verify(multipleQuestionFactory, never()).save(any(), any());
		verify(multipleQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(questionEntityRepository, never()).delete(any());
		verify(existingQuestion, never()).updateContent(anyString());
		verify(existingQuestion, never()).updateExplanation(anyString());
	}

	@Test
	@DisplayName("문제 단건 삭제 성공 테스트")
	void deleteQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		MultipleQuestionEntity existingQuestion = mock(MultipleQuestionEntity.class);
		when(existingQuestion.getId()).thenReturn(questionId);
		when(existingQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(existingQuestion.getType()).thenReturn(QuestionType.MULTIPLE);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(existingQuestion));

		// when
		questionService.deleteQuestion(questionSetId, questionId, USER_ID);

		// then
		verify(questionEntityRepository).findById(questionId);
		verify(multipleQuestionFactory).deleteSubEntities(existingQuestion);
		verify(questionEntityRepository).deleteById(questionId);
	}

	@Test
	@DisplayName("기본 문제 생성 성공")
	void createDefaultQuestion_Success() {
		// given
		final Long questionSetId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		// ArgumentCaptor로 save된 QuestionEntity 캡처
		ArgumentCaptor<QuestionEntity> questionCaptor = ArgumentCaptor.forClass(QuestionEntity.class);
		when(questionEntityRepository.save(questionCaptor.capture())).thenAnswer(
			invocation -> invocation.getArgument(0));

		// multipleQuestionFactory.getQuestion() mock 설정
		MultipleQuestionDto expectedQuestionDto = mock(MultipleQuestionDto.class);
		when(expectedQuestionDto.getType()).thenReturn(QuestionType.MULTIPLE);

		when(multipleQuestionFactory.getQuestion(any(QuestionEntity.class), eq(true))).thenReturn(expectedQuestionDto);

		// when
		QuestionDto result = questionService.createDefaultQuestion(questionSetId, USER_ID);

		// then
		assertNotNull(result);
		assertEquals(QuestionType.MULTIPLE, result.getType());

		QuestionEntity savedQuestion = questionCaptor.getValue();
		assertNotNull(savedQuestion);
		assertNotNull(savedQuestion.getLexoRank());
		assertEquals(questionSetEntity, savedQuestion.getQuestionSet());

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionEntityRepository).save(any(QuestionEntity.class));
		verify(multipleQuestionFactory).getQuestion(any(QuestionEntity.class), eq(true));
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 문제를 생성할 수 없다")
	void createQuestion_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionService.createQuestion(questionSetId, QuestionType.MULTIPLE,
			mock(MultipleQuestionDto.class), USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verify(multipleQuestionFactory, never()).save(any(), any());
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 기본 문제를 생성할 수 없다")
	void createDefaultQuestion_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionService.createDefaultQuestion(questionSetId, USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verify(questionEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 문제를 수정할 수 없다")
	void updateQuestion_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(question));
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionService.updateQuestion(questionSetId, questionId,
			mock(MultipleQuestionDto.class), USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verify(multipleQuestionFactory, never()).deleteSubEntities(any());
		verify(questionEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 문제를 삭제할 수 없다")
	void deleteQuestion_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(question));
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionService.deleteQuestion(questionSetId, questionId, USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verify(questionEntityRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("문제 셋 팀의 MAKER 이상이 아니면 문제 순서를 변경할 수 없다")
	void changeQuestionOrder_NotTeamManager_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long nextQuestionId = 2L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(question));
		doThrow(new UserRoleException("팀 관리 권한이 없습니다."))
			.when(teamRoleValidator).checkIsTeamManager(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionService.changeQuestionOrder(questionSetId, questionId, null, nextQuestionId,
			USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verify(questionEntityRepository, never()).findById(nextQuestionId);
		verify(question, never()).updateRank(any());
	}
}
