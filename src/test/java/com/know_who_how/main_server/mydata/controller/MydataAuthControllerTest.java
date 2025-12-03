package com.know_who_how.main_server.mydata.controller;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.jwt.JwtAuthFilter;
import com.know_who_how.main_server.mydata.service.MydataAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MydataAuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 비활성화 (직접 principal 세팅)
class MydataAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private MydataAuthService mydataAuthService;

    private User createTestUser() {
        try {
            var ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            User user = ctor.newInstance();
            ReflectionTestUtils.setField(user, "userId", 1L);
            ReflectionTestUtils.setField(user, "loginId", "testUser");
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setAuthentication(User user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/v1/my-data/authorize - 로그인 상태면 authorize url 반환")
    void authorize_success_whenLoggedIn() throws Exception {
        // given
        User user = createTestUser();
        setAuthentication(user);
        String authorizeUrl = "http://localhost:9000/oauth2/authorize?client_id=test";
        given(mydataAuthService.buildAuthorizeUrl()).willReturn(authorizeUrl);

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/my-data/authorize").accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").value(authorizeUrl));
        verify(mydataAuthService).buildAuthorizeUrl();
    }

    @Test
    @DisplayName("GET /api/v1/my-data/authorize - 미인증이면 403 응답")
    void authorize_fail_whenNotLoggedIn() throws Exception {
        // given
        SecurityContextHolder.clearContext();

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/my-data/authorize").accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_002"));
    }

    @Test
    @DisplayName("GET /api/v1/my-data/callback - 정상 콜백 처리")
    void callback_success() throws Exception {
        // given
        User user = createTestUser();
        setAuthentication(user);
        String code = "auth-code-123";
        String state = "xyz-state";

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/my-data/callback")
                        .param("code", code)
                        .param("state", state)
        );

        // then
        result.andExpect(status().isOk());
        verify(mydataAuthService).handleCallback(user.getUserId(), code, state);
    }

    @Test
    @DisplayName("GET /api/v1/my-data/callback - state 없이도 정상 처리")
    void callback_success_without_state() throws Exception {
        // given
        User user = createTestUser();
        setAuthentication(user);

        String code = "auth-code-456";

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/my-data/callback")
                        .param("code", code)
        );

        // then
        result.andExpect(status().isOk());
        verify(mydataAuthService).handleCallback(user.getUserId(), code, null);
    }

    @Test
    @DisplayName("GET /api/v1/my-data/callback - 미인증이면 403 응답")
    void callback_fail_whenNotLoggedIn() throws Exception {
        // given
        SecurityContextHolder.clearContext();

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/my-data/callback")
                        .param("code", "auth-code-123")
                        .param("state", "xyz-state")
        );

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_002"));
    }
}
