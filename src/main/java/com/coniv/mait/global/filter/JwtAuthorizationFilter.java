package com.coniv.mait.global.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.model.MaitUser;

import io.jsonwebtoken.ExpiredJwtException;
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
	private static final String REISSUE_URL = "/api/v1/auth/reissue";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserEntityRepository userEntityRepository;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return REISSUE_URL.equals(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		final String authorizationHeader = request.getHeader(AUTH_HEADER);

		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			final String bearerToken = authorizationHeader.replace(BEARER, "");
			final Long userId = jwtTokenProvider.getUserId(bearerToken);
			jwtTokenProvider.validateAccessToken(bearerToken);
			UserEntity user = userEntityRepository.findById(userId).orElseThrow();

			setAuthentication(user);
		} catch (ExpiredJwtException ex) {
			log.warn("[JWT 토큰 만료] {}", ex.getMessage());
			throw new BadCredentialsException("Access token expired", ex);
		} catch (Exception ex) {
			log.warn("[JWT 토큰 인증 실패] {}", ex.getMessage(), ex);
		}

		filterChain.doFilter(request, response);
	}

	private void setAuthentication(UserEntity user) {
		Authentication authenticationToken = new UsernamePasswordAuthenticationToken(MaitUser.from(user), "",
			List.of());
		SecurityContextHolder.getContext().setAuthentication(authenticationToken);
	}
}
