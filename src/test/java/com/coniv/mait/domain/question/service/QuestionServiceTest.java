package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionException;
import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
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
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;
import com.coniv.mait.global.exception.custom.UserParameterException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long TEAM_ID = 10L;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private QuestionSetReader questionSetReader;

	// @InjectMocks
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
			questionReader,
			questionSetReader);
		lenient().when(questionSetReader.getQuestionSetForUpdate(anyLong()))
			.thenAnswer(invocation -> QuestionSetEntity.builder()
				.id(invocation.getArgument(0)).teamId(TEAM_ID).build());
		lenient().when(questionReader.getQuestion(anyList(), anyLong())).thenAnswer(invocation -> {
			List<QuestionEntity> questions = invocation.getArgument(0);
			Long questionId = invocation.getArgument(1);
			return questions.stream().filter(question -> question.getId().equals(questionId)).findFirst().orElseThrow();
		});
	}

	@Test
	@DisplayName("순서 변경 - 맨 앞으로 이동")
	void changeOrder_MoveToFront_WhenPrevNull() {
		// given
		QuestionEntity source = orderQuestion(10L, "Z");
		QuestionEntity next = orderQuestion(20L, "B");
		when(questionReader.getOrderedQuestionsForUpdate(1L)).thenReturn(List.of(next, source));

		// when
		questionService.changeQuestionOrder(1L, 10L, null, 20L, USER_ID);

		// then
		assertThat(source.getLexoRank()).isLessThan(next.getLexoRank());
	}

	@Test
	@DisplayName("순서 변경 - 맨 뒤로 이동")
	void changeOrder_MoveToEnd_WhenNextNull() {
		// given
		QuestionEntity source = orderQuestion(11L, "0");
		QuestionEntity prev = orderQuestion(21L, "A");
		when(questionReader.getOrderedQuestionsForUpdate(1L)).thenReturn(List.of(source, prev));

		// when
		questionService.changeQuestionOrder(1L, 11L, 21L, null, USER_ID);

		// then
		assertThat(source.getLexoRank()).isGreaterThan(prev.getLexoRank());
	}

	@Test
	@DisplayName("순서 변경 - 두 문제 사이로 이동")
	void changeOrder_MoveBetween_WhenPrevAndNextPresent() {
		// given
		QuestionEntity source = orderQuestion(12L, "Z");
		QuestionEntity prev = orderQuestion(22L, "A");
		QuestionEntity next = orderQuestion(23L, "C");
		when(questionReader.getOrderedQuestionsForUpdate(1L)).thenReturn(List.of(prev, next, source));

		// when
		questionService.changeQuestionOrder(1L, 12L, 22L, 23L, USER_ID);

		// then
		assertThat(source.getLexoRank()).isGreaterThan("A").isLessThan("C");
	}

	@Test
	@DisplayName("같은 위치로 이동한 후의 오래된 요청은 중복 키를 만들지 않는다")
	void changeOrder_staleNeighbors_rejectsWithoutChangingRank() {
		// given
		QuestionEntity first = orderQuestion(1L, "A");
		QuestionEntity last = orderQuestion(2L, "Z");
		QuestionEntity moved = orderQuestion(3L, "z");
		QuestionEntity source = orderQuestion(4L, "zz");
		when(questionReader.getOrderedQuestionsForUpdate(1L))
			.thenReturn(List.of(first, last, moved, source), List.of(first, moved, last, source));
		questionService.changeQuestionOrder(1L, moved.getId(), first.getId(), last.getId(), USER_ID);

		// when & then
		assertThatThrownBy(() -> questionService.changeQuestionOrder(1L, source.getId(), first.getId(),
			last.getId(), USER_ID)).isInstanceOfSatisfying(QuestionException.class,
				ex -> assertThat(ex.getExceptionCode()).isEqualTo(QuestionExceptionCode.ORDER_CHANGED));
		assertThat(source.getLexoRank()).isEqualTo("zz");
	}

	@Test
	@DisplayName("중간 키를 만들 수 없는 경계에서는 잘못된 위치에 저장하지 않는다")
	void changeOrder_noSpace_requiresRebalance() {
		// given
		QuestionEntity prev = orderQuestion(1L, "123");
		QuestionEntity next = orderQuestion(2L, "1230");
		QuestionEntity source = orderQuestion(3L, "Z");
		when(questionReader.getOrderedQuestionsForUpdate(1L)).thenReturn(List.of(prev, next, source));

		// when & then
		assertThatThrownBy(() -> questionService.changeQuestionOrder(1L, 3L, 1L, 2L, USER_ID))
			.isInstanceOfSatisfying(QuestionException.class,
				ex -> assertThat(ex.getExceptionCode()).isEqualTo(QuestionExceptionCode.ORDER_REBALANCE_REQUIRED));
		assertThat(source.getLexoRank()).isEqualTo("Z");
	}

	@Test
	@DisplayName("동률인 이웃 사이의 이동은 재배치 전까지 차단한다")
	void changeOrder_duplicateNeighborRanks_requiresRebalance() {
		// given
		QuestionEntity source = orderQuestion(3L, "Z");
		when(questionReader.getOrderedQuestionsForUpdate(1L))
			.thenReturn(List.of(orderQuestion(1L, "A"), orderQuestion(2L, "A"), source));

		// when & then
		assertThatThrownBy(() -> questionService.changeQuestionOrder(1L, 3L, 1L, 2L, USER_ID))
			.isInstanceOfSatisfying(QuestionException.class,
				ex -> assertThat(ex.getExceptionCode()).isEqualTo(QuestionExceptionCode.ORDER_REBALANCE_REQUIRED));
		assertThat(source.getLexoRank()).isEqualTo("Z");
	}

	@Test
	@DisplayName("자기 자신을 이웃으로 지정할 수 없다")
	void changeOrder_selfNeighbor_rejects() {
		// given
		QuestionEntity source = orderQuestion(1L, "A");
		when(questionReader.getOrderedQuestionsForUpdate(1L)).thenReturn(List.of(source));

		// when & then
		assertThatThrownBy(() -> questionService.changeQuestionOrder(1L, 1L, 1L, null, USER_ID))
			.isInstanceOf(UserParameterException.class);
		assertThat(source.getLexoRank()).isEqualTo("A");
	}

	@Test
	@DisplayName("기본 문제 추가는 순서 키의 저장 길이를 초과하지 않는다")
	void createDefaultQuestion_rankTooLong_requiresRebalance() {
		// given
		when(questionReader.getOrderedQuestionsForUpdate(1L))
			.thenReturn(List.of(orderQuestion(1L, "z".repeat(255))));

		// when & then
		assertThatThrownBy(() -> questionService.createDefaultQuestion(1L, USER_ID))
			.isInstanceOfSatisfying(QuestionException.class,
				ex -> assertThat(ex.getExceptionCode()).isEqualTo(QuestionExceptionCode.ORDER_REBALANCE_REQUIRED));
		verify(questionEntityRepository, never()).save(any());
	}

	private QuestionEntity orderQuestion(Long id, String rank) {
		return MultipleQuestionEntity.builder().id(id).lexoRank(rank)
			.questionSet(QuestionSetEntity.builder().id(1L).teamId(TEAM_ID).build()).build();
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
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		MultipleQuestionEntity multipleQuestion = mock(MultipleQuestionEntity.class);
		when(multipleQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(multipleQuestion.getType()).thenReturn(QuestionType.MULTIPLE);

		MultipleQuestionDto expectedDto = mock(MultipleQuestionDto.class);
		when(multipleQuestionFactory.getQuestion(multipleQuestion, true)).thenReturn(expectedDto);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(multipleQuestion));

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then
		assertNotNull(result);
		assertEquals(expectedDto, result);
		verify(questionEntityRepository).findById(questionId);
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

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.empty());

		// when & then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW));

		verify(questionEntityRepository).findById(questionId);
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
		final Long differentQuestionSetId = 2L;

		QuestionSetEntity differentQuestionSet = mock(QuestionSetEntity.class);
		when(differentQuestionSet.getId()).thenReturn(differentQuestionSetId);

		MultipleQuestionEntity multipleQuestion = mock(MultipleQuestionEntity.class);
		when(multipleQuestion.getQuestionSet()).thenReturn(differentQuestionSet);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(multipleQuestion));

		// when & then
		ResourceNotBelongException exception = assertThrows(ResourceNotBelongException.class,
			() -> questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW));

		assertEquals("해당 문제 셋에 속한 문제가 아닙니다.", exception.getMessage());
		verify(questionEntityRepository).findById(questionId);
		verify(multipleQuestionFactory, never()).getQuestion(any(), anyBoolean());
	}

	@Test
	@DisplayName("문제 셋의 모든 문제 조회 성공 - lexo rank 순으로 정렬")
	void getQuestions_Success() {
		// given
		final Long questionSetId = 1L;

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
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(questions);

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
		List<QuestionDto> result = questionService.getQuestions(questionSetId, DeliveryMode.MAKING);

		// then
		assertNotNull(result);
		assertEquals(4, result.size());

		// lexoRank 순으로 정렬되었는지 ID로 확인 (aaaa, bbbb, cccc, dddd)
		assertEquals(1L, result.get(0).getId()); // shortQuestion (lexoRank: aaaa)
		assertEquals(2L, result.get(1).getId()); // orderingQuestion (lexoRank: bbbb)
		assertEquals(3L, result.get(2).getId()); // multipleQuestion (lexoRank: cccc)
		assertEquals(4L, result.get(3).getId()); // fillBlankQuestion (lexoRank: dddd)

		// factory 메서드들이 올바르게 호출되었는지 확인
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(shortQuestionFactory).getQuestion(shortQuestion, true);
		verify(orderingQuestionFactory).getQuestion(orderingQuestion, true);
		verify(multipleQuestionFactory).getQuestion(multipleQuestion, true);
		verify(fillBlankQuestionFactory).getQuestion(fillBlankQuestion, true);
	}

	@Test
	@DisplayName("DeliveryMode에 따른 answerVisible 파라미터 전달 확인")
	void getQuestion_DeliveryMode_AnswerVisible() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(shortQuestion.getType()).thenReturn(QuestionType.SHORT);

		ShortQuestionDto expectedDto = mock(ShortQuestionDto.class);
		when(shortQuestionFactory.getQuestion(shortQuestion, false)).thenReturn(expectedDto);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(shortQuestion));
		when(questionSetEntity.isOnLive()).thenReturn(true);

		// when - LIVE_TIME 모드 (답안 숨김)
		questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME);

		// then - answerVisible = false로 호출되어야 함
		verify(shortQuestionFactory).getQuestion(shortQuestion, false);
	}

	@Test
	@DisplayName("진행 중이지 않은 실시간 문제 셋의 문제 조회 시 예외 발생")
	void getQuestion_DeliveryMode_LiveTime_NotLive() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestion.getQuestionSet()).thenReturn(questionSetEntity);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(shortQuestion));
		when(questionSetEntity.isOnLive()).thenReturn(false);

		// when
		QuestionSetStatusException questionSetStatusException = assertThrows(QuestionSetStatusException.class,
			() -> questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME));

		// then
		assertEquals(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME, questionSetStatusException.getExceptionCode());
		assertEquals("실시간 상태의 문제 셋만 처리가 가능합니다.", questionSetStatusException.getMessage());
		verify(questionEntityRepository).findById(questionId);
		verify(shortQuestionFactory, never()).getQuestion(any(), anyBoolean());
	}

	@Test
	@DisplayName("REVIEW 모드에서는 답안이 보여야 함")
	void getQuestion_ReviewMode_AnswerVisible() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(shortQuestion.getType()).thenReturn(QuestionType.SHORT);

		ShortQuestionDto expectedDto = mock(ShortQuestionDto.class);
		when(shortQuestionFactory.getQuestion(shortQuestion, true)).thenReturn(expectedDto);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(shortQuestion));

		// when - REVIEW 모드 (답안 표시)
		questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then - answerVisible = true로 호출되어야 함
		verify(shortQuestionFactory).getQuestion(shortQuestion, true);
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
		when(questionSetReader.getQuestionSetForUpdate(questionSetId)).thenReturn(questionSetEntity);

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

		verify(questionSetReader).getQuestionSetForUpdate(questionSetId);
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
		when(questionSetReader.getQuestionSetForUpdate(questionSetId)).thenReturn(questionSet);
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
		when(questionSetReader.getQuestionSetForUpdate(questionSetId)).thenReturn(questionSet);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		QuestionEntity question = mock(QuestionEntity.class);

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
