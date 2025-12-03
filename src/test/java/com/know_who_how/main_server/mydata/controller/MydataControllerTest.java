package com.know_who_how.main_server.mydata.controller;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.jwt.JwtAuthFilter;
import com.know_who_how.main_server.mydata.dto.MydataDto;
import com.know_who_how.main_server.mydata.service.MydataService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MydataController.class)
@AutoConfigureMockMvc(addFilters = false)
class MydataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private MydataService mydataService;

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
    @DisplayName("GET /api/v1/resource/my-data - 로그인 시 MyData 반환")
    void getMyData_success() throws Exception {
        // given
        User user = createTestUser();
        setAuthentication(user);
        MydataDto dto = new MydataDto();
        dto.setAssets(Collections.emptyList());
        dto.setLiabilities(Collections.emptyList());
        given(mydataService.getMyData(user)).willReturn(dto);

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/resource/my-data").accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.assets").isArray())
                .andExpect(jsonPath("$.data.liabilities").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/resource/my-data - 미인증 시 403 반환")
    void getMyData_forbidden_whenNotLoggedIn() throws Exception {
        // given
        SecurityContextHolder.clearContext();

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/resource/my-data").accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_002"));
    }
}
