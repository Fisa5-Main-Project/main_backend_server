package com.know_who_how.main_server.auth.service;

import com.know_who_how.main_server.auth.dto.*;
import com.know_who_how.main_server.global.util.RedisUtil; // New import
import com.know_who_how.main_server.global.entity.Keyword.UserKeyword;
import com.know_who_how.main_server.global.entity.Term.Term;
import com.know_who_how.main_server.global.entity.Term.UserTerm;
import com.know_who_how.main_server.global.entity.Token.RefreshToken;
import com.know_who_how.main_server.global.entity.User.Gender;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.jwt.JwtUtil;
import com.know_who_how.main_server.user.repository.KeywordRepository;
import com.know_who_how.main_server.user.repository.RefreshTokenRepository;
import com.know_who_how.main_server.user.repository.TermRepository;
import com.know_who_how.main_server.user.repository.UserKeywordRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import com.know_who_how.main_server.user.repository.UserTermRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserTermRepository userTermRepository;
    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SmsCertificationService smsCertificationService;
    private final RedisUtil redisUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 회원가입을 처리합니다.
     *
     * @param requestDto 회원가입 요청 DTO
     */
    @Transactional
    public void signup(UserSignupRequestDto requestDto) {
        // === 소셜 로그인 연동 처리 (signupToken이 있는 경우) ===
        String provider = "local"; // 일반 로그인 사용자의 기본 provider를 "local"로 설정
        String providerId = null;
        if (requestDto.getSignupToken() != null && !requestDto.getSignupToken().isBlank()) {
            String redisKey = "oauth-signup:" + requestDto.getSignupToken();
            String redisValue = (String) redisUtil.get(redisKey);

            if (redisValue == null) {
                throw new CustomException(ErrorCode.SIGNUP_TOKEN_INVALID);
            }

            String[] socialInfo = redisValue.split(":");
            provider = socialInfo[0];
            providerId = socialInfo[1];

            // 이미 해당 소셜 계정으로 가입된 유저가 있는지 최종 확인
            if (userRepository.findByProviderAndProviderId(provider, providerId).isPresent()) {
                throw new CustomException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_REGISTERED);
            }

            // 사용 후 토큰 삭제
            redisUtil.delete(redisKey);
        }

        // 1. verificationId를 통해 SMS 인증 정보 조회
        SmsCertificationRequestDto smsRequestDto = smsCertificationService
                .getUserVerificationData(requestDto.getVerificationId());

        // 2. 아이디 중복 확인
        userRepository.findByLoginId(requestDto.getLoginId())
                .ifPresent(user -> {
                    throw new CustomException(ErrorCode.LOGIN_ID_DUPLICATE);
                });

        // 3. 전화번호 중복 확인 (SMS 인증 정보에서 가져온 전화번호 사용)
        userRepository.findByPhoneNum(smsRequestDto.getPhoneNum())
                .ifPresent(user -> {
                    throw new CustomException(ErrorCode.PHONE_NUM_DUPLICATE);
                });

        // 4. 비밀번호 확인 (passwordConfirm 필드 제거로 인해 확인 로직 삭제)

        // 5. 약관 동의 확인 및 저장
        List<Term> requiredTerms = termRepository.findByIsRequired(true);
        for (Term requiredTerm : requiredTerms) {
            boolean isAgreed = requestDto.getTermAgreements().stream()
                    .anyMatch(ta -> ta.getTermId().equals(requiredTerm.getId()) && ta.getIsAgreed());
            if (!isAgreed) {
                throw new CustomException(ErrorCode.REQUIRED_TERM_NOT_AGREED);
            }
        }

        // 6. User 엔티티 생성
        LocalDate birth = LocalDate.parse(smsRequestDto.getBirth(), DateTimeFormatter.ISO_LOCAL_DATE);
        Gender gender = Gender.valueOf(smsRequestDto.getGender());
        InvestmentTendancy investmentTendancy = InvestmentTendancy.valueOf(requestDto.getFinancialPropensity());

        User newUser = User.builder()
                .loginId(requestDto.getLoginId())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .name(smsRequestDto.getName())
                .phoneNum(smsRequestDto.getPhoneNum())
                .birth(birth)
                .gender(gender)
                .investmentTendancy(investmentTendancy)
                .provider(provider) // 소셜 정보 추가
                .providerId(providerId) // 소셜 정보 추가
                .userMydataRegistration(false) // 새로 추가된 필드
                .build();
        userRepository.save(newUser);

        // 7. 약관 동의 저장
        List<UserTerm> userTerms = requestDto.getTermAgreements().stream()
                .filter(ta -> ta.getIsAgreed()) // 동의한 약관만 저장
                .map(ta -> {
                    Term term = termRepository.findById(ta.getTermId())
                            .orElseThrow(() -> new CustomException(ErrorCode.TERM_NOT_FOUND));
                    return UserTerm.builder()
                            .user(newUser)
                            .term(term)
                            .isAgreed(true)
                            .agreedAt(LocalDateTime.now())
                            .build();
                })
                .collect(Collectors.toList());
        userTermRepository.saveAll(userTerms);

        // 8. 키워드 저장 (은퇴 후 희망 키워드)
        if (requestDto.getKeywordIds() != null && !requestDto.getKeywordIds().isEmpty()) {
            List<UserKeyword> retirementKeywords = requestDto.getKeywordIds().stream()
                    .map(keywordId -> keywordRepository.findById(keywordId)
                            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_KEYWORD_VALUE)))
                    .map(keyword -> UserKeyword.builder().user(newUser).keyword(keyword).build())
                    .collect(Collectors.toList());
            userKeywordRepository.saveAll(retirementKeywords);
        }

        // 9. SMS 인증 정보 삭제
        smsCertificationService.removeUserVerificationData(requestDto.getVerificationId());
    }

    /**
     * 사용자 로그아웃을 처리합니다. (표준 방식 적용)
     * 1. Access Token에서 User ID 추출 (필터에서 이미 검증됨)
     * 2. RDB의 Refresh Token과 대조 후 삭제
     * 3. Access Token을 남은 유효기간만큼 블랙리스트에 추가
     *
     * @param accessToken  Access Token
     * @param refreshToken 로그아웃 요청 DTO (RefreshToken 포함)
     */
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // 1. Access Token에서 UserId 추출 (만료 여부와 상관없이)
        Long userId = jwtUtil.extractUserId(accessToken, false);

        // 2. Refresh Token이 유효한지 확인 (null 또는 빈 값인 경우)
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. RDB에 저장된 Refresh Token과 전달받은 Refresh Token이 일치하는지 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        RefreshToken storedRefreshToken = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!storedRefreshToken.getTokenValue().equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 4. RDB에서 해당 유저의 Refresh Token 삭제
        refreshTokenRepository.delete(storedRefreshToken);

        // 5. 현재 요청에 사용된 Access Token을 남은 유효시간만큼 블랙리스트에 추가
        Date expiration = jwtUtil.extractExpiration(accessToken, false);
        long remainingValidity = expiration.getTime() - System.currentTimeMillis();

        if (remainingValidity > 0) {
            redisTemplate.opsForValue().set(accessToken, "logout", Duration.ofMillis(remainingValidity));
        }
    }

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급합니다.
     *
     * @param requestDto 로그인 요청 DTO
     * @return JWT 토큰 응답 DTO
     */
    @Transactional
    public TokenResponseDto login(LoginRequestDto requestDto) {
        // 1. UsernamePasswordAuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                requestDto.getLoginId(), requestDto.getPassword());

        // 2. AuthenticationManager를 통해 인증 시도
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 성공 시 User 객체에서 정보 추출
        User user = (User) authentication.getPrincipal();
        Long userId = user.getUserId().longValue();
        List<String> authorities = user.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toList());

        // 4. JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(userId, authorities);
        String refreshToken = jwtUtil.createRefreshToken(userId);

        // 5. Refresh Token을 RDB에 저장 (기존 토큰이 있으면 업데이트, 없으면 새로 생성)
        Instant expiryDate = jwtUtil.extractExpiration(refreshToken, true).toInstant();
        Optional<RefreshToken> existingRefreshToken = refreshTokenRepository.findByUser(user);

        if (existingRefreshToken.isPresent()) {
            existingRefreshToken.get().updateToken(refreshToken, expiryDate);
            refreshTokenRepository.save(existingRefreshToken.get());
        } else {
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .user(user)
                    .tokenValue(refreshToken)
                    .expiryDate(expiryDate)
                    .build();
            refreshTokenRepository.save(newRefreshToken);
        }

        // 6. TokenResponseDto로 반환
        return TokenResponseDto.builder()
                .grantType("Bearer") // Add grantType
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Access Token을 재발급합니다.
     *
     * @param refreshToken 재발급 요청에 사용될 Refresh Token
     * @return 새로운 Access Token이 담긴 응답 DTO
     */
    @Transactional
    public TokenResponseDto reissue(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        jwtUtil.validateRefreshToken(refreshToken);

        // 2. Refresh Token에서 UserId 추출
        Long userId = jwtUtil.extractUserId(refreshToken, true);

        // 3. RDB에 저장된 Refresh Token과 일치하는지 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        RefreshToken storedRefreshToken = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!storedRefreshToken.getTokenValue().equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        // 4. Refresh Token의 만료 여부 확인 (RDB에 저장된 expiryDate 사용)
        if (storedRefreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        // 5. 사용자 정보 조회 및 새로운 Access Token 생성
        List<String> authorities = user.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toList());
        String newAccessToken = jwtUtil.createAccessToken(userId, authorities);

        // 6. 새로운 Access Token만 담아서 반환 (Refresh Token은 유지)
        return TokenResponseDto.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // 기존 Refresh Token을 그대로 반환
                .build();
    }

    /**
     * 아이디 중복 여부를 확인합니다.
     *
     * @param loginId 아이디
     * @return 사용 가능 메시지 또는 예외 발생
     */
    @Transactional(readOnly = true)
    public String checkLoginIdDuplicate(String loginId) {
        userRepository.findByLoginId(loginId)
                .ifPresent(user -> {
                    throw new CustomException(ErrorCode.LOGIN_ID_DUPLICATE);
                });
        return "사용 가능한 아이디입니다.";
    }

    /**
     * 전화번호 중복 여부를 확인합니다.
     *
     * @param phoneNum 전화번호
     * @return 사용 가능 메시지 또는 예외 발생
     */
    @Transactional(readOnly = true)
    public String checkPhoneNumDuplicate(String phoneNum) {
        userRepository.findByPhoneNum(phoneNum)
                .ifPresent(user -> {
                    throw new CustomException(ErrorCode.PHONE_NUM_DUPLICATE);
                });
        return "사용 가능한 전화번호입니다.";
    }

}
