package com.know_who_how.main_server.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Slf4j
public class LoggingHttpSessionOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final HttpSessionOAuth2AuthorizationRequestRepository delegate = new HttpSessionOAuth2AuthorizationRequestRepository();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        OAuth2AuthorizationRequest result = delegate.loadAuthorizationRequest(request);
        HttpSession session = request.getSession(false);
        String sessionId = (session != null) ? session.getId() : "null";
        log.debug("Load Authorization Request: sessionId={}, result={}", sessionId, result);
        return result;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true); // Force create session if needed
        String sessionId = session.getId();
        log.debug("Save Authorization Request: sessionId={}, request={}", sessionId, authorizationRequest);
        delegate.saveAuthorizationRequest(authorizationRequest, request, response);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest result = delegate.removeAuthorizationRequest(request, response);
        HttpSession session = request.getSession(false);
        String sessionId = (session != null) ? session.getId() : "null";
        log.debug("Remove Authorization Request: sessionId={}, result={}", sessionId, result);
        return result;
    }
}
