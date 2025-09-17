package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.FillBlankQuestionFactory;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.component.OrderingQuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.component.ShortQuestionFactory;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionServiceTest {

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

	@BeforeEach
	void setUp() {
		// QuestionFactory들의 getQuestionType() 메서드 모킹
		when(multipleQuestionFactory.getQuestionType()).thenReturn(QuestionType.MULTIPLE);
		when(shortQuestionFactory.getQuestionType()).thenReturn(QuestionType.SHORT);
		when(orderingQuestionFactory.getQuestionType()).thenReturn(QuestionType.ORDERING);
		when(fillBlankQuestionFactory.getQuestionType()).thenReturn(QuestionType.FILL_BLANK);

		// QuestionService 수동 생성 (factory 리스트 전달)
		List<QuestionFactory<?>> factories = List.of(
			multipleQuestionFactory,
			shortQuestionFactory,
			orderingQuestionFactory,
			fillBlankQuestionFactory
		);

		questionService = new QuestionService(
			factories,
			questionEntityRepository,
			questionSetEntityRepository
		);
	}

	@Test
	@DisplayName("객관식 문제 생성 테스트")
	void createMultipleChoiceQuestionTest() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		MultipleQuestionDto questionDto = mock(MultipleQuestionDto.class);

		// multipleQuestionFactory.save 메서드 모킹
		doNothing().when(multipleQuestionFactory).save(questionDto, questionSetEntity);

		// when
		questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto);

		// then
		verify(multipleQuestionFactory).save(questionDto, questionSetEntity);
	}

	@Test
	@DisplayName("객관식 문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createMultipleChoiceQuestionFailTest() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

		MultipleQuestionDto questionDto = mock(MultipleQuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto));

		verify(multipleQuestionFactory, never()).save(any(), any());
	}

	@Test
	@DisplayName("주관식 문제 생성 테스트 - createQuestion(QuestionType.SHORT) 정상 동작")
	void createShortQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		ShortQuestionDto questionDto = mock(ShortQuestionDto.class);

		// shortQuestionFactory.save 메서드 모킹
		doNothing().when(shortQuestionFactory).save(questionDto, questionSetEntity);

		// when
		questionService.createQuestion(questionSetId, QuestionType.SHORT, questionDto);

		// then
		verify(shortQuestionFactory).save(questionDto, questionSetEntity);
	}

	@Test
	@DisplayName("주관식 문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createShortQuestion_QuestionSetNotFound() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());
		ShortQuestionDto questionDto = mock(ShortQuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.SHORT, questionDto));
		verify(shortQuestionFactory, never()).save(any(), any());
	}

	@Test
	@DisplayName("순서배열 문제 생성 테스트 - createQuestion(QuestionType.ORDERING) 정상 동작")
	void createOrderingQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		OrderingQuestionDto questionDto = mock(OrderingQuestionDto.class);

		// orderingQuestionFactory.save 메서드 모킹
		doNothing().when(orderingQuestionFactory).save(questionDto, questionSetEntity);

		// when
		questionService.createQuestion(questionSetId, QuestionType.ORDERING, questionDto);

		// then
		verify(orderingQuestionFactory).save(questionDto, questionSetEntity);
	}

	@Test
	@DisplayName("순서배열 문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createOrderingQuestion_QuestionSetNotFound() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());
		OrderingQuestionDto questionDto = mock(OrderingQuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.ORDERING, questionDto));
		verify(orderingQuestionFactory, never()).save(any(), any());
	}

	@Test
	@DisplayName("빈칸 문제 생성 테스트 - createQuestion(QuestionType.FILL_BLANK) 정상 동작")
	void createFillBlankQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		FillBlankQuestionDto questionDto = mock(FillBlankQuestionDto.class);

		// fillBlankQuestionFactory.save 메서드 모킹
		doNothing().when(fillBlankQuestionFactory).save(questionDto, questionSetEntity);

		// when
		questionService.createQuestion(questionSetId, QuestionType.FILL_BLANK, questionDto);

		// then
		verify(fillBlankQuestionFactory).save(questionDto, questionSetEntity);
	}

	@Test
	@DisplayName("빈칸 문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createFillBlankQuestion_QuestionSetNotFound() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());
		FillBlankQuestionDto questionDto = mock(FillBlankQuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.FILL_BLANK, questionDto));
		verify(fillBlankQuestionFactory, never()).save(any(), any());
	}

	@Test
	@DisplayName("빈칸 문제 생성 테스트 - createQuestion(QuestionType.FILL_BLANK) 정상 동작")
	void createFillBlankQuestion_Success_via_createQuestion() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));
		FillBlankQuestionDto questionDto = mock(FillBlankQuestionDto.class);

		// fillBlankQuestionFactory.save 메서드 모킹
		doNothing().when(fillBlankQuestionFactory).save(questionDto, questionSetEntity);

		// when
		questionService.createQuestion(questionSetId, QuestionType.FILL_BLANK, questionDto);

		// then
		verify(fillBlankQuestionFactory).save(questionDto, questionSetEntity);
	}

	@Test
	@DisplayName("객관식 문제 조회 성공")
	void getMultipleQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		MultipleQuestionEntity multipleQuestion = mock(MultipleQuestionEntity.class);
		when(multipleQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(multipleQuestion.getId()).thenReturn(questionId);
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
	}

	@Test
	@DisplayName("주관식 문제 조회 성공")
	void getShortQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 2L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(shortQuestion.getId()).thenReturn(questionId);
		when(shortQuestion.getType()).thenReturn(QuestionType.SHORT);

		ShortQuestionDto expectedDto = mock(ShortQuestionDto.class);
		when(shortQuestionFactory.getQuestion(shortQuestion, true)).thenReturn(expectedDto);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(shortQuestion));

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then
		assertNotNull(result);
		assertEquals(expectedDto, result);
		verify(questionEntityRepository).findById(questionId);
		verify(shortQuestionFactory).getQuestion(shortQuestion, true);
	}

	@Test
	@DisplayName("순서배열 문제 조회 성공")
	void getOrderingQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 3L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		OrderingQuestionEntity orderingQuestion = mock(OrderingQuestionEntity.class);
		when(orderingQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(orderingQuestion.getId()).thenReturn(questionId);
		when(orderingQuestion.getType()).thenReturn(QuestionType.ORDERING);

		OrderingQuestionDto expectedDto = mock(OrderingQuestionDto.class);
		when(orderingQuestionFactory.getQuestion(orderingQuestion, true)).thenReturn(expectedDto);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(orderingQuestion));

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then
		assertNotNull(result);
		assertEquals(expectedDto, result);
		verify(questionEntityRepository).findById(questionId);
		verify(orderingQuestionFactory).getQuestion(orderingQuestion, true);
	}

	@Test
	@DisplayName("빈칸채우기 문제 조회 성공")
	void getFillBlankQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 4L;

		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntity.getId()).thenReturn(questionSetId);

		FillBlankQuestionEntity fillBlankQuestion = mock(FillBlankQuestionEntity.class);
		when(fillBlankQuestion.getQuestionSet()).thenReturn(questionSetEntity);
		when(fillBlankQuestion.getId()).thenReturn(questionId);
		when(fillBlankQuestion.getType()).thenReturn(QuestionType.FILL_BLANK);

		FillBlankQuestionDto expectedDto = mock(FillBlankQuestionDto.class);
		when(fillBlankQuestionFactory.getQuestion(fillBlankQuestion, true)).thenReturn(expectedDto);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(fillBlankQuestion));

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then
		assertNotNull(result);
		assertEquals(expectedDto, result);
		verify(questionEntityRepository).findById(questionId);
		verify(fillBlankQuestionFactory).getQuestion(fillBlankQuestion, true);
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
	@DisplayName("문제 셋의 모든 문제 조회 성공 - number 순으로 정렬")
	void getQuestions_Success() {
		// given
		final Long questionSetId = 1L;

		// 다양한 타입의 문제들을 number 순서가 뒤섞인 상태로 생성
		MultipleQuestionEntity multipleQuestion = mock(MultipleQuestionEntity.class);
		when(multipleQuestion.getNumber()).thenReturn(3L);
		when(multipleQuestion.getId()).thenReturn(1L);

		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);
		when(shortQuestion.getNumber()).thenReturn(1L);
		when(shortQuestion.getId()).thenReturn(2L);

		OrderingQuestionEntity orderingQuestion = mock(OrderingQuestionEntity.class);
		when(orderingQuestion.getNumber()).thenReturn(2L);
		when(orderingQuestion.getId()).thenReturn(3L);

		FillBlankQuestionEntity fillBlankQuestion = mock(FillBlankQuestionEntity.class);
		when(fillBlankQuestion.getNumber()).thenReturn(4L);
		when(fillBlankQuestion.getId()).thenReturn(4L);

		// Repository에서 순서가 뒤섞인 상태로 반환
		List<QuestionEntity> questions = List.of(multipleQuestion, fillBlankQuestion, shortQuestion, orderingQuestion);
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(questions);

		// 각 문제 타입 설정
		when(multipleQuestion.getType()).thenReturn(QuestionType.MULTIPLE);
		when(shortQuestion.getType()).thenReturn(QuestionType.SHORT);
		when(orderingQuestion.getType()).thenReturn(QuestionType.ORDERING);
		when(fillBlankQuestion.getType()).thenReturn(QuestionType.FILL_BLANK);

		// 각 factory의 getQuestion 메서드 모킹 (정렬 순서 확인을 위해 number 설정)
		MultipleQuestionDto multipleDto = mock(MultipleQuestionDto.class);
		when(multipleDto.getNumber()).thenReturn(3L);
		
		ShortQuestionDto shortDto = mock(ShortQuestionDto.class);
		when(shortDto.getNumber()).thenReturn(1L);
		
		OrderingQuestionDto orderingDto = mock(OrderingQuestionDto.class);
		when(orderingDto.getNumber()).thenReturn(2L);
		
		FillBlankQuestionDto fillBlankDto = mock(FillBlankQuestionDto.class);
		when(fillBlankDto.getNumber()).thenReturn(4L);
		
		when(multipleQuestionFactory.getQuestion(multipleQuestion, true)).thenReturn(multipleDto);
		when(shortQuestionFactory.getQuestion(shortQuestion, true)).thenReturn(shortDto);
		when(orderingQuestionFactory.getQuestion(orderingQuestion, true)).thenReturn(orderingDto);
		when(fillBlankQuestionFactory.getQuestion(fillBlankQuestion, true)).thenReturn(fillBlankDto);

		// when
		List<QuestionDto> result = questionService.getQuestions(questionSetId);

		// then
		assertNotNull(result);
		assertEquals(4, result.size());

		// number 순으로 정렬되었는지 확인 (1, 2, 3, 4)
		assertEquals(1L, result.get(0).getNumber()); // shortDto (number: 1)
		assertEquals(2L, result.get(1).getNumber()); // orderingDto (number: 2)
		assertEquals(3L, result.get(2).getNumber()); // multipleDto (number: 3)
		assertEquals(4L, result.get(3).getNumber()); // fillBlankDto (number: 4)

		// factory 메서드들이 올바르게 호출되었는지 확인
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(shortQuestionFactory).getQuestion(shortQuestion, true);
		verify(orderingQuestionFactory).getQuestion(orderingQuestion, true);
		verify(multipleQuestionFactory).getQuestion(multipleQuestion, true);
		verify(fillBlankQuestionFactory).getQuestion(fillBlankQuestion, true);
	}

	@Test
	@DisplayName("문제 셋의 모든 문제 조회 성공 - 빈 목록")
	void getQuestions_EmptyList() {
		// given
		final Long questionSetId = 1L;
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(List.of());

		// when
		List<QuestionDto> result = questionService.getQuestions(questionSetId);

		// then
		assertNotNull(result);
		assertEquals(0, result.size());
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);

		// 빈 목록이므로 factory 호출은 없어야 함
		verify(multipleQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(shortQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(orderingQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(fillBlankQuestionFactory, never()).getQuestion(any(), anyBoolean());
	}

	@Test
	@DisplayName("문제 셋의 모든 문제 조회 성공 - 단일 타입 (객관식만)")
	void getQuestions_SingleType_MultipleChoice() {
		// given
		final Long questionSetId = 1L;

		MultipleQuestionEntity question1 = mock(MultipleQuestionEntity.class);
		when(question1.getNumber()).thenReturn(2L);
		when(question1.getId()).thenReturn(1L);

		MultipleQuestionEntity question2 = mock(MultipleQuestionEntity.class);
		when(question2.getNumber()).thenReturn(1L);
		when(question2.getId()).thenReturn(2L);

		List<QuestionEntity> questions = List.of(question1, question2);
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(questions);

		// 문제 타입 설정
		when(question1.getType()).thenReturn(QuestionType.MULTIPLE);
		when(question2.getType()).thenReturn(QuestionType.MULTIPLE);

		// factory 메서드 모킹
		when(multipleQuestionFactory.getQuestion(question1, true)).thenReturn(mock(MultipleQuestionDto.class));
		when(multipleQuestionFactory.getQuestion(question2, true)).thenReturn(mock(MultipleQuestionDto.class));

		// when
		List<QuestionDto> result = questionService.getQuestions(questionSetId);

		// then
		assertNotNull(result);
		assertEquals(2, result.size());
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(multipleQuestionFactory).getQuestion(question1, true);
		verify(multipleQuestionFactory).getQuestion(question2, true);

		// 다른 타입의 factory는 호출되지 않아야 함
		verify(shortQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(orderingQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(fillBlankQuestionFactory, never()).getQuestion(any(), anyBoolean());
	}

	@Test
	@DisplayName("실시간 모드에서 주관식 문제 조회 시 답안은 null이고 개수 정보만 제공한다")
	void getShortQuestion_LiveMode_HidesAnswersButShowsCount() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		ShortQuestionEntity shortQuestion = mock(ShortQuestionEntity.class);

		when(questionSet.getId()).thenReturn(questionSetId);
		when(shortQuestion.getId()).thenReturn(questionId);
		when(shortQuestion.getQuestionSet()).thenReturn(questionSet);
		when(shortQuestion.getType()).thenReturn(QuestionType.SHORT);

		// 실제 ShortQuestionDto 생성 (mock이 아닌)
		ShortQuestionDto shortQuestionDto = ShortQuestionDto.builder()
			.content("테스트 주관식 문제")
			.number(1L)
			.shortAnswers(null) // 실시간 모드에서는 null
			.answerCount(3) // 개수만 제공
			.build();

		when(shortQuestionFactory.getQuestion(shortQuestion, false)).thenReturn(shortQuestionDto);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(shortQuestion));

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME);

		// then
		assertThat(result).isInstanceOf(ShortQuestionDto.class);
		ShortQuestionDto shortResult = (ShortQuestionDto)result;

		assertThat(shortResult.getShortAnswers()).isNull(); // 실시간 모드에서는 답안 숨김
		assertThat(shortResult.getAnswerCount()).isEqualTo(3); // 개수 정보는 제공

		verify(shortQuestionFactory).getQuestion(shortQuestion, false);
	}

	@Test
	@DisplayName("실시간 모드에서 빈칸 채우기 문제 조회 시 답안은 null이고 빈칸 개수 정보만 제공한다")
	void getFillBlankQuestion_LiveMode_HidesAnswersButShowsBlankCount() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		FillBlankQuestionEntity fillBlankQuestion = mock(FillBlankQuestionEntity.class);

		when(questionSet.getId()).thenReturn(questionSetId);
		when(fillBlankQuestion.getId()).thenReturn(questionId);
		when(fillBlankQuestion.getQuestionSet()).thenReturn(questionSet);
		when(fillBlankQuestion.getType()).thenReturn(QuestionType.FILL_BLANK);

		// 실제 FillBlankQuestionDto 생성 (mock이 아닌)
		FillBlankQuestionDto fillBlankQuestionDto = FillBlankQuestionDto.builder()
			.content("테스트 빈칸 문제")
			.number(1L)
			.fillBlankAnswers(null) // 실시간 모드에서는 null
			.blankCount(2) // 빈칸 개수만 제공
			.build();

		when(fillBlankQuestionFactory.getQuestion(fillBlankQuestion, false)).thenReturn(fillBlankQuestionDto);
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(fillBlankQuestion));

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME);

		// then
		assertThat(result).isInstanceOf(FillBlankQuestionDto.class);
		FillBlankQuestionDto fillBlankResult = (FillBlankQuestionDto)result;

		assertThat(fillBlankResult.getFillBlankAnswers()).isNull(); // 실시간 모드에서는 답안 숨김
		assertThat(fillBlankResult.getBlankCount()).isEqualTo(2); // 빈칸 개수는 제공 (number 1, 2)

		verify(fillBlankQuestionFactory).getQuestion(fillBlankQuestion, false);
	}
}
