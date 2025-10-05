package com.coniv.mait.migration.entity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationLogRepository extends JpaRepository<MigrationLog, Long> {
	Optional<MigrationLog> findByName(String name);
}
