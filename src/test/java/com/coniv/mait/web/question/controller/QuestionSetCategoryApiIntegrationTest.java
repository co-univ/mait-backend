package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.exception.code.QuestionSetCategoryExceptionCode;
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

	@Test
	@DisplayName("카테고리 생성 API 실패 - 동일 이름 활성 카테고리 존재 (409)")
	void createCategoryApiFail_duplicateActive() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("코니브").creatorId(1L).build());
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));
		questionSetCategoryEntityRepository.save(QuestionSetCategoryEntity.of(team.getId(), "알고리즘"));

		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(team.getId(), "알고리즘");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isConflict(),
				jsonPath("$.code").value(QuestionSetCategoryExceptionCode.DUPLICATE_NAME.getCode()));
	}

	@Test
	@DisplayName("카테고리 생성 API 실패 - 동일 이름 삭제된 카테고리 존재 (409, 복구 안내)")
	void createCategoryApiFail_duplicateDeleted() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("코니브").creatorId(1L).build());
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));

		QuestionSetCategoryEntity deleted = QuestionSetCategoryEntity.of(team.getId(), "알고리즘");
		deleted.updateDeletedAt(LocalDateTime.now());
		questionSetCategoryEntityRepository.save(deleted);

		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(team.getId(), "알고리즘");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isConflict(),
				jsonPath("$.code").value(QuestionSetCategoryExceptionCode.DUPLICATE_NAME_DELETED.getCode()));
	}

	@Test
	@DisplayName("카테고리 생성 API 실패 - 팀 멤버가 아닌 사용자 (403)")
	void createCategoryApiFail_notTeamMember() throws Exception {
		// given
		userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("코니브").creatorId(1L).build());

		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(team.getId(), "알고리즘");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().is4xxClientError());
	}

	@Test
	@DisplayName("카테고리 생성 API 실패 - 이름 40자 초과 (400)")
	void createCategoryApiFail_nameTooLong() throws Exception {
		// given
		String longName = "가".repeat(41);
		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(1L, longName);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("카테고리 생성 API 실패 - 이름 공백 (400)")
	void createCategoryApiFail_blankName() throws Exception {
		// given
		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(1L, "   ");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}
}
