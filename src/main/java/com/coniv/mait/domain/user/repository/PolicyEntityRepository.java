package com.coniv.mait.domain.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.enums.PolicyTiming;

public interface PolicyEntityRepository extends JpaRepository<PolicyEntity, Long> {
	List<PolicyEntity> findAllByTiming(PolicyTiming timing);
}
