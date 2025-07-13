package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

@ExtendWith(MockitoExtension.class)
class QuestionSetServiceTest {

	@InjectMocks
	private QuestionSetService questionSetService;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Test
	@DisplayName("문제 셋 생성 테스트")
	void createQuestionSetTest() {
		// given
		String subject = "Sample Subject";
		var creationType = QuestionSetCreationType.MANUAL;

		// when
		QuestionSetDto questionSetDto = questionSetService.createQuestionSet(subject, creationType);

		// then
		assertThat(questionSetDto).isNotNull();
		assertThat(questionSetDto.getSubject()).isEqualTo(subject);
		verify(questionSetEntityRepository).save(any());
	}
}
