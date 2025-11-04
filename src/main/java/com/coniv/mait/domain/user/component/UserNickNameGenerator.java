package com.coniv.mait.domain.user.component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserNickNameGenerator {

	private static final int MAX_NICKNAME_CODE = 9999;
	private static final int MIN_NICKNAME_CODE = 0;
	private static final int TOTAL_CODE_RANGE = MAX_NICKNAME_CODE - MIN_NICKNAME_CODE + 1;

	private final UserEntityRepository userEntityRepository;

	public String generateNicknameCode(final String nickname) {
		List<UserEntity> existUserEntity = userEntityRepository.findAllByNickname(nickname);

		Set<Integer> existCodes = existUserEntity.stream()
			.map(UserEntity::getNicknameCode)
			.filter(code -> code != null && !code.isEmpty())
			.map(Integer::parseInt)
			.collect(Collectors.toSet());

		if (existCodes.size() >= TOTAL_CODE_RANGE) {
			throw new UserParameterException("해당 닉네임은 사용할 수 없습니다: " + nickname);
		}

		int startCode = RandomUtil.getRandomNumber(TOTAL_CODE_RANGE);

		for (int i = 0; i < TOTAL_CODE_RANGE; i++) {
			int code = (startCode + i) % TOTAL_CODE_RANGE;
			if (!existCodes.contains(code)) {
				return String.format("%04d", code);
			}
		}

		throw new UserParameterException("해당 닉네임은 사용할 수 없습니다: " + nickname);
	}
}
