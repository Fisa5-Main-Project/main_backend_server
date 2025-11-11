package com.know_who_how.main_server.auth;

import com.know_who_how.main_server.auth.dto.CheckIdRequestDto;
import com.know_who_how.main_server.auth.dto.LoginRequestDto;
import com.know_who_how.main_server.auth.dto.TokenResponseDto;
import com.know_who_how.main_server.auth.dto.UserSignupRequestDto;
import com.know_who_how.main_server.global.entity.Keyword.Keyword;
import com.know_who_how.main_server.global.entity.Keyword.KeywordType;
import com.know_who_how.main_server.global.entity.Keyword.UserKeyword;
import com.know_who_how.main_server.global.entity.Term.Term;
import com.know_who_how.main_server.global.entity.Term.UserTerm;
import com.know_who_how.main_server.global.entity.User.Gender;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.jwt.JwtUtil;
import com.know_who_how.main_server.user.repository.KeywordRepository;
import com.know_who_how.main_server.user.repository.TermRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import com.know_who_how.main_server.user.repository.UserTermRepository;
import com.know_who_how.main_server.user.repository.UserKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository; // Assuming this is for finding user by loginId
    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserTermRepository userTermRepository;
    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    /**
     * 회원가입을 처리합니다.
     *
     * @param requestDto 회원가입 요청 DTO
     * @return 생성된 사용자 ID
     */
    @Transactional
    public Integer signup(UserSignupRequestDto requestDto) {
        // 1. 아이디 중복 확인
        if (userRepository.findByLoginId(requestDto.getLoginId()).isPresent()) {
            throw new CustomException(ErrorCode.LOGIN_ID_DUPLICATE);
        }

        // 2. 전화번호 중복 확인
        if (userRepository.findByPhoneNum(requestDto.getPhoneNum()).isPresent()) {
            throw new CustomException(ErrorCode.PHONE_NUM_DUPLICATE);
        }

        // 3. 비밀번호 확인
        if (!requestDto.getPassword().equals(requestDto.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 4. 약관 동의 확인 및 저장
        List<Term> requiredTerms = termRepository.findByIsRequired(true);
        for (Term requiredTerm : requiredTerms) {
            if (!requestDto.getAgreedTermIds().contains(requiredTerm.getId())) {
                throw new CustomException(ErrorCode.REQUIRED_TERM_NOT_AGREED);
            }
        }

        // 5. User 엔티티 생성
        LocalDate birthDate = LocalDate.parse(requestDto.getBirthDate(), DateTimeFormatter.BASIC_ISO_DATE);
        Gender gender = (requestDto.getGenderDigit() % 2 == 1) ? Gender.M : Gender.F; // 1,3 남성 / 2,4 여성

        User newUser = User.builder()
                .loginId(requestDto.getLoginId())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .name(requestDto.getName())
                .phoneNum(requestDto.getPhoneNum())
                .birth(birthDate)
                .gender(gender)
                .telecom(requestDto.getTelecom()) // User 엔티티에 telecom 필드 추가 필요
                .build();
        userRepository.save(newUser);

        // 6. 약관 동의 저장
        List<UserTerm> userTerms = requestDto.getAgreedTermIds().stream()
                .map(termId -> termRepository.findById(termId)
                        .orElseThrow(() -> new CustomException(ErrorCode.TERM_NOT_FOUND)))
                .map(term -> UserTerm.builder().user(newUser).term(term).build())
                .collect(Collectors.toList());
        userTermRepository.saveAll(userTerms);

        // 7. 키워드 저장
        // 투자 성향 키워드 (단일 선택)
        Keyword investmentKeyword = keywordRepository.findById(requestDto.getInvestmentKeywordId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_KEYWORD_VALUE));
        if (!investmentKeyword.getType().equals(KeywordType.INVESTMENT)) {
            throw new CustomException(ErrorCode.INVALID_KEYWORD_VALUE); // 타입 불일치
        }
        userKeywordRepository.save(UserKeyword.builder().user(newUser).keyword(investmentKeyword).build());

        // 은퇴 후 희망 키워드 (중복 선택)
        if (requestDto.getRetirementKeywordIds() != null && !requestDto.getRetirementKeywordIds().isEmpty()) {
            List<UserKeyword> retirementUserKeywords = requestDto.getRetirementKeywordIds().stream()
                    .map(keywordId -> keywordRepository.findById(keywordId)
                            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_KEYWORD_VALUE)))
                    .filter(keyword -> keyword.getType().equals(KeywordType.RETIREMENT)) // 은퇴 키워드만 필터링
                    .map(keyword -> UserKeyword.builder().user(newUser).keyword(keyword).build())
                    .collect(Collectors.toList());
            if (retirementUserKeywords.size() != requestDto.getRetirementKeywordIds().size()) {
                throw new CustomException(ErrorCode.INVALID_KEYWORD_VALUE); // 유효하지 않은 키워드 ID가 포함됨
            }
            userKeywordRepository.saveAll(retirementUserKeywords);
        }

        return newUser.getUserId();
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

        // 5. TokenResponseDto로 반환
        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 아이디 중복 여부를 확인합니다.
     *
     * @param requestDto 아이디 중복 확인 요청 DTO
     * @return 중복 여부 (true: 중복, false: 사용 가능)
     */
    @Transactional(readOnly = true)
    public boolean checkLoginIdDuplicate(CheckIdRequestDto requestDto) {
        return userRepository.findByLoginId(requestDto.getLoginId()).isPresent();
    }
}
