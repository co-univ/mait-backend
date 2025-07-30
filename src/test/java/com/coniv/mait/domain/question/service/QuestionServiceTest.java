package com.coniv.mait.domain.question.service;

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

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.component.ShortQuestionFactory;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;

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

	@Test
	@DisplayName("객관식 문제 생성 테스트")
	void createMultipleChoiceQuestionTest() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));

		List<MultipleChoiceDto> choiceDtos = List.of(MultipleChoiceDto.builder().number(1).build(),
			MultipleChoiceDto.builder().number(2).build(),
			MultipleChoiceDto.builder().number(3).build());

		MultipleQuestionDto multipleQuestionDto = MultipleQuestionDto.builder()
			.choices(choiceDtos)
			.build();
		MultipleQuestionEntity multipleQuestionEntity = mock(MultipleQuestionEntity.class);
		when(multipleQuestionFactory.create(multipleQuestionDto, questionSetEntity))
			.thenReturn(multipleQuestionEntity);

		when(multipleQuestionFactory.createChoices(choiceDtos, multipleQuestionEntity))
			.thenReturn(List.of(mock(MultipleChoiceEntity.class), mock(MultipleChoiceEntity.class),
				mock(MultipleChoiceEntity.class)));

		// when
		questionService.createMultipleQuestion(questionSetId, multipleQuestionDto);

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

		List<MultipleChoiceDto> choiceDtos = List.of(MultipleChoiceDto.builder().number(1).build(),
			MultipleChoiceDto.builder().number(2).build(),
			MultipleChoiceDto.builder().number(3).build());

		MultipleQuestionDto multipleQuestionDto = MultipleQuestionDto.builder()
			.choices(choiceDtos)
			.build();

		// when, then
		assertThrows(EntityNotFoundException.class,
			() -> questionService.createMultipleQuestion(questionSetId, multipleQuestionDto));

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
			ShortAnswerDto.builder().number(2L).answer("정답2").isMain(false).build()
		);
		ShortQuestionDto shortQuestionDto = ShortQuestionDto.builder()
			.content("주관식 문제 내용")
			.explanation("해설")
			.number(1L)
			.shortAnswers(answerDtos)
			.build();
		ShortQuestionEntity shortQuestionEntity = mock(ShortQuestionEntity.class);
		List<ShortAnswerEntity> shortAnswerEntities = List.of(
			mock(ShortAnswerEntity.class), mock(ShortAnswerEntity.class)
		);

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
	@DisplayName("지원하지 않는 문제 타입 입력 시 예외 발생")
	void createQuestion_UnsupportedType() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSetEntity = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSetEntity));
		QuestionDto questionDto = mock(QuestionDto.class);

		// when, then
		assertThrows(IllegalArgumentException.class,
			() -> questionService.createQuestion(questionSetId, QuestionType.MULTIPLE, questionDto));
		verify(questionEntityRepository, never()).save(any());
		verify(shortAnswerEntityRepository, never()).saveAll(any());
	}
}
