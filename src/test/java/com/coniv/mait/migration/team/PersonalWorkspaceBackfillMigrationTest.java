package com.coniv.mait.migration.team;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.enums.TeamType;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

@ExtendWith(MockitoExtension.class)
class PersonalWorkspaceBackfillMigrationTest {

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private TeamEntityRepository teamEntityRepository;

	@Mock
	private TeamService teamService;

	@InjectMocks
	private PersonalWorkspaceBackfillMigration migration;

	@Test
	@DisplayName("개인 워크스페이스가 없는 사용자는 기본 생성 로직으로 생성한다")
	void migrate_CreatesMissingPersonalWorkspace() {
		// given
		UserEntity user = mock(UserEntity.class);
		when(user.getId()).thenReturn(1L);

		when(userEntityRepository.findAll()).thenReturn(List.of(user));
		when(teamEntityRepository.findAllByType(TeamType.PERSONAL)).thenReturn(List.of());

		// when
		migration.migrate();

		// then
		verify(teamService).createPersonalWorkspace(user);
	}

	@Test
	@DisplayName("개인 워크스페이스가 이미 있는 사용자는 기존 이름을 변경하지 않고 스킵한다")
	void migrate_SkipsExistingPersonalWorkspace() {
		// given
		UserEntity user = mock(UserEntity.class);
		when(user.getId()).thenReturn(1L);

		TeamEntity personalWorkspace = TeamEntity.ofPersonal("홍길동의 워크스페이스", 1L);

		when(userEntityRepository.findAll()).thenReturn(List.of(user));
		when(teamEntityRepository.findAllByType(TeamType.PERSONAL)).thenReturn(List.of(personalWorkspace));

		// when
		migration.migrate();

		// then
		assertThat(personalWorkspace.getName()).isEqualTo("홍길동의 워크스페이스");
		verify(teamService, never()).createPersonalWorkspace(any());
	}
}
