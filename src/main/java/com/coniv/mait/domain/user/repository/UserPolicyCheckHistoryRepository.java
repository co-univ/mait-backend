package com.coniv.mait.domain.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.user.entity.UserPolicyCheckHistory;

public interface UserPolicyCheckHistoryRepository extends JpaRepository<UserPolicyCheckHistory, Long> {

	List<UserPolicyCheckHistory> findAllByUserId(Long userId);
}

