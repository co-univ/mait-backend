package com.coniv.mait.login;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.transaction.support.TransactionTemplate;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

@TestComponent
public class WithCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithCustomUser> {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Override
	public SecurityContext createSecurityContext(WithCustomUser annotation) {
		String email = annotation.email();
		String name = annotation.name();

		UserEntity user = transactionTemplate.execute(status -> userEntityRepository.findByEmail(email)
			.orElseGet(() -> {
				UserEntity newUser = UserEntity.socialLoginUser(email, name, "providerId", LoginProvider.GOOGLE);
				return userEntityRepository.save(newUser);
			}));

		UserEntity byEmail = userEntityRepository.findByEmail(email).get();
		System.out.println(byEmail.getEmail() + " " + byEmail.getId() + " byEmail Content");

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,
			"N/A",
			List.of()
		);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		return context;
	}
}
