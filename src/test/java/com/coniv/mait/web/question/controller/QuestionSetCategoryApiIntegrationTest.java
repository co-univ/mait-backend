package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.CreateQuestionSetCategoryApiRequest;

@WithCustomUser
public class QuestionSetCategoryApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private QuestionSetCategoryEntityRepository questionSetCategoryEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@BeforeEach
	void setUp() {
		teamUserEntityRepository.deleteAll();
		questionSetCategoryEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("카테고리 생성 API 성공 테스트 - MAKER 권한")
	void createCategoryApiSuccess() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("코니브").creatorId(1L).build());
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));

		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(team.getId(), "알고리즘");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.teamId").value(team.getId()),
				jsonPath("$.data.name").value("알고리즘"),
				jsonPath("$.data.id").exists());

		assertThat(questionSetCategoryEntityRepository.count()).isEqualTo(1);
	}
}
