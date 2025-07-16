package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiRequest;

public class QuestionSetApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@BeforeEach
	void setUp() {
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("문제 셋 생성 API 성공 테스트")
	void createQuestionSetApiSuccess() throws Exception {
		// given
		String subject = "Sample Subject";
		QuestionSetCreationType creationType = QuestionSetCreationType.MANUAL;

		CreateQuestionSetApiRequest request = new CreateQuestionSetApiRequest(subject, creationType);

		// when
		mockMvc.perform(post("/api/v1/question-sets").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.subject").value(subject));

		// then
		QuestionSetEntity questionSetEntity = questionSetEntityRepository.findAll().get(0);

		assertThat(questionSetEntityRepository.count()).isEqualTo(1);
		assertThat(questionSetEntity.getSubject()).isEqualTo(subject);
		assertThat(questionSetEntity.getCreationType()).isEqualTo(creationType);
	}
}
