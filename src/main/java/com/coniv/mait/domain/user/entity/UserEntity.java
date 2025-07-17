package com.coniv.mait.domain.user.entity;

import com.coniv.mait.domain.user.enums.LoginProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	private String password;

	@Column(nullable = false)
	private String name;

	private Boolean isLocalLogin;

	private String providerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LoginProvider loginProvider;
}