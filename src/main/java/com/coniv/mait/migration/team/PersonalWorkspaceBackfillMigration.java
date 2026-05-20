package com.coniv.mait.migration.team;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.enums.TeamType;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.migration.MigrationJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile({"dev", "local", "prod"})
@Component
@RequiredArgsConstructor
public class PersonalWorkspaceBackfillMigration implements MigrationJob {

	private final UserEntityRepository userEntityRepository;
	private final TeamEntityRepository teamEntityRepository;
	private final TeamService teamService;

	@Override
	public void migrate() {
		List<UserEntity> users = userEntityRepository.findAll();
		Set<Long> usersWithPersonalWorkspace = teamEntityRepository.findAll().stream()
			.filter(team -> team.getType() == TeamType.PERSONAL)
			.map(TeamEntity::getCreatorId)
			.collect(Collectors.toSet());

		int created = 0;
		int skipped = 0;
		int failed = 0;

		for (UserEntity user : users) {
			if (usersWithPersonalWorkspace.contains(user.getId())) {
				skipped++;
				continue;
			}
			try {
				teamService.createPersonalWorkspace(user);
				created++;
				log.info("[{}] userId={} 개인 워크스페이스 생성", getName(), user.getId());
			} catch (Exception e) {
				failed++;
				log.error("[{}] userId={} 개인 워크스페이스 생성 실패: {}", getName(), user.getId(), e.getMessage());
			}
		}

		log.info("[{}] 전체 {}명, 생성 {}, 스킵 {}, 실패 {}",
			getName(), users.size(), created, skipped, failed);

		if (failed > 0) {
			throw new IllegalStateException(
				"[" + getName() + "] 개인 워크스페이스 백필 실패 건수: " + failed);
		}
	}
}
