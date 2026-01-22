package com.coniv.mait.global.authorization;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.exception.TeamManagerException;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RequireTeamRole 어노테이션을 처리하는 AOP Aspect
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TeamRoleAspect {

	private final TeamAuthorizationChecker teamAuthChecker;

	@Before("@annotation(requireTeamRole)")
	public void checkTeamRole(JoinPoint joinPoint, RequireTeamRole requireTeamRole) {

		// 현재 인증된 사용자 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity loginUser)) {
			log.warn("User is not authenticated or principal is not UserEntity");
			throw new TeamManagerException("User is not authenticated.");
		}

		// 메서드 파라미터에서 teamId 찾기
		Long teamId = extractTeamId(joinPoint);
		if (teamId == null) {
			throw new IllegalArgumentException("teamId parameter is required for @RequireTeamRole annotation");
		}

		TeamUserRole requiredRole = requireTeamRole.value();

		log.info("Checking team role - teamId: {}, userId: {}, requiredRole: {}",
			teamId, loginUser.getId(), requiredRole);

		teamAuthChecker.hasRole(teamId, loginUser, requiredRole);
	}

	private Long extractTeamId(JoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		String[] parameterNames = signature.getParameterNames();
		Object[] args = joinPoint.getArgs();

		for (int i = 0; i < parameterNames.length; i++) {
			if ("teamId".equals(parameterNames[i]) && args[i] instanceof Long) {
				return (Long)args[i];
			}
		}

		return null;
	}
}

