package com.coniv.mait.domain.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.user.entity.UserEntity;

public interface TeamUserEntityRepository extends JpaRepository<TeamUserEntity, Long> {
	List<TeamUserEntity> findAllByTeamId(Long teamId);

	Optional<TeamUserEntity> findByTeamAndUser(TeamEntity team, UserEntity user);

	boolean existsByTeamAndUser(TeamEntity team, UserEntity user);

	@Query("select tu from TeamUserEntity tu join fetch tu.team where tu.user = :user")
	List<TeamUserEntity> findAllByUserFetchJoinTeam(@Param("user") final UserEntity user);

	@Query("select tu from TeamUserEntity tu join fetch tu.user where tu.team.id = :teamId")
	List<TeamUserEntity> findAllByTeamIdFetchJoinUser(@Param("teamId") Long teamId);

	boolean existsByTeamIdAndUserIdAndUserRoleIn(Long teamId, Long userId, List<TeamUserRole> roles);

	boolean existsByTeamIdAndUserId(Long teamId, Long userId);
}
