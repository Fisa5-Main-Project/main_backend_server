package com.know_who_how.main_server.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.admin.dto.UserResponseDto;
import com.know_who_how.main_server.admin.service.AdminUserService;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.jwt.JwtAccessDeniedHandler;
import com.know_who_how.main_server.global.jwt.JwtAuthEntryPoint;
import com.know_who_how.main_server.global.jwt.JwtUtil;
import com.know_who_how.main_server.global.util.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class,
    excludeAutoConfiguration = {
        WebFluxAutoConfiguration.class
    }
)
@TestPropertySource(properties = {"springdoc.api-docs.enabled=false"})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("전체 사용자 목록 조회 - 성공")
    @WithMockUser
    void getUsers_Success() throws Exception {
        // given
        UserResponseDto user1 = new UserResponseDto(1L, "홍길동", "gildong_id", 30, 1000000L, LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1), "active");
        UserResponseDto user2 = new UserResponseDto(2L, "김철수", "chulsoo_id", 25, 500000L, LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(5), "active");
        List<UserResponseDto> userList = Arrays.asList(user1, user2);

        given(adminUserService.getUsers()).willReturn(userList);

        // when & then
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(userList.size()))
                .andExpect(jsonPath("$[0].id").value(user1.getId()))
                .andExpect(jsonPath("$[0].loginId").value(user1.getLoginId()))
                .andExpect(jsonPath("$[1].id").value(user2.getId()))
                .andExpect(jsonPath("$[1].loginId").value(user2.getLoginId()))
                .andDo(print());

        // verify
        verify(adminUserService).getUsers();
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 - 실패 (서비스 예외)")
    @WithMockUser
    void getUsers_Fail_ServiceException() throws Exception {
        // given
        given(adminUserService.getUsers()).willThrow(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        // when & then
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                .andDo(print());
    }
}