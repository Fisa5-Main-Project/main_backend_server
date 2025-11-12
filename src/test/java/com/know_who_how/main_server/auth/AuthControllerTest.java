package com.know_who_how.main_server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.auth.dto.LogoutRequestDto;
import com.know_who_how.main_server.auth.service.SmsCertificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SmsCertificationService smsCertificationService;

    @Test
    @DisplayName("로그아웃_should_성공_when_유효한-리프레시-토큰")
    void logout_should_ReturnSuccess_when_ValidRequest() throws Exception {
        // given
        // LogoutRequestDto has no setter, so create json manually.
        String jsonRequest = "{\"refreshToken\": \"valid-refresh-token\"}";

        willDoNothing().given(authService).logout(any(), any());

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer dummy-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data").value("로그아웃이 완료되었습니다."));
    }
}
