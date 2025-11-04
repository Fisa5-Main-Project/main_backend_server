package com.know_who_how.main_server.auth;

import com.know_who_how.main_server.auth.dto.AuthSignupRequest;
import com.know_who_how.main_server.auth.dto.TermAgreementRequest;
import com.know_who_how.main_server.auth.dto.UserKeywordsRequest;
import com.know_who_how.main_server.auth.repository.TermRepository;
import com.know_who_how.main_server.auth.repository.UserKeywordRepository;
import com.know_who_how.main_server.auth.repository.UserRepository;
import com.know_who_how.main_server.auth.repository.UserTermRepository;
import com.know_who_how.main_server.global.entity.User.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final UserTermRepository userTermRepository;
    private final TermRepository termRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 로직
     */
    @Override
    public void registerUser(AuthSignupRequest request) {
        validateSignUp(request);
        User savedUser = createUser(request);

        createKeywords(savedUser, request.getKeywords());

        createTerms(savedUser, request.getTermAgreements());
    }

    /**
     * 로그인 ID 중복 검사
     */
    @Override
    public void checkLoginIdDuplicate(String loginId) {
        if(userRepository.existByLoginId(loginId)){
            throw new SignupValidationException("409_DUP_ID", "이미 사용 중인 아이디입니다.");
        }
        // 존재하지 않으면 예외 없이 정상 종료
    }

    /**
     * 전화번호 중복 검사
     */
    @Override
    public void checkPhoneNumDuplicate(String phoneNum) {
        if(userRepository.existsByPhoneNum(phoneNum)){
            throw new SignupValidationException("409_DUP_PHONE", "이미 등록된 전화번호입니다.");
        }
        // 존재하지 않으면 예외 없이 정상 종료
    }

    // ------- private 함수들 -------

    /**
     * registerUser 내부에서 사용할 최종 검증 로직
     */
    private void validateSignUp(AuthSignupRequest request) throws SignupValidationException {
        if(userRepository.existByLoginId(request.getLoginId())){
            throw new SignupValidationException("409_DUP_ID", "이미 사용 중인 아이디입니다.");
        }
        if(userRepository.existsByPhoneNum(request.getPhoneNum())){
            throw new SignupValidationException("409_DUP_PHONE", "이미 등록된 전화번호입니다.");
        }

        /**
         * 필수 약관 검증 로직
         */
        validateMandatoryTerms(request.getTermAgreements());

    }

    /**
     * 필수 약관 검증 로직
     */
    private void validateMandatoryTerms(List<TermAgreementRequest> userAgreements) {
        List<Term> mandatoryTerms = termRepository.findByIsRequired(true);

        Map<Long, Boolean> userAgreementMap = userAgreements.stream()
                .collect(Collectors.toMap(
                        TermAgreementRequest::getTermId,
                        TermAgreementRequest::getIsAgreed
                ));

        for (Term mandatoryTerm : mandatoryTerms) {
            Long termId = mandatoryTerm.getId();

            if(!userAgreementMap.containsKey(termId) || !userAgreementMap.get(termId)){
                throw new SignupValidationException(
                        "400_TERM_REQUIRED",
                        "필수 약관("+ mandatoryTerm.getTermName()+")에 동의해야 합니다."
                );
            }
        }
    }

    /**
     * USER 등록
     */
    private User createUser(AuthSignupRequest request) {

        User user = new User();

        user.setLoginId(request.getLoginId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNum(request.getPhoneNum());
        user.setBirth(request.getBirth());
        user.setJob(request.getJob());
        user.setName(request.getName());
        user.setGender(Gender.valueOf(request.getGender().toUpperCase()));

        return userRepository.save(user);
    }

    /**
     * UserKeyword 엔티티 생성 및 저장 (키워드 개수만큼)
     */
    private void createKeywords(User savedUser, UserKeywordsRequest keywordsDto) {

        List<String> keywordStrings = keywordsDto.getRetirementKeywords();
        String investmentTendency = keywordsDto.getInvestmentTendency();

        // 키워드 리스트를 순회하며 UserKeyword 엔티티 생성
        for (String keywordStr : keywordStrings) {
            UserKeyword userKeyword = new UserKeyword();

            userKeyword.setUser(savedUser);
            userKeyword.setRetirementKeyword(RetirementKeyword.valueOf(keywordStr));
            userKeyword.setInvestmentTendency(investmentTendency);
            userKeywordRepository.save(userKeyword);
        }
    }

    /**
     * UserTerm 엔티티 생성 및 저장 (약관 개수만큼)
     */
    private void createTerms(User savedUser, List<TermAgreementRequest> termDtos) {
        for (TermAgreementRequest termDto : termDtos) {
            // DTO의 termId로 실제 Term 엔티티 조회
            Term term = termRepository.findById(termDto.getTermId())
                    .orElseThrow(() -> new SignupValidationException(
                            "400_TERM_ID", "존재하지 않는 약관 ID입니다: " + termDto.getTermId()
                    ));

            UserTerm userTerm = new UserTerm();
            userTerm.setUser(savedUser);
            userTerm.setTerm(term);
            userTerm.setIsAgreed(termDto.getIsAgreed());
            userTerm.setAgreedAt(termDto.getIsAgreed()?LocalDateTime.now():null);

            userTermRepository.save(userTerm);
        }
    }
}
