package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.exception.QuestionSetCategoryException;
import com.coniv.mait.domain.question.exception.code.QuestionSetCategoryExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
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
	private QuestionSetCategoryLinkEntityRepository questionSetCategoryLinkEntityRepository;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

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
		when(questionSetCategoryEntityRepository.saveAndFlush(any(QuestionSetCategoryEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		QuestionSetCategoryDto result = questionSetCategoryService.createCategory(teamId, name, userId);

		// then
		assertThat(result.getTeamId()).isEqualTo(teamId);
		assertThat(result.getName()).isEqualTo(name);

		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);
		verify(questionSetCategoryEntityRepository).saveAndFlush(any(QuestionSetCategoryEntity.class));
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

		verify(questionSetCategoryEntityRepository, never()).saveAndFlush(any());
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

		verify(questionSetCategoryEntityRepository, never()).saveAndFlush(any());
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
		verify(questionSetCategoryEntityRepository, never()).saveAndFlush(any());
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
	@DisplayName("카테고리 검색 성공 - 검색어를 포함한 활성 카테고리만 반환")
	void searchCategories_success() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String keyword = " 고리 ";

		QuestionSetCategoryEntity category = QuestionSetCategoryEntity.of(teamId, "알고리즘");
		when(questionSetCategoryEntityRepository
			.findAllByTeamIdAndNameContainingAndDeletedAtIsNullOrderByCreatedAtAsc(teamId, "고리"))
			.thenReturn(List.of(category));

		// when
		List<QuestionSetCategoryDto> result = questionSetCategoryService.searchCategories(teamId, userId, keyword);

		// then
		assertThat(result).hasSize(1);
		assertThat(result).extracting(QuestionSetCategoryDto::getName)
			.containsExactly("알고리즘");

		verify(teamRoleValidator).checkIsTeamMember(teamId, userId);
	}

	@Test
	@DisplayName("카테고리 검색 성공 - 검색어가 공백이면 팀의 전체 활성 카테고리 반환")
	void searchCategories_blankKeyword_returnsAllActiveCategories() {
		// given
		Long teamId = 1L;
		Long userId = 10L;

		QuestionSetCategoryEntity first = QuestionSetCategoryEntity.of(teamId, "알고리즘");
		QuestionSetCategoryEntity second = QuestionSetCategoryEntity.of(teamId, "자료구조");
		when(questionSetCategoryEntityRepository.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(teamId))
			.thenReturn(List.of(first, second));

		// when
		List<QuestionSetCategoryDto> result = questionSetCategoryService.searchCategories(teamId, userId, "   ");

		// then
		assertThat(result).extracting(QuestionSetCategoryDto::getName)
			.containsExactly("알고리즘", "자료구조");

		verify(teamRoleValidator).checkIsTeamMember(teamId, userId);
		verify(questionSetCategoryEntityRepository, never())
			.findAllByTeamIdAndNameContainingAndDeletedAtIsNullOrderByCreatedAtAsc(anyLong(), anyString());
	}

	@Test
	@DisplayName("카테고리 검색 성공 - 검색어가 null 이면 팀의 전체 활성 카테고리 반환")
	void searchCategories_nullKeyword_returnsAllActiveCategories() {
		// given
		Long teamId = 1L;
		Long userId = 10L;

		QuestionSetCategoryEntity category = QuestionSetCategoryEntity.of(teamId, "알고리즘");
		when(questionSetCategoryEntityRepository.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(teamId))
			.thenReturn(List.of(category));

		// when
		List<QuestionSetCategoryDto> result = questionSetCategoryService.searchCategories(teamId, userId, null);

		// then
		assertThat(result).extracting(QuestionSetCategoryDto::getName)
			.containsExactly("알고리즘");

		verify(teamRoleValidator).checkIsTeamMember(teamId, userId);
		verify(questionSetCategoryEntityRepository, never())
			.findAllByTeamIdAndNameContainingAndDeletedAtIsNullOrderByCreatedAtAsc(anyLong(), anyString());
	}

	@Test
	@DisplayName("카테고리 검색 실패 - 팀 멤버가 아님 (TeamRoleValidator 예외 전파)")
	void searchCategories_fail_notTeamMember() {
		// given
		Long teamId = 1L;
		Long userId = 10L;

		doThrow(new RuntimeException("팀 멤버가 아님"))
			.when(teamRoleValidator).checkIsTeamMember(teamId, userId);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.searchCategories(teamId, userId, "알고"))
			.isInstanceOf(RuntimeException.class);

		verify(questionSetCategoryEntityRepository, never())
			.findAllByTeamIdAndNameContainingAndDeletedAtIsNullOrderByCreatedAtAsc(anyLong(), anyString());
	}

	@Test
	@DisplayName("카테고리 이름 수정 성공 - 활성 카테고리 이름만 변경")
	void updateCategoryName_success() {
		// given
		Long categoryId = 100L;
		Long teamId = 1L;
		Long userId = 10L;
		String newName = "자료구조";

		QuestionSetCategoryEntity category = category(categoryId, teamId, "알고리즘");
		when(questionSetCategoryEntityRepository.findByIdAndDeletedAtIsNull(categoryId))
			.thenReturn(Optional.of(category));
		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, newName))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNotNull(teamId, newName))
			.thenReturn(false);

		// when
		QuestionSetCategoryDto result = questionSetCategoryService.updateCategoryName(categoryId, newName, userId);

		// then
		assertThat(category.getName()).isEqualTo(newName);
		assertThat(result.getId()).isEqualTo(categoryId);
		assertThat(result.getTeamId()).isEqualTo(teamId);
		assertThat(result.getName()).isEqualTo(newName);

		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);
		verify(questionSetCategoryEntityRepository).saveAndFlush(category);
	}

	@Test
	@DisplayName("카테고리 이름 수정 성공 - 기존 이름과 같으면 멱등 처리")
	void updateCategoryName_sameName_idempotent() {
		// given
		Long categoryId = 100L;
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		QuestionSetCategoryEntity category = category(categoryId, teamId, name);
		when(questionSetCategoryEntityRepository.findByIdAndDeletedAtIsNull(categoryId))
			.thenReturn(Optional.of(category));

		// when
		QuestionSetCategoryDto result = questionSetCategoryService.updateCategoryName(categoryId, name, userId);

		// then
		assertThat(result.getName()).isEqualTo(name);
		verify(questionSetCategoryEntityRepository, never())
			.existsByTeamIdAndNameAndDeletedAtIsNull(anyLong(), anyString());
		verify(questionSetCategoryEntityRepository, never())
			.existsByTeamIdAndNameAndDeletedAtIsNotNull(anyLong(), anyString());
		verify(questionSetCategoryEntityRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("카테고리 이름 수정 실패 - 동일 이름의 활성 카테고리가 이미 존재")
	void updateCategoryName_fail_duplicateActiveName() {
		// given
		Long categoryId = 100L;
		Long teamId = 1L;
		Long userId = 10L;
		String newName = "자료구조";

		QuestionSetCategoryEntity category = category(categoryId, teamId, "알고리즘");
		when(questionSetCategoryEntityRepository.findByIdAndDeletedAtIsNull(categoryId))
			.thenReturn(Optional.of(category));
		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, newName))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.updateCategoryName(categoryId, newName, userId))
			.isInstanceOf(QuestionSetCategoryException.class)
			.extracting("exceptionCode")
			.isEqualTo(QuestionSetCategoryExceptionCode.DUPLICATE_NAME);

		assertThat(category.getName()).isEqualTo("알고리즘");
		verify(questionSetCategoryEntityRepository, never())
			.existsByTeamIdAndNameAndDeletedAtIsNotNull(anyLong(), anyString());
		verify(questionSetCategoryEntityRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("카테고리 이름 수정 실패 - 동일 이름의 삭제된 카테고리 존재")
	void updateCategoryName_fail_duplicateDeletedName() {
		// given
		Long categoryId = 100L;
		Long teamId = 1L;
		Long userId = 10L;
		String newName = "자료구조";

		QuestionSetCategoryEntity category = category(categoryId, teamId, "알고리즘");
		when(questionSetCategoryEntityRepository.findByIdAndDeletedAtIsNull(categoryId))
			.thenReturn(Optional.of(category));
		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, newName))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNotNull(teamId, newName))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.updateCategoryName(categoryId, newName, userId))
			.isInstanceOf(QuestionSetCategoryException.class)
			.extracting("exceptionCode")
			.isEqualTo(QuestionSetCategoryExceptionCode.DUPLICATE_NAME_DELETED);

		assertThat(category.getName()).isEqualTo("알고리즘");
		verify(questionSetCategoryEntityRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("카테고리 이름 수정 실패 - 삭제된 카테고리는 수정 불가")
	void updateCategoryName_fail_deletedCategory() {
		// given
		Long categoryId = 100L;
		Long teamId = 1L;
		Long userId = 10L;

		when(questionSetCategoryEntityRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.updateCategoryName(categoryId, "자료구조", userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("해당 카테고리를 찾을 수 없습니다.");

		verify(teamRoleValidator, never()).checkHasCreateQuestionSetAuthority(anyLong(), anyLong());
		verify(questionSetCategoryEntityRepository, never())
			.existsByTeamIdAndNameAndDeletedAtIsNull(anyLong(), anyString());
		verify(questionSetCategoryEntityRepository, never())
			.existsByTeamIdAndNameAndDeletedAtIsNotNull(anyLong(), anyString());
		verify(questionSetCategoryEntityRepository, never()).saveAndFlush(any());
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

	@Test
	@DisplayName("카테고리 복구 성공 - 삭제된 카테고리의 deletedAt 이 null 로 변경됨")
	void restoreCategory_success() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		QuestionSetCategoryEntity category = QuestionSetCategoryEntity.of(teamId, name);
		category.markDeleted();
		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, name))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.findByTeamIdAndNameAndDeletedAtIsNotNull(teamId, name))
			.thenReturn(Optional.of(category));

		// when
		QuestionSetCategoryDto result = questionSetCategoryService.restoreCategory(teamId, name, userId);

		// then
		assertThat(category.deleted()).isFalse();
		assertThat(result.getTeamId()).isEqualTo(teamId);
		assertThat(result.getName()).isEqualTo(name);

		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);
	}

	@Test
	@DisplayName("카테고리 복구 실패 - 권한 없음 (TeamRoleValidator 예외 전파)")
	void restoreCategory_fail_noPermission() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		doThrow(new RuntimeException("권한 없음"))
			.when(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.restoreCategory(teamId, name, userId))
			.isInstanceOf(RuntimeException.class);

		verify(questionSetCategoryEntityRepository, never())
			.existsByTeamIdAndNameAndDeletedAtIsNull(anyLong(), anyString());
		verify(questionSetCategoryEntityRepository, never())
			.findByTeamIdAndNameAndDeletedAtIsNotNull(anyLong(), anyString());
	}

	@Test
	@DisplayName("카테고리 복구 실패 - 동일 이름의 활성 카테고리가 이미 존재")
	void restoreCategory_fail_alreadyActive() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, name))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.restoreCategory(teamId, name, userId))
			.isInstanceOf(QuestionSetCategoryException.class)
			.extracting("exceptionCode")
			.isEqualTo(QuestionSetCategoryExceptionCode.ALREADY_ACTIVE);

		verify(questionSetCategoryEntityRepository, never())
			.findByTeamIdAndNameAndDeletedAtIsNotNull(anyLong(), anyString());
	}

	@Test
	@DisplayName("카테고리 복구 실패 - 복구할 카테고리가 존재하지 않음")
	void restoreCategory_fail_notFound() {
		// given
		Long teamId = 1L;
		Long userId = 10L;
		String name = "알고리즘";

		when(questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, name))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.findByTeamIdAndNameAndDeletedAtIsNotNull(teamId, name))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.restoreCategory(teamId, name, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("복구할 카테고리를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("detachCategory - 매핑 row 가 있으면 삭제")
	void detachCategory_existingMapping_deletes() {
		// given
		Long questionSetId = 1L;
		Long categoryId = 11L;
		Long teamId = 100L;
		Long userId = 10L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getTeamId()).thenReturn(teamId);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));

		// when
		questionSetCategoryService.detachCategory(questionSetId, categoryId, userId);

		// then
		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);
		verify(questionSetCategoryLinkEntityRepository)
			.deleteByQuestionSetIdAndCategoryId(questionSetId, categoryId);
	}

	@Test
	@DisplayName("detachCategory - 문제 셋이 존재하지 않으면 예외")
	void detachCategory_questionSetNotFound_throws() {
		// given
		Long questionSetId = 999L;
		Long categoryId = 11L;
		Long userId = 10L;

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.detachCategory(questionSetId, categoryId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("문제 셋을 찾을 수 없습니다.");

		verify(questionSetCategoryLinkEntityRepository, never())
			.deleteByQuestionSetIdAndCategoryId(anyLong(), anyLong());
	}

	@Test
	@DisplayName("attachCategories - 모든 categoryId가 같은 팀의 활성 카테고리면 일괄 INSERT")
	void attachCategories_savesAll() {
		// given
		Long questionSetId = 1L;
		Long teamId = 100L;
		List<Long> incoming = List.of(11L, 12L);

		QuestionSetCategoryEntity category1 = mock(QuestionSetCategoryEntity.class);
		QuestionSetCategoryEntity category2 = mock(QuestionSetCategoryEntity.class);

		when(questionSetCategoryEntityRepository.findAllByIdInAndTeamIdAndDeletedAtIsNull(anySet(), eq(teamId)))
			.thenReturn(List.of(category1, category2));

		// when
		questionSetCategoryService.attachCategories(questionSetId, teamId, incoming);

		// then
		verify(questionSetCategoryLinkEntityRepository, times(1)).saveAll(anyList());
	}

	@Test
	@DisplayName("attachCategories - 다른 팀 또는 미존재(또는 삭제된) 카테고리 포함 시 예외")
	void attachCategories_invalidCategory_throws() {
		// given
		Long questionSetId = 1L;
		Long teamId = 100L;
		List<Long> incoming = List.of(11L, 12L);

		QuestionSetCategoryEntity onlyOne = mock(QuestionSetCategoryEntity.class);

		when(questionSetCategoryEntityRepository.findAllByIdInAndTeamIdAndDeletedAtIsNull(anySet(), eq(teamId)))
			.thenReturn(List.of(onlyOne));

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.attachCategories(questionSetId, teamId, incoming))
			.isInstanceOf(QuestionSetCategoryException.class)
			.hasMessage(QuestionSetCategoryExceptionCode.INVALID_TEAM_OR_NOT_FOUND.getMessage());

		verify(questionSetCategoryLinkEntityRepository, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("attachCategory - 활성 카테고리 단건 매핑 성공")
	void attachCategory_success() {
		// given
		Long questionSetId = 1L;
		Long categoryId = 11L;
		Long teamId = 100L;
		Long userId = 10L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getTeamId()).thenReturn(teamId);
		QuestionSetCategoryEntity category = mock(QuestionSetCategoryEntity.class);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSetCategoryLinkEntityRepository.existsByQuestionSetIdAndCategoryId(questionSetId, categoryId))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.findByIdAndTeamIdAndDeletedAtIsNull(categoryId, teamId))
			.thenReturn(Optional.of(category));

		// when
		questionSetCategoryService.attachCategory(questionSetId, categoryId, userId);

		// then
		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(teamId, userId);
		verify(questionSetCategoryLinkEntityRepository).save(any(QuestionSetCategoryLinkEntity.class));
	}

	@Test
	@DisplayName("attachCategory - 이미 매핑된 카테고리는 멱등 처리 (save 호출 안 됨)")
	void attachCategory_alreadyMapped_idempotent() {
		// given
		Long questionSetId = 1L;
		Long categoryId = 11L;
		Long teamId = 100L;
		Long userId = 10L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getTeamId()).thenReturn(teamId);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSetCategoryLinkEntityRepository.existsByQuestionSetIdAndCategoryId(questionSetId, categoryId))
			.thenReturn(true);

		// when
		questionSetCategoryService.attachCategory(questionSetId, categoryId, userId);

		// then
		verify(questionSetCategoryEntityRepository, never())
			.findByIdAndTeamIdAndDeletedAtIsNull(anyLong(), anyLong());
		verify(questionSetCategoryLinkEntityRepository, never()).save(any(QuestionSetCategoryLinkEntity.class));
	}

	@Test
	@DisplayName("attachCategory - 문제 셋이 존재하지 않으면 예외")
	void attachCategory_questionSetNotFound_throws() {
		// given
		Long questionSetId = 999L;
		Long categoryId = 11L;
		Long userId = 10L;

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.attachCategory(questionSetId, categoryId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("문제 셋을 찾을 수 없습니다.");

		verify(questionSetCategoryLinkEntityRepository, never()).save(any(QuestionSetCategoryLinkEntity.class));
	}

	@Test
	@DisplayName("attachCategory - 다른 팀 또는 삭제된 카테고리는 EntityNotFoundException")
	void attachCategory_categoryNotFoundOrOtherTeamOrDeleted_throws() {
		// given
		Long questionSetId = 1L;
		Long categoryId = 11L;
		Long teamId = 100L;
		Long userId = 10L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getTeamId()).thenReturn(teamId);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSetCategoryLinkEntityRepository.existsByQuestionSetIdAndCategoryId(questionSetId, categoryId))
			.thenReturn(false);
		when(questionSetCategoryEntityRepository.findByIdAndTeamIdAndDeletedAtIsNull(categoryId, teamId))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetCategoryService.attachCategory(questionSetId, categoryId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("해당 카테고리를 찾을 수 없습니다.");

		verify(questionSetCategoryLinkEntityRepository, never()).save(any(QuestionSetCategoryLinkEntity.class));
	}

	@Test
	@DisplayName("updateLinkedCategories - 기존 매핑과 요청을 비교해 추가/제거를 적용")
	void updateLinkedCategories_addsAndRemoves() {
		// given
		Long questionSetId = 1L;
		Long teamId = 100L;
		Long keepId = 11L;
		Long removeId = 12L;
		Long addId = 13L;

		QuestionSetCategoryLinkEntity keepLink = QuestionSetCategoryLinkEntity.of(questionSetId, keepId);
		QuestionSetCategoryLinkEntity removeLink = QuestionSetCategoryLinkEntity.of(questionSetId, removeId);

		when(questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(keepLink, removeLink));

		QuestionSetCategoryEntity newCategory = mock(QuestionSetCategoryEntity.class);
		when(questionSetCategoryEntityRepository.findAllByIdInAndTeamIdAndDeletedAtIsNull(eq(Set.of(addId)),
			eq(teamId))).thenReturn(List.of(newCategory));

		// when
		questionSetCategoryService.updateLinkedCategories(questionSetId, teamId, List.of(keepId, addId));

		// then
		verify(questionSetCategoryLinkEntityRepository).saveAll(anyList());
		verify(questionSetCategoryLinkEntityRepository).deleteByQuestionSetIdAndCategoryIdIn(questionSetId,
			Set.of(removeId));
	}

	@Test
	@DisplayName("updateLinkedCategories - 요청과 기존이 동일하면 실제 추가/제거 대상이 없음")
	void updateLinkedCategories_noChange_idempotent() {
		// given
		Long questionSetId = 1L;
		Long teamId = 100L;
		Long categoryId = 11L;

		QuestionSetCategoryLinkEntity existingLink = QuestionSetCategoryLinkEntity.of(questionSetId, categoryId);
		when(questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(existingLink));

		// when
		questionSetCategoryService.updateLinkedCategories(questionSetId, teamId, List.of(categoryId));

		// then
		verify(questionSetCategoryLinkEntityRepository, never()).saveAll(anyList());
		verify(questionSetCategoryLinkEntityRepository)
			.deleteByQuestionSetIdAndCategoryIdIn(eq(questionSetId), argThat(c -> c.isEmpty()));
		verify(questionSetCategoryEntityRepository, never())
			.findAllByIdInAndTeamIdAndDeletedAtIsNull(anySet(), anyLong());
	}

	@Test
	@DisplayName("updateLinkedCategories - 빈 리스트면 기존 매핑을 모두 제거")
	void updateLinkedCategories_emptyList_removesAll() {
		// given
		Long questionSetId = 1L;
		Long teamId = 100L;

		QuestionSetCategoryLinkEntity link1 = QuestionSetCategoryLinkEntity.of(questionSetId, 11L);
		QuestionSetCategoryLinkEntity link2 = QuestionSetCategoryLinkEntity.of(questionSetId, 12L);
		when(questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(link1, link2));

		// when
		questionSetCategoryService.updateLinkedCategories(questionSetId, teamId, List.of());

		// then
		verify(questionSetCategoryLinkEntityRepository).deleteByQuestionSetIdAndCategoryIdIn(questionSetId,
			Set.of(11L, 12L));
		verify(questionSetCategoryLinkEntityRepository, never()).saveAll(anyList());
		verify(questionSetCategoryEntityRepository, never())
			.findAllByIdInAndTeamIdAndDeletedAtIsNull(anySet(), anyLong());
	}

	@Test
	@DisplayName("updateLinkedCategories - 추가 대상 중 다른 팀 또는 미존재 카테고리가 있으면 예외")
	void updateLinkedCategories_invalidNewCategory_throws() {
		// given
		Long questionSetId = 1L;
		Long teamId = 100L;
		Long addId1 = 13L;
		Long addId2 = 14L;

		when(questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(List.of());

		QuestionSetCategoryEntity onlyOne = mock(QuestionSetCategoryEntity.class);
		when(questionSetCategoryEntityRepository.findAllByIdInAndTeamIdAndDeletedAtIsNull(anySet(), eq(teamId)))
			.thenReturn(List.of(onlyOne));

		// when & then
		assertThatThrownBy(() ->
			questionSetCategoryService.updateLinkedCategories(questionSetId, teamId, List.of(addId1, addId2)))
			.isInstanceOf(QuestionSetCategoryException.class)
			.hasMessage(QuestionSetCategoryExceptionCode.INVALID_TEAM_OR_NOT_FOUND.getMessage());

		verify(questionSetCategoryLinkEntityRepository, never()).saveAll(anyList());
		verify(questionSetCategoryLinkEntityRepository, never())
			.deleteByQuestionSetIdAndCategoryIdIn(anyLong(), anyCollection());
	}

	@Test
	@DisplayName("updateLinkedCategories - 기존에 매핑된 카테고리는 검증 대상에서 제외 (deleted 라도 유지 가능)")
	void updateLinkedCategories_existingCategoryNotRevalidated() {
		// given
		Long questionSetId = 1L;
		Long teamId = 100L;
		Long existingId = 11L;

		QuestionSetCategoryLinkEntity existingLink = QuestionSetCategoryLinkEntity.of(questionSetId, existingId);
		when(questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(existingLink));

		// when
		questionSetCategoryService.updateLinkedCategories(questionSetId, teamId, List.of(existingId));

		// then
		verify(questionSetCategoryEntityRepository, never())
			.findAllByIdInAndTeamIdAndDeletedAtIsNull(anySet(), anyLong());
		verify(questionSetCategoryLinkEntityRepository, never()).saveAll(anyList());
		verify(questionSetCategoryLinkEntityRepository)
			.deleteByQuestionSetIdAndCategoryIdIn(eq(questionSetId), argThat(c -> c.isEmpty()));
	}

	@Test
	@DisplayName("getCategoriesByQuestionSetId - 매핑이 없으면 빈 리스트")
	void getCategoriesByQuestionSetId_noLinks_returnsEmpty() {
		// given
		Long questionSetId = 1L;
		when(questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(List.of());

		// when
		List<QuestionSetCategoryDto> result = questionSetCategoryService.getCategoriesByQuestionSetId(questionSetId);

		// then
		assertThat(result).isEmpty();
		verify(questionSetCategoryEntityRepository, never()).findAllByIdIn(anySet());
	}

	@Test
	@DisplayName("getCategoriesByQuestionSetId - 매핑된 카테고리(삭제 포함)를 DTO 로 변환")
	void getCategoriesByQuestionSetId_returnsMappedCategoriesIncludingDeleted() {
		// given
		Long questionSetId = 1L;
		Long activeId = 11L;
		Long deletedId = 12L;

		QuestionSetCategoryLinkEntity activeLink = QuestionSetCategoryLinkEntity.of(questionSetId, activeId);
		QuestionSetCategoryLinkEntity deletedLink = QuestionSetCategoryLinkEntity.of(questionSetId, deletedId);

		QuestionSetCategoryEntity activeCategory = mock(QuestionSetCategoryEntity.class);
		when(activeCategory.getId()).thenReturn(activeId);
		when(activeCategory.getName()).thenReturn("활성");
		when(activeCategory.deleted()).thenReturn(false);

		QuestionSetCategoryEntity deletedCategory = mock(QuestionSetCategoryEntity.class);
		when(deletedCategory.getId()).thenReturn(deletedId);
		when(deletedCategory.getName()).thenReturn("삭제됨");
		when(deletedCategory.deleted()).thenReturn(true);

		when(questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(activeLink, deletedLink));
		when(questionSetCategoryEntityRepository.findAllByIdIn(anySet()))
			.thenReturn(List.of(activeCategory, deletedCategory));

		// when
		List<QuestionSetCategoryDto> result = questionSetCategoryService.getCategoriesByQuestionSetId(questionSetId);

		// then
		assertThat(result).hasSize(2);
		assertThat(result).extracting(QuestionSetCategoryDto::isDeleted)
			.containsExactlyInAnyOrder(false, true);
	}

	private static QuestionSetCategoryEntity category(final Long id, final Long teamId, final String name) {
		QuestionSetCategoryEntity category = QuestionSetCategoryEntity.of(teamId, name);
		ReflectionTestUtils.setField(category, "id", id);
		return category;
	}
}
