package com.know_who_how.main_server.user;

import com.know_who_how.main_server.global.entity.Term.Term;
import com.know_who_how.main_server.global.entity.Term.UserTerm;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.user.dto.TermAgreementRequest;
import com.know_who_how.main_server.user.dto.UserSignupRequest;
import com.know_who_how.main_server.user.repository.TermRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import com.know_who_how.main_server.user.repository.UserTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserTermRepository  userTermRepository;

    private final PasswordEncoder passwordEncoder;

    // 로그인 ID 중복 검사
    @Override
    public void checkLoginIdDuplicate(String loginId) {
        // 로그인 ID 중복 시 예외 처리
        if(userRepository.existsByLoginId(loginId)){
            throw new CustomException(ErrorCode.LOGIN_ID_DUPLICATE);
        }
        // 존재하지 않으면 예외 없이 정상 종료
    }

    // 전화번호 중복 검사
    @Override
    public void checkPhoneNumDuplicate(String phoneNum) {
        // 전화 번호 중복 시 예외 처리
        if(userRepository.existsByPhoneNum(phoneNum)){
            throw new CustomException(ErrorCode.PHONE_NUM_DUPLICATE);
        }
        // 존재하지 않으면 예외 없이 정상 종료
    }

    // 회원 가입 로직
    @Override
    @Transactional
    public void registerUser(UserSignupRequest request) {
        // 회원 검증
        validateSignupRequest(request);
        // 회원 등록
        User user = createUser(request);

        createTerms(user, request.getTermAgreements());
    }


    // 회원 가입 로직 검증
    private void validateSignupRequest(UserSignupRequest request) {
        // 로그인 중복 검증
        checkLoginIdDuplicate(request.getLoginId());
        // 전화 번호 중복 검증
        checkPhoneNumDuplicate(request.getPhoneNum());

        // 약관 검증
        validateMandatoryTerms(request.getTermAgreements());
    }

    // 약관 검증
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
                throw new CustomException(ErrorCode.REQUIRED_TERM_NOT_AGREED);
            }
        }
    }

    // 회원 등록
    private User createUser(UserSignupRequest request) {

        User user = User.builder().
                loginId(request.getLoginId()).
                password(passwordEncoder.encode(request.getPassword())).
                phoneNum(request.getPhoneNum()).
                birth(request.getBirth()).
                gender(request.getGender()).
                name(request.getName()).
                job(request.getJob()).
                build();

        return userRepository.save(user);
    }

    // 회원과 약관 매핑
    private void createTerms(User savedUser, List<TermAgreementRequest> termDtos) {
        for (TermAgreementRequest termDto : termDtos) {
            // DTO의 termId로 실제 Term 엔티티 조회
            Term term = termRepository.findById(termDto.getTermId())
                    .orElseThrow(() -> new CustomException(ErrorCode.TERM_NOT_FOUND) );

            UserTerm userTerm = UserTerm.builder()
                    .user(savedUser)
                    .term(term)
                    .isAgreed(termDto.getIsAgreed())
                    .agreedAt(LocalDateTime.now())
                    .build();

            userTermRepository.save(userTerm);
        }
    }

}
