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
    private final JwtAuthFilter jwtAuthFilter; // JwtAuthFilter 주입
    private final JwtUtil jwtUtil;

    // [신규] 인증이 필요 없는 API 경로 (v1 적용)
    private static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/login",                   // 로그인
            "/api/v1/auth/signup",                  // 회원가입
            "/api/v1/auth/sms-certification/sends", // SMS 인증번호 발송
            "/api/v1/auth/sms-certification/confirms",// SMS 인증번호 확인
            "/api/v1/auth/check-id",                // 아이디 중복 확인
            "/swagger-ui.html",                     // Swagger UI HTML
            "/swagger-ui/**",                       // Swagger UI (JS, CSS 등)
            "/v3/api-docs/**",                      // OpenAPI 3.0 Docs
            "/v1/api-docs/**",                      // Swagger API Docs (application.yml 설정)
            "/webjars/**",                          // Swagger UI Webjars
            "/error"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 적용
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP 기본 인증 비활성화

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 사용 안 함
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthEntryPoint)    // 401 (인증 실패)
                        .accessDeniedHandler(jwtAccessDeniedHandler)    // 403 (권한 없음)
                )

                // [수정] v1 경로를 AUTH_WHITELIST로 통합
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(AUTH_WHITELIST).permitAll() // AUTH_WHITELIST 경로는 모두 허용
                        .anyRequest().authenticated() // 나머지 모든 경로는 인증 필요
                )

                // [유지] 사용자가 제공한 JwtAuthFilter 사용
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // FE 주소 정해지면 추가
        configuration.setAllowedOrigins(List.of("*"));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false); // JWT 토큰 사용 시, 자격증명(쿠키 등) 불필요
        configuration.setMaxAge(3600L); // Preflight 요청 캐시 시간 (1시간)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 이 CORS 설정 적용
        return source;
    }
}
