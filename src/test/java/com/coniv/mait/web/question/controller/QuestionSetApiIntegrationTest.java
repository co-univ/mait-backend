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
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiRequest;

public class QuestionSetApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

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

	@Test
	@DisplayName("문제 셋 목록 조회 API 성공 테스트")
	void getQuestionSetsApiSuccess() throws Exception {
		// given
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("코니브").build());
		String subject1 = "Subject 1";
		String subject2 = "Subject 2";

		QuestionSetEntity questionSet1 = QuestionSetEntity.builder().subject(subject1).teamId(team.getId()).build();
		QuestionSetEntity questionSet2 = QuestionSetEntity.builder().subject(subject2).teamId(team.getId()).build();

		questionSetEntityRepository.save(questionSet1);
		questionSetEntityRepository.save(questionSet2);

		// when
		mockMvc.perform(get("/api/v1/question-sets")
				.param("teamId", String.valueOf(team.getId()))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].subject").value(subject2)) // 최신 것이 먼저
			.andExpect(jsonPath("$.data[1].subject").value(subject1));

		// then
		assertThat(questionSetEntityRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("문제 셋 단건 조회 API 성공 테스트")
	void getQuestionSetApiSuccess() throws Exception {
		// given
		String subject = "Sample Subject";
		QuestionSetEntity questionSet = questionSetEntityRepository.save(QuestionSetEntity.builder().subject(subject).build());

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}", questionSet.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(questionSet.getId()))
			.andExpect(jsonPath("$.data.subject").value(subject));
	}
}
