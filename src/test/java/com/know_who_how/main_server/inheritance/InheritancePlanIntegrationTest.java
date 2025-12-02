package com.know_who_how.main_server.inheritance;

import com.know_who_how.main_server.global.entity.User.Gender;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.inheritance.service.EmailService;
import com.know_who_how.main_server.inheritance.service.InheritanceService;
import com.know_who_how.main_server.inheritance.service.S3Service;
import com.know_who_how.main_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// 상속 계획 관리 테스트
// 목표: POST /plan, GET /plan 로직 검증
@ActiveProfiles("test") //application-test.yml 쓰도록
@SpringBootTest
@Transactional
public class InheritancePlanIntegrationTest {

    // 테스트할 메인 서비스
    @Autowired
    private InheritanceService inheritanceService;

    @Autowired
    private UserRepository userRepository;

    // DB와 무관한 외부 통신은 MockBean 처리(S3 통신 방지)
    @MockBean
    private S3Service  s3Service;

    // 예약 발송 테스트 시 실제 메일 발송 방지
    @MockBean
    private EmailService emailService;

    private Long testUserId;

    @BeforeEach
    void setup(){
        User user = User.builder()
                .loginId("testuser")
                .password("encodedPassword123")
                .phoneNum("01020002000")
                .birth(LocalDate.of(1990, 1,1))
                .gender(Gender.M)
                .name("테스터")
                .assetTotal(100000L)
                .investmentTendancy(InvestmentTendancy.공격투자형)
                .build(); // 마이데이터랑 상속 여부는 기본값 null, false 사용

        userRepository.save(user);
        testUserId = user.getUserId();

        assertFalse(userRepository.findById().get().isUserInheritanceRegistration())
    }
}
