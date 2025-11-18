package com.coniv.mait.domain.team.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.team.entity.TeamInviteEntity;

public interface TeamInviteEntityRepository extends JpaRepository<TeamInviteEntity, Long> {
	boolean existsByToken(String token);

	@Query("select ti from TeamInviteEntity ti join fetch ti.team where ti.token = :token")
	Optional<TeamInviteEntity> findByToken(@Param("token") String token);
}
