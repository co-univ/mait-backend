package com.coniv.mait.domain.user.entity;

import java.time.LocalDateTime;

import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_policy_user_policy", columnNames = {"user_id", "policy_id"})
	},
	indexes = {
		@Index(name = "idx_user_policy_user_id", columnList = "user_id"),
	}
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPolicyCheckHistory extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Boolean isChecked;

	@Column(nullable = false)
	private LocalDateTime checkTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "policy_version_id", nullable = false)
	private PolicyVersionEntity policyVersion;

	private UserPolicyCheckHistory(Boolean isChecked, UserEntity user, PolicyVersionEntity policyVersion,
		LocalDateTime checkTime) {
		this.isChecked = isChecked;
		this.user = user;
		this.policyVersion = policyVersion;
		this.checkTime = checkTime;
	}

	public static UserPolicyCheckHistory of(Boolean isChecked, UserEntity user, PolicyVersionEntity policyVersion,
		LocalDateTime checkTime) {
		return new UserPolicyCheckHistory(isChecked, user, policyVersion, checkTime);
	}
}
