package com.coniv.mait.domain.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.enums.TeamType;

public interface TeamEntityRepository extends JpaRepository<TeamEntity, Long> {
	Optional<TeamEntity> findByIdAndDeletedAtIsNull(Long id);

	List<TeamEntity> findAllByType(TeamType type);
}
