package com.coniv.mait.domain.team.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.team.entity.TeamInvitationLinkEntity;

public interface TeamInvitationEntityRepository extends JpaRepository<TeamInvitationLinkEntity, Long> {
	boolean existsByToken(String token);

	@Query("select ti from TeamInvitationLinkEntity ti join fetch ti.team where ti.token = :token")
	Optional<TeamInvitationLinkEntity> findByTokenFetchJoinTeam(@Param("token") String token);
}
