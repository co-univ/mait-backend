package com.coniv.mait.domain.user.entity;

import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.global.entity.BaseTimeEntity;

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
public class UserEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	private String password;

	@Column(nullable = false)
	private String name;

	@Column(unique = true)
	private String nickname;

	private Boolean isLocalLogin;

	@Column(unique = true)
	private String providerId;

	@Enumerated(EnumType.STRING)
	private LoginProvider loginProvider;

	//TODO 초대링크 작업 시 팀 필드 추가

	private UserEntity(String email, String name, String nickname, Boolean isLocalLogin, String providerId,
		LoginProvider loginProvider) {
		this.email = email;
		this.name = name;
		this.nickname = nickname;
		this.isLocalLogin = isLocalLogin;
		this.providerId = providerId;
		this.loginProvider = loginProvider;
	}

	public static UserEntity socialLoginUser(String email, String name, String providerId,
		LoginProvider loginProvider) {
		return new UserEntity(email, name, null, false, providerId, loginProvider);
	}
}
