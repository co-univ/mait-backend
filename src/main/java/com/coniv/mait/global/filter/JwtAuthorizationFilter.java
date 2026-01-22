package com.coniv.mait.global.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPatternParser;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtAuthenticationEntryPoint;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;

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
	private static final PathPatternParser PARSER = new PathPatternParser();

	private final JwtTokenProvider jwtTokenProvider;

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	private final UserEntityRepository userEntityRepository;

	private final List<RequestMatcher> authRequiredMatchers = List.of(
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.POST, "/api/v1/auth/logout"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.GET, "/api/v1/users/me"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.PATCH, "/api/v1/users/nickname"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.POST, "/api/v1/teams"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.GET, "/api/v1/teams/joined"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER)
			.matcher(HttpMethod.GET, "/api/v1/teams/invitation/info"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.POST, "/api/v1/teams/*/applicant"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.POST, "/api/v1/teams/*/applicant/*"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.POST, "/api/v1/teams/*/invitation"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.POST, "/api/v1/question-sets"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.POST, "/api/v1/policies/check"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER)
			.matcher(HttpMethod.PUT, "/api/v1/question-sets/*/questions/last-viewed"),
		PathPatternRequestMatcher.withPathPatternParser(PARSER)
			.matcher(HttpMethod.GET, "/api/v1/question-sets/*/questions/last-viewed")
	);

	private final List<RequestMatcher> persistedOrNotMatchers = List.of(
		PathPatternRequestMatcher.withPathPatternParser(PARSER).matcher(HttpMethod.GET, "/api/v1/teams/invitation/info")
	);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		final String authorizationHeader = request.getHeader(AUTH_HEADER);

		if (persistedOrNotMatchers.stream().anyMatch(matcher -> matcher.matches(request))
			&& (authorizationHeader == null)) {
			filterChain.doFilter(request, response);
			return;
		}

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
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return authRequiredMatchers.stream()
			.noneMatch(matcher -> matcher.matches(request));
	}
}
