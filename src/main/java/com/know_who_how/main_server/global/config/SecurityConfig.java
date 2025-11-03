package com.know_who_how.main_server.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Spring Security의 핵심 보안 설정을 정의하는 파일입니다.
 * API 경로별 접근 권한(로그인 필요 여부 등)을 설정하고,
 * JWT 토큰 검증 필터 및 비밀번호 암호화(PasswordEncoder) 방식을 등록합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

}
