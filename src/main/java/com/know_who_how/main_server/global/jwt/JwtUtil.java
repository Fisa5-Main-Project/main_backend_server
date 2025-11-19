package com.know_who_how.main_server.global.jwt;

import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final JwtProperties jwtProperties;
    private final com.know_who_how.main_server.user.repository.UserRepository userRepository;

    // 서명 키
    private Key accessKey;
    private Key refreshKey;

    @PostConstruct
    public void init() throws Exception {
        if (jwtProperties.getSecret() == null) {
            throw new IllegalStateException("JWT secret cannot be null");
        }
        if (jwtProperties.getRefreshSecret() == null) { // Refresh Secret null check
            throw new IllegalStateException("JWT refresh secret cannot be null");
        }
        // [임시 디버깅 로그] 설정된 토큰 유효기간 값을 확인합니다.
        log.info("Loaded Access Token Validity (seconds): {}", jwtProperties.getAccessTokenValidityInSeconds());
        log.info("Loaded Refresh Token Validity (seconds): {}", jwtProperties.getRefreshTokenValidityInSeconds());

        // Access Token Key
        byte[] accessKeyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        this.accessKey = Keys.hmacShaKeyFor(accessKeyBytes);

        // Refresh Token Key
        byte[] refreshKeyBytes = Decoders.BASE64.decode(jwtProperties.getRefreshSecret());
        this.refreshKey = Keys.hmacShaKeyFor(refreshKeyBytes);
    }

    // AccessToken - 일반 요청 인증용
    public String createAccessToken(Long userId, List<String> authorities) {
        long now = System.currentTimeMillis();
        long validity = jwtProperties.getAccessTokenValidityInSeconds() * 1000L;
        Date expiration = new Date(now + validity);

        // [임시 디버깅 로그] 토큰 생성 시 사용되는 만료 시간 값을 확인합니다.
        log.info("Creating Access Token. Now: {}, Validity: {}ms, Expiration: {}", new Date(now), validity, expiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("authorities", String.join(",", authorities))
                .setExpiration(expiration)
                .signWith(accessKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // RefreshToken - 재발급용
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenValidityInSeconds() * 1000L))
                .signWith(refreshKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // accessToken 인증정보 추출
    public Authentication getAuthenticationFromAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((SecretKey) accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.parseLong(claims.getSubject());
        com.know_who_how.main_server.global.entity.User.User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities());
    }

    // 유효성 검사 (용도별 분리)
    public boolean validateAccessToken(String token) {
        return validate(token, accessKey);
    }

    public boolean validateRefreshToken(String token) {
        return validate(token, refreshKey);
    }

    // 유효성 검사
    public boolean validate(String token, Key key) {
        if (token == null || token.trim().isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_TOKEN_ERROR);
        }

        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException | SecurityException e) {
            log.error("JWT 서명 검증 실패", e);
            throw new CustomException(ErrorCode.INVALID_TOKEN_SIGNATURE);
        } catch (MalformedJwtException e) {
            log.error("JWT 형식 오류", e);
            throw new CustomException(ErrorCode.MALFORMED_TOKEN_ERROR);
        } catch (ExpiredJwtException e) {
            log.error("JWT 만료", e);
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰 형식", e);
            throw new CustomException(ErrorCode.UNSUPPORTED_TOKEN_ERROR);
        } catch (IllegalArgumentException e) {
            log.error("JWT 파싱 실패 - 비어있는 토큰 등", e);
            throw new CustomException(ErrorCode.TOKEN_PARSING_FAILED);
        }
    }

    //  토큰에서 userId(subject) 추출 (만료된 토큰에서도 추출 가능하도록 수정)
    public Long extractUserId(String token, boolean isRefresh) {
        Key key = isRefresh ? refreshKey : accessKey;
        try {
            return Long.parseLong(Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
        } catch (ExpiredJwtException e) {
            // 만료된 토큰의 경우에도 subject(userId)는 반환
            return Long.parseLong(e.getClaims().getSubject());
        }
    }

    // 토큰에서 만료 시간 추출 (만료된 토큰에서도 추출 가능하도록 수정)
    public Date extractExpiration(String token, boolean isRefresh) {
        Key key = isRefresh ? refreshKey : accessKey;
        try {
            return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰의 경우에도 만료 시간은 반환
            return e.getClaims().getExpiration();
        }
    }

}
