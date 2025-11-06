package com.know_who_how.main_server.global.config;

import com.know_who_how.main_server.global.jwt.JwtAccessDeniedHandler;
import com.know_who_how.main_server.global.jwt.JwtAuthEntryPoint;
import com.know_who_how.main_server.global.jwt.JwtAuthFilter;
import com.know_who_how.main_server.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security의 핵심 보안 설정을 정의하는 파일입니다.
 * API 경로별 접근 권한(로그인 필요 여부 등)을 설정하고,
 * JWT 토큰 검증 필터 및 비밀번호 암호화(PasswordEncoder) 방식을 등록합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtUtil jwtUtil;

    // PasswordEncoder 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 정의한 CORS 설정을 Security Filter Chain에 적용

                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화 (stateless 세션 사용 시)
                .formLogin(AbstractHttpConfigurer::disable) // 폼 기반 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP 기본 인증 비활성화

                // 세션 관리 정책 설정
                .sessionManagement(session ->
                        // 세션을 생성하거나 사용하지 않도록 설정 (STATELESS)
                        // 모든 요청은 인증 정보(JWT)를 포함
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 예외 처리 핸들러 설정
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthEntryPoint)    // 401 (인증 실패) (예: 유효하지 않은 토큰, 토큰 없음 등)
                        .accessDeniedHandler(jwtAccessDeniedHandler)    // 403 (권한 없음) (예: 인증은 되었으나, 해당 리소스에 접근 권한이 없음)
                )

                // HTTP 요청별 권한 설정
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/api/v1/auth/login",     // 로그인
                                "/user/signup",    // 회원가입
                                "/user/**/exists-id", // 아이디 중복 확인
                                "/user/**/exists-phonenum", // 전화번호 중복 확인
                                "/public/**",          // 정적 리소스 등
                                "/error"
                        ).permitAll()
                        // 위의 경로 외에는 인증 되어야 함
                        .anyRequest().authenticated()
                )

                // 사용자 정의 필터 추가
                .addFilterBefore(new JwtAuthFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // FE 주소 정해지면 추가
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*")); // 현재 JWT 방식이기에 자격증명 사용 안함
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
