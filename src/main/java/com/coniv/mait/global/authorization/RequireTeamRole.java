package com.coniv.mait.global.authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

public class RequireTeamRole {

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@PreAuthorize("@teamAuth.isManager(#teamId, authentication.principal)")
	public @interface MANAGER {
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@PreAuthorize("@teamAuth.isMember(#teamId, authentication.principal)")
	public @interface MEMBER {
	}
}
