package com.know_who_how.main_server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.auth.controller.AuthController;
import com.know_who_how.main_server.auth.dto.LoginRequestDto;
import com.know_who_how.main_server.auth.dto.TokenResponseDto;
import com.know_who_how.main_server.auth.dto.UserSignupRequestDto;
import com.know_who_how.main_server.auth.service.AuthService;
import com.know_who_how.main_server.auth.service.SmsCertificationService;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.jwt.JwtAuthFilter;
import com.know_who_how.main_server.global.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SmsCertificationService smsCertificationService;

    @MockBean
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("아이디 중복 확인 - 사용 가능")
    void checkLoginIdDuplicate_should_ReturnSuccess_when_IdIsAvailable() throws Exception {
        // given
        String loginId = "newuser";
        String successMessage = "사용 가능한 아이디입니다.";
        given(authService.checkLoginIdDuplicate(loginId)).willReturn(successMessage);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/signup/check-login-id")
                .param("loginId", loginId));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").value(successMessage));
    }

    @Test
    @DisplayName("아이디 중복 확인 - 이미 사용 중")
    void checkLoginIdDuplicate_should_ThrowException_when_IdIsTaken() throws Exception {
        // given
        String loginId = "existinguser";
        given(authService.checkLoginIdDuplicate(loginId)).willThrow(new CustomException(ErrorCode.LOGIN_ID_DUPLICATE));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/signup/check-login-id")
                .param("loginId", loginId));

        // then
        resultActions.andExpect(status().isConflict()) // 409 Conflict
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.LOGIN_ID_DUPLICATE.getCode()));
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    @WithMockUser
    void logout_should_ReturnSuccess_when_ValidRequest() throws Exception {
        // given
        String accessToken = "dummy-access-token";
        String refreshToken = "dummy-refresh-token";

        willDoNothing().given(authService).logout(any(String.class), any(String.class));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .cookie(new Cookie("refresh_token", refreshToken))); // Simulate browser sending HttpOnly cookie

        // then
        MvcResult result = resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").value("로그아웃이 완료되었습니다."))
                .andExpect(cookie().exists("refresh_token")) // Check that the cookie is present (for deletion instruction)
                .andExpect(cookie().maxAge("refresh_token", 0)) // Verify Max-Age is 0 for deletion
                .andReturn();
    }

    @Test
    @DisplayName("회원가입 - 성공")
    void signup_should_ReturnSuccess_when_ValidRequest() throws Exception {
        // given
        // Since DTO has no setters, construct JSON manually
        String requestJson = """
                {
                  "verificationId": "c78a8a1d-2c4f-4f3a-b5f7-5a9a8e3a4b2f",
                  "termAgreements": [
                    { "termId": 1, "isAgreed": true },
                    { "termId": 2, "isAgreed": true },
                    { "termId": 3, "isAgreed": false
                     }
                  ],
                  "loginId": "newuser123",
                  "password": "Password123!",
                  "financialPropensity": "STABLE",
                  "keywordIds": [1, 2, 3]
                }
                """;

        willDoNothing().given(authService).signup(any(UserSignupRequestDto.class));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("로그인 - 성공")
    void login_should_ReturnTokens_when_CredentialsAreValid() throws Exception {
        // given
        String requestJson = """
                {
                  "loginId": "testuser1",
                  "password": "password123!"
                }
                """;

        TokenResponseDto tokenResponseDto = TokenResponseDto.builder()
                .grantType("Bearer")
                .accessToken("dummy-access-token")
                .refreshToken("dummy-refresh-token")
                .build();

        given(authService.login(any(LoginRequestDto.class))).willReturn(tokenResponseDto);
        given(jwtProperties.getRefreshTokenValidityInSeconds()).willReturn(604800L); // Mock the TTL for cookie

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.grantType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").value("dummy-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist()) // Verify refresh token is not in body
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().secure("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/"))
                .andExpect(cookie().maxAge("refresh_token", 604800))
                .andExpect(cookie().value("refresh_token", "dummy-refresh-token"));
    }
}
