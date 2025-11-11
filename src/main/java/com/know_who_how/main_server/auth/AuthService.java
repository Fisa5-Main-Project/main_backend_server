package com.know_who_how.main_server.auth;

import com.know_who_how.main_server.auth.dto.LoginRequestDto;
import com.know_who_how.main_server.auth.dto.SmsCertificationRequestDto;
import com.know_who_how.main_server.auth.dto.TokenResponseDto;
import com.know_who_how.main_server.auth.dto.UserSignupRequestDto;
import com.know_who_how.main_server.auth.service.SmsCertificationService;
import com.know_who_how.main_server.global.entity.Keyword.Keyword;
import com.know_who_how.main_server.global.entity.Keyword.UserKeyword;
import com.know_who_how.main_server.global.entity.Term.Term;
import com.know_who_how.main_server.global.entity.Term.UserTerm;
import com.know_who_how.main_server.global.entity.User.Gender;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.jwt.JwtUtil;
import com.know_who_how.main_server.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserTermRepository userTermRepository;
    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SmsCertificationService smsCertificationService; // Inject SmsCertificationService
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 회원가입을 처리합니다.
     *
     * @param requestDto 회원가입 요청 DTO
     */
    @Transactional
    public void signup(UserSignupRequestDto requestDto) {
        // 1. verificationId를 통해 SMS 인증 정보 조회
        SmsCertificationRequestDto smsRequestDto = smsCertificationService.getUserVerificationData(requestDto.getVerificationId());

        // 2. 아이디 중복 확인
        if (userRepository.findByLoginId(requestDto.getLoginId()).isPresent()) {
            throw new CustomException(ErrorCode.LOGIN_ID_DUPLICATE);
        }

        // 3. 전화번호 중복 확인 (SMS 인증 정보에서 가져온 전화번호 사용)
        if (userRepository.findByPhoneNum(smsRequestDto.getPhoneNum()).isPresent()) {
            throw new CustomException(ErrorCode.PHONE_NUM_DUPLICATE);
        }

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
                .build();
        userRepository.save(newUser);

        // 7. 약관 동의 저장
        List<UserTerm> userTerms = requestDto.getTermAgreements().stream()
                .filter(ta -> ta.getIsAgreed()) // 동의한 약관만 저장
                .map(ta -> termRepository.findById(ta.getTermId())
                        .orElseThrow(() -> new CustomException(ErrorCode.TERM_NOT_FOUND)))
                .map(term -> UserTerm.builder().user(newUser).term(term).build())
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
     * 사용자 로그아웃을 처리합니다.
     * White-list 전략: Redis에서 사용자의 Refresh Token을 삭제합니다.
     * 또한, 현재 요청에 사용된 Access Token을 남은 유효시간만큼 블랙리스트에 추가합니다.
     *
     * @param accessToken     현재 사용중인 Access Token (Bearer 포함)
     * @param refreshToken    무효화할 Refresh Token
     */
    public void logout(String accessToken, String refreshToken) {
        // 1. Access Token 유효성 검사 (Filter에서 이미 처리하지만, 추가적으로 수행 가능)
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_FORMAT);
        }
        String pureAccessToken = accessToken.substring(7);
        jwtUtil.validateAccessToken(pureAccessToken);

        // 2. 현재 사용자의 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new CustomException(ErrorCode.SECURITY_CONTEXT_NOT_FOUND);
        }
        String userId = authentication.getName();

        // 3. Redis에 저장된 Refresh Token과 전달받은 Refresh Token이 일치하는지 확인
        String redisKey = "RT:" + userId;
        Object storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null) {
            throw new CustomException(ErrorCode.ALREADY_LOGGED_OUT); // 이미 로그아웃 처리됨
        }
        if (!storedToken.toString().equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 4. Redis에서 해당 유저의 Refresh Token 삭제
        redisTemplate.delete(redisKey);

        // 5. 현재 요청에 사용된 Access Token을 블랙리스트에 추가
        Date expiration = jwtUtil.extractExpiration(pureAccessToken, false);
        long remainingValidity = expiration.getTime() - System.currentTimeMillis();

        if (remainingValidity > 0) {
            redisTemplate.opsForValue().set(pureAccessToken, "logout", Duration.ofMillis(remainingValidity));
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
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(requestDto.getLoginId(), requestDto.getPassword());

        // 2. AuthenticationManager를 통해 인증 시도
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 성공 시 User 객체에서 정보 추출
        com.know_who_how.main_server.global.entity.User.User user = (com.know_who_how.main_server.global.entity.User.User) authentication.getPrincipal();
        Long userId = user.getUserId().longValue();
        List<String> authorities = user.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toList());

        // 4. JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(userId, authorities);
        String refreshToken = jwtUtil.createRefreshToken(userId);

        // 5. Refresh Token을 Redis에 저장 (userId를 key로 사용)
        Date refreshTokenExpiration = jwtUtil.extractExpiration(refreshToken, true);
        Duration refreshDuration = Duration.ofMillis(refreshTokenExpiration.getTime() - System.currentTimeMillis());
        redisTemplate.opsForValue().set("RT:" + userId, refreshToken, refreshDuration);

        // 6. TokenResponseDto로 반환
        return TokenResponseDto.builder()
                .grantType("Bearer") // Add grantType
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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
        if (userRepository.findByLoginId(loginId).isPresent()) {
            throw new CustomException(ErrorCode.LOGIN_ID_DUPLICATE);
        }
        return "사용 가능한 아이디입니다.";
    }

    @jakarta.annotation.PostConstruct
    @Transactional
    public void initTestUser() {
        if (userRepository.findByLoginId("testuser1").isEmpty()) {
            User testUser = User.builder()
                    .loginId("testuser1")
                    .password(passwordEncoder.encode("password123!"))
                    .name("홍길동")
                    .phoneNum("01011112222")
                    .birth(LocalDate.of(2001, 3, 24))
                    .gender(Gender.M)
                    .investmentTendancy(InvestmentTendancy.안정추구형)
                    .build();
            userRepository.save(testUser);
        }
    }
}
