package com.coniv.mait.migration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.coniv.mait.migration.entity.MigrationLog;
import com.coniv.mait.migration.entity.MigrationLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile({"dev", "local", "prod"})
@Slf4j
@Component
@RequiredArgsConstructor
public class MigrationRunner implements ApplicationRunner {

	private final MigrationLogRepository migrationLogRepository;
	private final List<MigrationJob> migrationJobs;

	@Override
	public void run(ApplicationArguments args) {
		for (MigrationJob migrationJob : migrationJobs) {
			Optional<MigrationLog> maybeMigration = migrationLogRepository.findByName(migrationJob.getName());
			if (maybeMigration.isPresent()) {
				log.info("[Migration] Already Done: {}", migrationJob.getName());
				continue;
			}
			try {
				migrationJob.migrate();
			} catch (Exception e) {
				log.error("[Migration] Failed: {} {}", migrationJob.getName(), e.getMessage());
				continue;
			}
			migrationLogRepository.save(MigrationLog.builder()
				.name(migrationJob.getName())
				.createdAt(LocalDateTime.now())
				.build());
		}
	}
}
