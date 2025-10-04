package com.coniv.mait.domain.team.service.component;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.team.repository.TeamInviteEntityRepository;
import com.coniv.mait.util.Base62Convertor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InviteTokenGenerator {

	private static final int MAX_RETRY_COUNT = 3;

	private final TeamInviteEntityRepository teamInviteEntityRepository;

	public String generateUniqueInviteToken() {
		String inviteToken;

		for (int attempt = 0; attempt < MAX_RETRY_COUNT; attempt++) {
			inviteToken = Base62Convertor.uuidToBase62(UUID.randomUUID());
			if (!teamInviteEntityRepository.existsByToken(inviteToken)) {
				return inviteToken;
			}
		}
		throw new IllegalStateException(
			"Failed to generate unique invite token after " + MAX_RETRY_COUNT + " attempts");
	}
}
