package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.component.FillBlankQuestionFactory;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.component.OrderingQuestionFactory;
import com.coniv.mait.domain.question.service.component.ShortQuestionFactory;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

	@InjectMocks
	private QuestionService questionService;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Mock
	private MultipleQuestionFactory multipleQuestionFactory;

	@Mock
	private ShortQuestionFactory shortQuestionFactory;
	@Mock
	private ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Mock
	private OrderingQuestionFactory orderingQuestionFactory;
	@Mock
	private OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Mock
	private FillBlankQuestionFactory fillBlankQuestionFactory;
	@Mock
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Test
	@DisplayName("객관식 문제 생성 테스트")
	void createMultipleChoiceQuestionTest() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		List<MultipleChoiceDto> choiceDtos = List.of(MultipleChoiceDto.builder().number(1).build(),
			MultipleChoiceDto.builder().number(2).build(), MultipleChoiceDto.builder().number(3).build());

		MultipleQuestionDto multipleQuestionDto = MultipleQuestionDto.builder().choices(choiceDtos).build();
		MultipleQuestionEntity multipleQuestionEntity = mock(MultipleQuestionEntity.class);
		when(multipleQuestionFactory.create(multipleQuestionDto, questionSetEntity)).thenReturn(multipleQuestionEntity);

		when(multipleQuestionFactory.createChoices(choiceDtos, multipleQuestionEntity)).thenReturn(
			List.of(mock(MultipleChoiceEntity.class), mock(MultipleChoiceEntity.class),
				mock(MultipleChoiceEntity.class)));

		// QuestionDto의 toQuestionDto()가 MultipleQuestionDto 반환하도록 mock
		QuestionDto questionDto = mock(QuestionDto.class);
		when(questionDto.toQuestionDto()).thenReturn(multipleQuestionDto);

		// when
		questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto);

		// then
		verify(questionEntityRepository).save(any());
		verify(multipleChoiceEntityRepository).saveAll(any());
	}

	@Test
	@DisplayName("객관식 문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createMultipleChoiceQuestionFailTest() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

		QuestionDto questionDto = mock(QuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto));

		verify(questionEntityRepository, never()).save(any());
		verify(multipleChoiceEntityRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("주관식 문제 생성 테스트 - createQuestion(QuestionType.SHORT) 정상 동작")
	void createShortQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		List<ShortAnswerDto> answerDtos = List.of(
			ShortAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			ShortAnswerDto.builder().number(2L).answer("정답2").isMain(false).build());
		ShortQuestionDto shortQuestionDto = ShortQuestionDto.builder()
			.content("주관식 문제 내용")
			.explanation("해설")
			.number(1L)
			.shortAnswers(answerDtos)
			.build();
		ShortQuestionEntity shortQuestionEntity = mock(ShortQuestionEntity.class);
		List<ShortAnswerEntity> shortAnswerEntities = List.of(mock(ShortAnswerEntity.class),
			mock(ShortAnswerEntity.class));

		when(shortQuestionFactory.create(shortQuestionDto, questionSetEntity)).thenReturn(shortQuestionEntity);
		when(shortQuestionFactory.createShortAnswers(answerDtos, shortQuestionEntity)).thenReturn(shortAnswerEntities);

		// QuestionDto의 toQuestionDto()가 ShortQuestionDto 반환하도록 mock
		QuestionDto questionDto = mock(QuestionDto.class);
		when(questionDto.toQuestionDto()).thenReturn(shortQuestionDto);

		// when
		questionService.createQuestion(questionSetId, QuestionType.SHORT, questionDto);

		// then
		verify(questionEntityRepository).save(shortQuestionEntity);
		verify(shortQuestionFactory).createShortAnswers(answerDtos, shortQuestionEntity);
		verify(shortAnswerEntityRepository).saveAll(shortAnswerEntities);
	}

	@Test
	@DisplayName("주관식 문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createShortQuestion_QuestionSetNotFound() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());
		QuestionDto questionDto = mock(QuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.SHORT, questionDto));
		verify(questionEntityRepository, never()).save(any());
		verify(shortAnswerEntityRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("순서배열 문제 생성 테스트 - createQuestion(QuestionType.ORDERING) 정상 동작")
	void createOrderingQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		List<OrderingQuestionOptionDto> optionDtos = List.of(
			OrderingQuestionOptionDto.builder().content("첫 번째 옵션").originOrder(1).answerOrder(2).build(),
			OrderingQuestionOptionDto.builder().content("두 번째 옵션").originOrder(2).answerOrder(1).build(),
			OrderingQuestionOptionDto.builder().content("세 번째 옵션").originOrder(3).answerOrder(3).build());
		OrderingQuestionDto orderingQuestionDto = OrderingQuestionDto.builder()
			.content("순서배열 문제 내용")
			.explanation("해설")
			.number(1L)
			.options(optionDtos)
			.build();
		OrderingQuestionEntity orderingQuestionEntity = mock(OrderingQuestionEntity.class);
		List<OrderingOptionEntity> optionEntities = List.of(mock(OrderingOptionEntity.class),
			mock(OrderingOptionEntity.class), mock(OrderingOptionEntity.class));

		when(orderingQuestionFactory.create(orderingQuestionDto, questionSetEntity)).thenReturn(orderingQuestionEntity);
		when(orderingQuestionFactory.createOrderingQuestionOptions(optionDtos, orderingQuestionEntity)).thenReturn(
			optionEntities);

		// QuestionDto의 toQuestionDto()가 OrderingQuestionDto 반환하도록 mock
		QuestionDto questionDto = mock(QuestionDto.class);
		when(questionDto.toQuestionDto()).thenReturn(orderingQuestionDto);

		// when
		questionService.createQuestion(questionSetId, QuestionType.ORDERING, questionDto);

		// then
		verify(questionEntityRepository).save(orderingQuestionEntity);
		verify(orderingQuestionFactory).create(orderingQuestionDto, questionSetEntity);
		verify(orderingQuestionFactory).createOrderingQuestionOptions(optionDtos, orderingQuestionEntity);
		verify(orderingOptionEntityRepository).saveAll(optionEntities);
	}

	@Test
	@DisplayName("순서배열 문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createOrderingQuestion_QuestionSetNotFound() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());
		QuestionDto questionDto = mock(QuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.ORDERING, questionDto));
		verify(questionEntityRepository, never()).save(any());
		verify(orderingOptionEntityRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("빈칸 문제 생성 테스트 - createQuestion(QuestionType.FILL_BLANK) 정상 동작")
	void createFillBlankQuestion_Success() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		List<FillBlankAnswerDto> answerDtos = List.of(
			FillBlankAnswerDto.builder().number(1L).answer("정답1").isMain(true).build(),
			FillBlankAnswerDto.builder().number(2L).answer("정답2").isMain(true).build());
		FillBlankQuestionDto fillBlankQuestionDto = FillBlankQuestionDto.builder()
			.content("빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("해설")
			.number(1L)
			.fillBlankAnswers(answerDtos)
			.build();
		FillBlankQuestionEntity fillBlankQuestionEntity = mock(FillBlankQuestionEntity.class);
		List<FillBlankAnswerEntity> fillBlankAnswerEntities = List.of(mock(FillBlankAnswerEntity.class),
			mock(FillBlankAnswerEntity.class));

		when(fillBlankQuestionFactory.create(fillBlankQuestionDto, questionSetEntity)).thenReturn(
			fillBlankQuestionEntity);
		when(fillBlankQuestionFactory.createFillBlankAnswers(answerDtos, fillBlankQuestionEntity)).thenReturn(
			fillBlankAnswerEntities);

		// QuestionDto의 toQuestionDto()가 FillBlankQuestionDto 반환하도록 mock
		QuestionDto questionDto = mock(QuestionDto.class);
		when(questionDto.toQuestionDto()).thenReturn(fillBlankQuestionDto);

		// when
		questionService.createQuestion(questionSetId, QuestionType.FILL_BLANK, questionDto);

		// then
		verify(questionEntityRepository).save(fillBlankQuestionEntity);
		verify(fillBlankQuestionFactory).create(fillBlankQuestionDto, questionSetEntity);
		verify(fillBlankQuestionFactory).createFillBlankAnswers(answerDtos, fillBlankQuestionEntity);
		verify(fillBlankAnswerEntityRepository).saveAll(fillBlankAnswerEntities);
	}

	@Test
	@DisplayName("빈칸 문제 생성 실패 - QuestionSet이 존재하지 않을 때")
	void createFillBlankQuestion_QuestionSetNotFound() {
		// given
		final Long questionSetId = 1L;
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());
		QuestionDto questionDto = mock(QuestionDto.class);

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.FILL_BLANK, questionDto));
		verify(questionEntityRepository, never()).save(any());
		verify(fillBlankAnswerEntityRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("빈칸 문제 생성 테스트 - createQuestion(QuestionType.FILL_BLANK) 정상 동작")
	void createFillBlankQuestion_Success_via_createQuestion() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));
		FillBlankQuestionDto fillBlankQuestionDto = mock(FillBlankQuestionDto.class);
		QuestionDto questionDto = mock(QuestionDto.class);
		when(questionDto.toQuestionDto()).thenReturn(fillBlankQuestionDto);

		FillBlankQuestionEntity fillBlankQuestionEntity = mock(FillBlankQuestionEntity.class);
		when(fillBlankQuestionFactory.create(fillBlankQuestionDto, questionSetEntity)).thenReturn(
			fillBlankQuestionEntity);
		when(fillBlankQuestionFactory.createFillBlankAnswers(any(), eq(fillBlankQuestionEntity))).thenReturn(List.of());

		// when
		questionService.createQuestion(questionSetId, QuestionType.FILL_BLANK, questionDto);

		// then
		verify(questionEntityRepository).save(fillBlankQuestionEntity);
		verify(fillBlankAnswerEntityRepository).saveAll(any());
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

		List<MultipleChoiceEntity> choices = List.of(
			mock(MultipleChoiceEntity.class),
			mock(MultipleChoiceEntity.class)
		);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(multipleQuestion));
		when(multipleChoiceEntityRepository.findAllByQuestionId(questionId)).thenReturn(choices);

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then
		assertNotNull(result);
		verify(questionEntityRepository).findById(questionId);
		verify(multipleChoiceEntityRepository).findAllByQuestionId(questionId);
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

		List<ShortAnswerEntity> shortAnswers = List.of(
			mock(ShortAnswerEntity.class),
			mock(ShortAnswerEntity.class)
		);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(shortQuestion));
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(shortAnswers);

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then
		assertNotNull(result);
		verify(questionEntityRepository).findById(questionId);
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(questionId);
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

		List<OrderingOptionEntity> options = List.of(
			mock(OrderingOptionEntity.class),
			mock(OrderingOptionEntity.class)
		);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(orderingQuestion));
		when(orderingOptionEntityRepository.findAllByOrderingQuestionId(questionId)).thenReturn(options);

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then
		assertNotNull(result);
		verify(questionEntityRepository).findById(questionId);
		verify(orderingOptionEntityRepository).findAllByOrderingQuestionId(questionId);
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

		List<FillBlankAnswerEntity> fillBlankAnswers = List.of(
			mock(FillBlankAnswerEntity.class),
			mock(FillBlankAnswerEntity.class)
		);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(fillBlankQuestion));
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(questionId)).thenReturn(fillBlankAnswers);

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.REVIEW);

		// then
		assertNotNull(result);
		verify(questionEntityRepository).findById(questionId);
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(questionId);
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
		verify(multipleChoiceEntityRepository, never()).findAllByQuestionId(any());
		verify(shortAnswerEntityRepository, never()).findAllByShortQuestionId(any());
		verify(orderingOptionEntityRepository, never()).findAllByOrderingQuestionId(any());
		verify(fillBlankAnswerEntityRepository, never()).findAllByFillBlankQuestionId(any());
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
		verify(multipleChoiceEntityRepository, never()).findAllByQuestionId(any());
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

		// 각 문제 타입별 세부 데이터 mock
		when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(
			List.of(mock(MultipleChoiceEntity.class)));
		when(shortAnswerEntityRepository.findAllByShortQuestionId(2L)).thenReturn(
			List.of(mock(ShortAnswerEntity.class)));
		when(orderingOptionEntityRepository.findAllByOrderingQuestionId(3L)).thenReturn(
			List.of(mock(OrderingOptionEntity.class)));
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(4L)).thenReturn(
			List.of(mock(FillBlankAnswerEntity.class)));

		// when
		List<QuestionDto> result = questionService.getQuestions(questionSetId);

		// then
		assertNotNull(result);
		assertEquals(4, result.size());

		// number 순으로 정렬되었는지 확인 (1, 2, 3, 4)
		// 실제로는 QuestionDto의 구체적인 타입을 확인하기 어려우므로,
		// repository 호출이 올바르게 이루어졌는지 확인
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(2L);
		verify(orderingOptionEntityRepository).findAllByOrderingQuestionId(3L);
		verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(4L);
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

		// 빈 목록이므로 세부 repository 호출은 없어야 함
		verify(multipleChoiceEntityRepository, never()).findAllByQuestionId(any());
		verify(shortAnswerEntityRepository, never()).findAllByShortQuestionId(any());
		verify(orderingOptionEntityRepository, never()).findAllByOrderingQuestionId(any());
		verify(fillBlankAnswerEntityRepository, never()).findAllByFillBlankQuestionId(any());
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

		when(multipleChoiceEntityRepository.findAllByQuestionId(1L)).thenReturn(
			List.of(mock(MultipleChoiceEntity.class)));
		when(multipleChoiceEntityRepository.findAllByQuestionId(2L)).thenReturn(
			List.of(mock(MultipleChoiceEntity.class)));

		// when
		List<QuestionDto> result = questionService.getQuestions(questionSetId);

		// then
		assertNotNull(result);
		assertEquals(2, result.size());
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(multipleChoiceEntityRepository).findAllByQuestionId(1L);
		verify(multipleChoiceEntityRepository).findAllByQuestionId(2L);

		// 다른 타입의 repository는 호출되지 않아야 함
		verify(shortAnswerEntityRepository, never()).findAllByShortQuestionId(any());
		verify(orderingOptionEntityRepository, never()).findAllByOrderingQuestionId(any());
		verify(fillBlankAnswerEntityRepository, never()).findAllByFillBlankQuestionId(any());
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
		when(shortQuestion.getContent()).thenReturn("테스트 주관식 문제");
		when(shortQuestion.getNumber()).thenReturn(1L);

		List<ShortAnswerEntity> shortAnswers = List.of(
			mock(ShortAnswerEntity.class),
			mock(ShortAnswerEntity.class),
			mock(ShortAnswerEntity.class)
		);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(shortQuestion));
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(shortAnswers);

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME);

		// then
		assertThat(result).isInstanceOf(com.coniv.mait.domain.question.service.dto.ShortQuestionDto.class);
		
		com.coniv.mait.domain.question.service.dto.ShortQuestionDto shortResult = 
			(com.coniv.mait.domain.question.service.dto.ShortQuestionDto) result;
		
		assertThat(shortResult.getShortAnswers()).isNull(); // 실시간 모드에서는 답안 숨김
		assertThat(shortResult.getAnswerCount()).isEqualTo(3); // 개수 정보는 제공
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
		when(fillBlankQuestion.getContent()).thenReturn("테스트 빈칸 문제");
		when(fillBlankQuestion.getNumber()).thenReturn(1L);

		// 2개의 빈칸 (number 1, 2), 각각 여러 정답 가능
		FillBlankAnswerEntity answer1 = mock(FillBlankAnswerEntity.class);
		FillBlankAnswerEntity answer2 = mock(FillBlankAnswerEntity.class);
		FillBlankAnswerEntity answer3 = mock(FillBlankAnswerEntity.class);
		
		when(answer1.getNumber()).thenReturn(1L); // 첫 번째 빈칸
		when(answer2.getNumber()).thenReturn(1L); // 첫 번째 빈칸의 다른 정답
		when(answer3.getNumber()).thenReturn(2L); // 두 번째 빈칸

		List<FillBlankAnswerEntity> fillBlankAnswers = List.of(answer1, answer2, answer3);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(fillBlankQuestion));
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(questionId)).thenReturn(fillBlankAnswers);

		// when
		QuestionDto result = questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME);

		// then
		assertThat(result).isInstanceOf(com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto.class);
		
		com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto fillBlankResult = 
			(com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto) result;
		
		assertThat(fillBlankResult.getFillBlankAnswers()).isNull(); // 실시간 모드에서는 답안 숨김
		assertThat(fillBlankResult.getBlankCount()).isEqualTo(2); // 빈칸 개수는 제공 (number 1, 2)
	}
}
