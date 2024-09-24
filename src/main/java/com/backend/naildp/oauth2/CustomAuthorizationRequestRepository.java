package com.backend.naildp.oauth2;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.backend.naildp.common.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomAuthorizationRequestRepository
	implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private static final String AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
	private static final String OAUTH2_STATE_COOKIE_NAME = "oauth2_state";
	private static final int COOKIE_EXPIRE_SECONDS = 180;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {

		OAuth2AuthorizationRequest authorizationRequest = CookieUtil.getStateCookie(request,
				AUTHORIZATION_REQUEST_COOKIE_NAME)
			.map(cookie -> CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class))
			.orElse(null);

		if (authorizationRequest == null) {
			log.error("Authorization request not found in the session or cookies.");
		} else {
			log.info("Authorization request and state value found: {}", authorizationRequest.getState());
		}

		return authorizationRequest;

	}

	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
		HttpServletResponse response) {

		if (authorizationRequest == null) {
			CookieUtil.deleteCookie(AUTHORIZATION_REQUEST_COOKIE_NAME, request, response);
			CookieUtil.deleteCookie(OAUTH2_STATE_COOKIE_NAME, request, response);
			return;
		}

		CookieUtil.addStateCookie(response, AUTHORIZATION_REQUEST_COOKIE_NAME,
			CookieUtil.serialize(authorizationRequest),
			COOKIE_EXPIRE_SECONDS, true, "None");

		String state = authorizationRequest.getState();
		CookieUtil.addStateCookie(response, OAUTH2_STATE_COOKIE_NAME,
			state,
			COOKIE_EXPIRE_SECONDS, true, "None");
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
		HttpServletResponse response) {

		OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
		if (authorizationRequest != null) {
			CookieUtil.deleteCookie(AUTHORIZATION_REQUEST_COOKIE_NAME, request, response);
			CookieUtil.deleteCookie(OAUTH2_STATE_COOKIE_NAME, request, response);
		}
		return authorizationRequest;
	}
}
