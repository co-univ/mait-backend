package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.exception.QuestionSetCategoryException;
import com.coniv.mait.domain.question.exception.code.QuestionSetCategoryExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetCategoryDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionSetCategoryService 단위 테스트")
class QuestionSetCategoryServiceTest {

	@InjectMocks
	private QuestionSetCategoryService questionSetCategoryService;

	@Mock
	private QuestionSetCategoryEntityRepository questionSetCategoryEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Test
	@DisplayName("카테고리 생성 성공 - 동일 이름 카테고리 없음")
	void createCategory_success() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, name))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNotNull(teamId, name))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.save(any(QuestionSetCategoryEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		QuestionSetCategoryDto result = questionSetCategoryService.createCategory(teamId, name, userId);

		// then
		assertThat(result.getTeamId()).isEqualTo(teamId);
		assertThat(result.getName()).isEqualTo(name);

		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);
		verify(questionSetCategoryEntityRepository).save(any(QuestionSetCategoryEntity.class));
	}

	@Test
	@DisplayName("카테고리 생성 실패 - 동일 이름의 활성 카테고리가 이미 존재")
	void createCategory_fail_duplicateActiveName() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, name))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.createCategory(teamId, name, userId))
			.isInstanceOf(QuestionSetCategoryException.class)
			.extracting("exceptionCode")
			.isEqualTo(QuestionSetCategoryExceptionCode.DUPLICATE_NAME);

		verify(questionSetCategoryEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("카테고리 생성 실패 - 동일 이름의 삭제된 카테고리 존재 (복구 안내)")
	void createCategory_fail_duplicateDeletedName() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, name))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNotNull(teamId, name))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.createCategory(teamId, name, userId))
			.isInstanceOf(QuestionSetCategoryException.class)
			.extracting("exceptionCode")
			.isEqualTo(QuestionSetCategoryExceptionCode.DUPLICATE_NAME_DELETED);

		verify(questionSetCategoryEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("카테고리 생성 실패 - 권한 없음 (TeamRoleValidator 예외 전파)")
	void createCategory_fail_noPermission() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		doThrow(new RuntimeException("권한 없음"))
			.when(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.createCategory(teamId, name, userId))
			.isInstanceOf(RuntimeException.class);

		verify(questionSetCategoryEntityRepository, never())
			.existsByTeamIdAndNameAndDeletedAtIsNull(anyLong(), anyString());
		verify(questionSetCategoryEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("카테고리 목록 조회 성공 - 활성 카테고리만 DTO 변환되어 반환")
	void getCategories_success() {
		// given
		Long teamId = 1L;
		Long userId = 10L;

		QuestionSetCategoryEntity first = QuestionSetCategoryEntity.of(teamId, "알고리즘");
		QuestionSetCategoryEntity second = QuestionSetCategoryEntity.of(teamId, "자료구조");
		when(questionSetCategoryEntityRepository.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(teamId))
			.thenReturn(List.of(first, second));

		// when
		List<QuestionSetCategoryDto> result = questionSetCategoryService.getCategories(teamId, userId);

		// then
		assertThat(result).hasSize(2);
		assertThat(result).extracting(QuestionSetCategoryDto::getName)
			.containsExactly("알고리즘", "자료구조");
		assertThat(result).allSatisfy(dto -> assertThat(dto.getTeamId()).isEqualTo(teamId));

		verify(teamRoleValidator).checkIsTeamMember(teamId, userId);
	}

	@Test
	@DisplayName("카테고리 목록 조회 실패 - 팀 멤버가 아님 (TeamRoleValidator 예외 전파)")
	void getCategories_fail_notTeamMember() {
		// given
		Long teamId = 1L;
		Long userId = 10L;

		doThrow(new RuntimeException("팀 멤버가 아님"))
			.when(teamRoleValidator).checkIsTeamMember(teamId, userId);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.getCategories(teamId, userId))
			.isInstanceOf(RuntimeException.class);

		verify(questionSetCategoryEntityRepository, never())
			.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(anyLong());
	}

	@Test
	@DisplayName("카테고리 삭제 성공 - 활성 카테고리는 markDeleted 호출")
	void deleteCategory_success() {
		// given
		Long categoryId = 100L;
		Long teamId = 1L;
		Long userId = 10L;

		QuestionSetCategoryEntity category = QuestionSetCategoryEntity.of(teamId, "알고리즘");
		when(questionSetCategoryEntityRepository.findById(categoryId)).thenReturn(Optional.of(category));

		// when
		questionSetCategoryService.deleteCategory(categoryId, userId);

		// then
		assertThat(category.deleted()).isTrue();
		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);
	}

	@Test
	@DisplayName("카테고리 삭제 성공 - 이미 삭제된 카테고리는 멱등 처리 (no-op)")
	void deleteCategory_success_alreadyDeletedIdempotent() {
		// given
		Long categoryId = 100L;
		Long teamId = 1L;
		Long userId = 10L;

		QuestionSetCategoryEntity category = QuestionSetCategoryEntity.of(teamId, "알고리즘");
		category.markDeleted();
		java.time.LocalDateTime originalDeletedAt = category.getDeletedAt();
		when(questionSetCategoryEntityRepository.findById(categoryId)).thenReturn(Optional.of(category));

		// when
		questionSetCategoryService.deleteCategory(categoryId, userId);

		// then
		assertThat(category.getDeletedAt()).isEqualTo(originalDeletedAt);
		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);
	}

	@Test
	@DisplayName("카테고리 삭제 실패 - 카테고리가 존재하지 않음")
	void deleteCategory_fail_notFound() {
		// given
		Long categoryId = 100L;
		Long userId = 10L;

		when(questionSetCategoryEntityRepository.findById(categoryId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.deleteCategory(categoryId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("해당 카테고리를 찾을 수 없습니다.");

		verify(teamRoleValidator, never()).checkHasCreateQuestionSetAuthority(anyLong(), anyLong());
	}

	@Test
	@DisplayName("카테고리 삭제 실패 - 권한 없음 (TeamRoleValidator 예외 전파)")
	void deleteCategory_fail_noPermission() {
		// given
		Long categoryId = 100L;
		Long teamId = 1L;
		Long userId = 10L;

		QuestionSetCategoryEntity category = QuestionSetCategoryEntity.of(teamId, "알고리즘");
		when(questionSetCategoryEntityRepository.findById(categoryId)).thenReturn(Optional.of(category));
		doThrow(new RuntimeException("권한 없음"))
			.when(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.deleteCategory(categoryId, userId))
			.isInstanceOf(RuntimeException.class);

		assertThat(category.deleted()).isFalse();
	}
}
