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
import com.coniv.mait.web.question.dto.RestoreQuestionSetCategoryApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionSetCategoryApiRequest;

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
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("코니브", 1L));
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
	@DisplayName("카테고리 이름 수정 API 성공 테스트 - 카테고리 ID 유지, 이름만 변경")
	void updateCategoryNameApiSuccess() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("코니브", 1L));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));

		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "알고리즘"));
		UpdateQuestionSetCategoryApiRequest request = new UpdateQuestionSetCategoryApiRequest("자료구조");

		// when & then
		mockMvc.perform(patch("/api/v1/question-sets/categories/{categoryId}", category.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.id").value(category.getId()),
				jsonPath("$.data.teamId").value(team.getId()),
				jsonPath("$.data.name").value("자료구조"));

		QuestionSetCategoryEntity updated = questionSetCategoryEntityRepository.findById(category.getId())
			.orElseThrow();
		assertThat(updated.getName()).isEqualTo("자료구조");
	}

	@Test
	@DisplayName("카테고리 이름 수정 API 실패 테스트 - 활성 카테고리 이름 중복")
	void updateCategoryNameApiFailDuplicateActiveName() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("코니브", 1L));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));

		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "알고리즘"));
		questionSetCategoryEntityRepository.save(QuestionSetCategoryEntity.of(team.getId(), "자료구조"));
		UpdateQuestionSetCategoryApiRequest request = new UpdateQuestionSetCategoryApiRequest("자료구조");

		// when & then
		mockMvc.perform(patch("/api/v1/question-sets/categories/{categoryId}", category.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isConflict(),
				jsonPath("$.code").value("QSC-001"));
	}

	@Test
	@DisplayName("카테고리 이름 수정 API 실패 테스트 - 삭제된 카테고리 이름 중복")
	void updateCategoryNameApiFailDuplicateDeletedName() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("코니브", 1L));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));

		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "알고리즘"));
		QuestionSetCategoryEntity deleted = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "자료구조"));
		deleted.updateDeletedAt(LocalDateTime.now());
		questionSetCategoryEntityRepository.save(deleted);
		UpdateQuestionSetCategoryApiRequest request = new UpdateQuestionSetCategoryApiRequest("자료구조");

		// when & then
		mockMvc.perform(patch("/api/v1/question-sets/categories/{categoryId}", category.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isConflict(),
				jsonPath("$.code").value("QSC-002"));
	}

	@Test
	@DisplayName("카테고리 삭제 API 성공 테스트 - MAKER 권한, soft delete 반영")
	void deleteCategoryApiSuccess() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("코니브", 1L));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));

		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "알고리즘"));

		// when & then
		mockMvc.perform(delete("/api/v1/question-sets/categories/{categoryId}", category.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true));

		QuestionSetCategoryEntity updated = questionSetCategoryEntityRepository.findById(category.getId())
			.orElseThrow();
		assertThat(updated.deleted()).isTrue();
		assertThat(updated.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("카테고리 복구 API 성공 테스트 - MAKER 권한, (teamId, name) 식별 row 의 deletedAt 이 null 로 반영")
	void restoreCategoryApiSuccess() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("코니브", 1L));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));

		QuestionSetCategoryEntity category = QuestionSetCategoryEntity.of(team.getId(), "알고리즘");
		category.updateDeletedAt(LocalDateTime.now());
		QuestionSetCategoryEntity saved = questionSetCategoryEntityRepository.save(category);

		RestoreQuestionSetCategoryApiRequest request = new RestoreQuestionSetCategoryApiRequest(
			team.getId(), "알고리즘");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories/restore")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.id").value(saved.getId()),
				jsonPath("$.data.teamId").value(team.getId()),
				jsonPath("$.data.name").value("알고리즘"));

		QuestionSetCategoryEntity updated = questionSetCategoryEntityRepository.findById(saved.getId())
			.orElseThrow();
		assertThat(updated.deleted()).isFalse();
		assertThat(updated.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("카테고리 목록 조회 API 성공 테스트 - 활성 카테고리만 추가순으로 반환")
	void getCategoriesApiSuccess() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("코니브", 1L));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.PLAYER));

		QuestionSetCategoryEntity first = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "알고리즘"));
		QuestionSetCategoryEntity second = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "자료구조"));
		QuestionSetCategoryEntity deleted = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "삭제된카테고리"));
		deleted.updateDeletedAt(LocalDateTime.now());
		questionSetCategoryEntityRepository.save(deleted);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/categories")
				.param("teamId", String.valueOf(team.getId())))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.length()").value(2),
				jsonPath("$.data[0].id").value(first.getId()),
				jsonPath("$.data[0].teamId").value(team.getId()),
				jsonPath("$.data[0].name").value("알고리즘"),
				jsonPath("$.data[1].id").value(second.getId()),
				jsonPath("$.data[1].name").value("자료구조"));
	}

	@Test
	@DisplayName("카테고리 검색 API 성공 테스트 - 팀의 활성 카테고리 중 이름 부분 일치만 반환")
	void searchCategoriesApiSuccess() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("코니브", 1L));
		TeamEntity otherTeam = teamEntityRepository.save(TeamEntity.ofGroup("다른팀", 2L));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.PLAYER));

		QuestionSetCategoryEntity first = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "알고리즘"));
		QuestionSetCategoryEntity second = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "고급 알고리즘"));
		questionSetCategoryEntityRepository.save(QuestionSetCategoryEntity.of(team.getId(), "자료구조"));
		questionSetCategoryEntityRepository.save(QuestionSetCategoryEntity.of(otherTeam.getId(), "알고리즘"));
		QuestionSetCategoryEntity deleted = questionSetCategoryEntityRepository.save(
			QuestionSetCategoryEntity.of(team.getId(), "알고리즘 삭제됨"));
		deleted.updateDeletedAt(LocalDateTime.now());
		questionSetCategoryEntityRepository.save(deleted);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/categories/search")
				.param("teamId", String.valueOf(team.getId()))
				.param("keyword", "알고"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.length()").value(2),
				jsonPath("$.data[0].id").value(first.getId()),
				jsonPath("$.data[0].teamId").value(team.getId()),
				jsonPath("$.data[0].name").value("알고리즘"),
				jsonPath("$.data[1].id").value(second.getId()),
				jsonPath("$.data[1].name").value("고급 알고리즘"));
	}
}
