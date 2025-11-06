package com.coniv.mait.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.entity.PolicyVersionEntity;

public interface PolicyVersionEntityRepository extends JpaRepository<PolicyVersionEntity, Long> {
	@Query("SELECT pv FROM PolicyVersionEntity pv WHERE pv.policy = :policy ORDER BY pv.version DESC LIMIT 1")
	Optional<PolicyVersionEntity> findLatestByPolicy(@Param("policy") PolicyEntity policy);

	@Query("""
		SELECT pv FROM PolicyVersionEntity pv
		WHERE pv.policy IN :policies
		AND pv.version = (
		SELECT MAX(pv2.version)
		FROM PolicyVersionEntity pv2
		WHERE pv2.policy = pv.policy)
		""")
	List<PolicyVersionEntity> findLatestVersionsByPolicies(@Param("policies") List<PolicyEntity> policies);
}
