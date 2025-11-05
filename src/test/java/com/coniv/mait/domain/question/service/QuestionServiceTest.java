package com.coniv.mait.domain.question.service;

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

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
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
import com.coniv.mait.domain.question.util.LexoRank;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
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

	@Mock
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Mock
	private QuestionImageService questionImageService;

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
			questionImageService);
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
		questionService.changeQuestionOrder(questionSetId, sourceQuestionId, null, nextQuestionId);

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
		questionService.changeQuestionOrder(questionSetId, sourceQuestionId, prevQuestionId, null);

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
		questionService.changeQuestionOrder(questionSetId, sourceQuestionId, prevQuestionId, nextQuestionId);

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
		questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto);

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
			() -> questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto));

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
		List<QuestionDto> result = questionService.getQuestions(questionSetId);

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
		when(question1.getType()).thenReturn(QuestionType.MULTIPLE);

		MultipleQuestionEntity question2 = mock(MultipleQuestionEntity.class);
		when(question2.getType()).thenReturn(QuestionType.MULTIPLE);

		List<QuestionEntity> questions = List.of(question1, question2);
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(questions);

		// factory 메서드 모킹
		MultipleQuestionDto dto1 = mock(MultipleQuestionDto.class);
		MultipleQuestionDto dto2 = mock(MultipleQuestionDto.class);
		when(multipleQuestionFactory.getQuestion(question1, true)).thenReturn(dto1);
		when(multipleQuestionFactory.getQuestion(question2, true)).thenReturn(dto2);

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

		// when - LIVE_TIME 모드 (답안 숨김)
		questionService.getQuestion(questionSetId, questionId, DeliveryMode.LIVE_TIME);

		// then - answerVisible = false로 호출되어야 함
		verify(shortQuestionFactory).getQuestion(shortQuestion, false);
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
		QuestionDto result = questionService.updateQuestion(questionSetId, questionId, questionDto);

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
		when(shortQuestionFactory.save(questionDto, questionSetEntity)).thenReturn(newQuestion);

		ShortQuestionDto expectedResult = mock(ShortQuestionDto.class);
		when(shortQuestionFactory.getQuestion(newQuestion, true)).thenReturn(expectedResult);

		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(existingQuestion));

		// when
		QuestionDto result = questionService.updateQuestion(questionSetId, questionId, questionDto);

		// then
		assertNotNull(result);
		assertEquals(expectedResult, result);

		// 다른 타입 수정 로직 검증
		verify(questionEntityRepository).findById(questionId);
		verify(multipleQuestionFactory).deleteSubEntities(existingQuestion); // 기존 타입의 팩토리
		verify(questionEntityRepository).delete(existingQuestion);
		verify(shortQuestionFactory).save(questionDto, questionSetEntity); // 새로운 타입의 팩토리
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
			() -> questionService.updateQuestion(questionSetId, questionId, questionDto));

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
			() -> questionService.updateQuestion(questionSetId, questionId, questionDto));

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
		questionService.deleteQuestion(questionSetId, questionId);

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
		when(expectedQuestionDto.getContent()).thenReturn(QuestionConstant.DEFAULT_QUESTION_CONTENT);

		when(multipleQuestionFactory.getQuestion(any(QuestionEntity.class), eq(true))).thenReturn(expectedQuestionDto);

		// when
		QuestionDto result = questionService.createDefaultQuestion(questionSetId);

		// then
		assertNotNull(result);
		assertEquals(QuestionType.MULTIPLE, result.getType());
		assertEquals(QuestionConstant.DEFAULT_QUESTION_CONTENT, result.getContent());

		QuestionEntity savedQuestion = questionCaptor.getValue();
		assertNotNull(savedQuestion);
		assertNotNull(savedQuestion.getLexoRank());
		assertEquals(QuestionConstant.DEFAULT_QUESTION_CONTENT, savedQuestion.getContent());
		assertEquals(questionSetEntity, savedQuestion.getQuestionSet());
		assertEquals(QuestionConstant.MAX_DISPLAY_DELAY_MILLISECONDS, savedQuestion.getDisplayDelayMilliseconds());

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionEntityRepository).save(any(QuestionEntity.class));
		verify(multipleQuestionFactory).getQuestion(any(QuestionEntity.class), eq(true));
	}
}
