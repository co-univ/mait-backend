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
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;

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
}
