package com.know_who_how.main_server.global.config;

import com.know_who_how.main_server.global.jwt.JwtAccessDeniedHandler;
import com.know_who_how.main_server.global.jwt.JwtAuthEntryPoint;
import com.know_who_how.main_server.global.jwt.JwtAuthFilter;
import com.know_who_how.main_server.global.jwt.JwtUtil;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 보안 설정
 * - 접근 제어, JWT 필터, CORS, OpenAPI 유지
 * - [변경] BFF + OAuth2 로그인 지원: 세션 정책/허용 경로/성공 핸들러 추가
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthFilter jwtAuthFilter; // JwtAuthFilter 주입
    private final JwtUtil jwtUtil;
    // [추가] OAuth2 로그인 성공 핸들러 (AT/RT 동기화)
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final AppProperties appProperties;

    private static final Logger sessionLog = LoggerFactory.getLogger(SecurityConfig.class);

    // [변경] OAuth2 Client 도입: 로그인 시작 경로 허용 추가
    private static final String[] AUTH_WHITELIST = {

            "/api/v1/auth/login",                   // 로그인
            "/api/v1/auth/reissue",                 // 토큰 재발급
            "/api/v1/auth/signup/**",               // 회원가입 관련 모든 경로
            "/login/oauth2/code/**",                // 소셜 로그인 콜백 경로
            "/oauth2/authorization/**",      // [추가] OAuth2 로그인 시작 경로 허용
            "/swagger-ui.html",                     // Swagger UI HTML
            "/swagger-ui/**",                       // Swagger UI (JS, CSS 등)
            "/v3/api-docs/**",                      // OpenAPI 3.0 Docs
            "/v1/api-docs/**",                      // Swagger API Docs (application.yml 설정)
            "/webjars/**",                          // Swagger UI Webjars
            "/error"
    };

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("KnowWhoHow API Docs")
                .version("v1.0.0")
                .description("KnowWhoHow 프로젝트의 API 명세서입니다.");

        // Security Scheme 이름
        String jwtSchemeName = "Bearer Authentication";
        // API 요청 헤더에 인증 정보 포함
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        // SecuritySchemes 등록
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer")
                        .bearerFormat("JWT") // 토큰 형식 지정
                        .description("Access Token 값만 입력해주세요. (예: eyJhbGci...) Bearer 접두사는 자동으로 추가됩니다."));

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

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

        // [변경] BFF 세션 기반 OAuth2 로그인 지원: 세션 정책 IF_REQUIRED 재설정
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        // [추가] OAuth2 Client 로그인 활성화 (성공 핸들러 등록)
        http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                        .authorizationRequestRepository(new LoggingHttpSessionOAuth2AuthorizationRequestRepository())
                )
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    sessionLog.error("OAuth2 Login Failed: {}", exception.getMessage(), exception);
                    response.sendRedirect(appProperties.getFrontendBaseUrl() + "/login?error");
                })
        );

        return http.build();
    }

    @Bean
    public Filter sessionLogFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                var session = request.getSession(false);
                String sid = (session != null) ? session.getId() : "null";
                sessionLog.info("SESSION_TRACE sid={} uri={} method={}", sid, request.getRequestURI(), request.getMethod());
                chain.doFilter(request, response);
            }
        };
    }
}
