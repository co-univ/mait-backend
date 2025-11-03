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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"nickname", "code"}))
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

	@Column(length = 20)
	private String nickname;

	@Column(name = "code", length = 4)
	private String nicknameCode;

	private Boolean isLocalLogin;

	@Column(unique = true)
	private String providerId;

	@Enumerated(EnumType.STRING)
	private LoginProvider loginProvider;

	private UserEntity(String email, String name, String nickname, Boolean isLocalLogin, String providerId,
		LoginProvider loginProvider) {
		this.email = email;
		this.name = name;
		this.nickname = nickname;
		this.isLocalLogin = isLocalLogin;
		this.providerId = providerId;
		this.loginProvider = loginProvider;
	}

	private UserEntity(String email, String password, String name, String nickname, Boolean isLocalLogin,
		String providerId, LoginProvider loginProvider) {
		this.email = email;
		this.password = password;
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

	public static UserEntity localLoginUser(String email, String password, String name, String nickname) {
		return new UserEntity(email, password, name, nickname, true, null, null);
	}

	public void updateNickname(final String nickname, final String nicknameCode) {
		this.nickname = nickname;
		this.nicknameCode = nicknameCode;
	}

	public String getFullNickname() {
		return this.nickname + "#" + this.nicknameCode;
	}
}
