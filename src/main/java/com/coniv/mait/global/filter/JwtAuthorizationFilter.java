package com.coniv.mait.global.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.jwt.JwtAuthenticationEntryPoint;
import com.coniv.mait.global.jwt.JwtTokenProvider;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

	private static final String AUTH_HEADER = "Authorization";

	private static final String BEARER = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	private final UserEntityRepository userEntityRepository;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		final String authorizationHeader = request.getHeader(AUTH_HEADER);
		final String bearerToken = getBearerToken(authorizationHeader);
		try {
			jwtTokenProvider.validateAccessToken(bearerToken);
			final Long userId = jwtTokenProvider.getUserId(bearerToken);
			UserEntity user = userEntityRepository.findById(userId).orElseThrow();

			setAuthentication(user);
			filterChain.doFilter(request, response);
		} catch (BadCredentialsException | JwtException e) {
			jwtAuthenticationEntryPoint.commence(request, response,
				new BadCredentialsException("Invalid JWT token", e));
		}
	}

	private void setAuthentication(UserEntity user) {
		log.info("authenticated member : <{}> , <{}>", user.getId(), user.getName());

		Authentication authenticationToken = new UsernamePasswordAuthenticationToken(user, "",
			List.of());
		SecurityContextHolder.getContext().setAuthentication(authenticationToken);
	}

	private String getBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
			throw new BadCredentialsException("Authorization header is missing or does not start with Bearer");
		}
		return authorizationHeader.replace(BEARER, "");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String path = request.getRequestURI();
		String method = request.getMethod();

		if ("GET".equals(method) && pathMatcher.match("/api/v1/users/me", path)) {
			return false; // 필터를 실행
		}

		if ("POST".equals(method) && pathMatcher.match("/api/v1/teams", path)) {
			return false; // 필터를 실행
		}

		return true;
	}
}
