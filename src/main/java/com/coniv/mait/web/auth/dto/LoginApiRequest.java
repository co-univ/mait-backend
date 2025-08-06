package com.coniv.mait.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginApiRequest(
	@Email(message = "유효한 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일을 입력해주세요.")
	String email,

	@NotBlank(message = "비밀번호를 입력해주세요.")
	String password
) {
}
