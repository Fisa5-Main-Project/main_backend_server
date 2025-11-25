package com.know_who_how.main_server.global.config;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.Keyword.Keyword;
import com.know_who_how.main_server.global.entity.Keyword.UserKeyword;
import com.know_who_how.main_server.global.entity.Asset.Pension.Pension;
import com.know_who_how.main_server.global.entity.Asset.Pension.PensionType;
import java.time.LocalDateTime;
import com.know_who_how.main_server.global.entity.User.Gender;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import com.know_who_how.main_server.user.repository.*;
import com.know_who_how.main_server.asset_management.service.AssetManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final AssetsRepository assetsRepository;
    private final PensionRepository pensionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AssetManagementService assetManagementService;

    @Override
    @Transactional
    // @Profile("local")
    public void run(String... args) throws Exception {
        // 1. 테스트 유저 생성 (없을 경우에만)
        if (userRepository.findByLoginId("testuser1").isEmpty()) {

            // A. User 생성 (기본 계정 정보)
            User testUser = User.builder()
                    .loginId("testuser1")
                    .password(passwordEncoder.encode("password123!")) // 암호화 필수
                    .name("홍길동")
                    .phoneNum("01011112222")
                    .birth(LocalDate.of(2001, 3, 24))
                    .gender(Gender.M)
                    .investmentTendancy(InvestmentTendancy.안정추구형)
                    .userMydataRegistration(true) // 필요 시 추가
                    .build();

            userRepository.save(testUser);

            // B. UserInfo 생성 (자산 관리 설문 정보)
            UserInfo userInfo = UserInfo.builder()
                    .user(testUser)
                    .goalAmount(1_000_000_000L) // 목표 10억
                    .goalTargetDate(LocalDate.now().plusYears(15)) // 15년 뒤
                    .annualIncome(60_000_000L) // 연봉 6천 (월 500)
                    .expectationMonthlyCost(2_000_000L) // 생활비 200
                    .fixedMonthlyCost(1_000_000L) // 고정비 100
                    .retirementStatus(false) // 재직 중
                    .targetRetiredAge(55)
                    .numDependents(0)
                    .mydataStatus(UserInfo.MyDataStatus.CONNECTED)
                    .build();

            userInfoRepository.save(userInfo);

            // C. 키워드 연결 (1, 6, 11, 15번)
            List<Long> keywordIds = List.of(1L, 6L, 11L, 15L);
            for (Long keywordId : keywordIds) {
                Keyword keyword = keywordRepository.findById(keywordId).orElse(null);
                if (keyword != null) {
                    UserKeyword userKeyword = UserKeyword.builder()
                            .user(testUser)
                            .keyword(keyword)
                            .build();
                    userKeywordRepository.save(userKeyword);
                }
            }

            // D. 자산(Assets) 데이터 생성 (MyData 모의 데이터)

            // 1. 입출금
            assetsRepository.save(Asset.builder()
                    .user(testUser)
                    .type(AssetType.CURRENT)
                    .balance(new BigDecimal("5000000"))
                    .bankCode("004")
                    .build());

            // 2. 예적금
            assetsRepository.save(Asset.builder()
                    .user(testUser)
                    .type(AssetType.SAVING)
                    .balance(new BigDecimal("12000000"))
                    .bankCode("004")
                    .build());

            // 3. 투자
            assetsRepository.save(Asset.builder()
                    .user(testUser)
                    .type(AssetType.INVEST)
                    .balance(new BigDecimal("25000000"))
                    .build());

            // 4. 연금 (Pension Entity 연결 필요)
            Asset pensionAsset = assetsRepository.save(Asset.builder()
                    .user(testUser)
                    .type(AssetType.PENSION)
                    .balance(new BigDecimal("150000000"))
                    .build());

            // 5. 자동차
            assetsRepository.save(Asset.builder()
                    .user(testUser)
                    .type(AssetType.AUTOMOBILE)
                    .balance(new BigDecimal("30000000"))
                    .build());

            // 6. 부동산
            assetsRepository.save(Asset.builder()
                    .user(testUser)
                    .type(AssetType.REAL_ESTATE)
                    .balance(new BigDecimal("8000000"))
                    .build());

            // 7. 대출
            assetsRepository.save(Asset.builder()
                    .user(testUser)
                    .type(AssetType.LOAN)
                    .balance(new BigDecimal("30000000"))
                    .build());

            // E. Pension 데이터 생성
            pensionRepository.save(Pension.builder()
                    .asset(pensionAsset)
                    .updatedAt(LocalDateTime.now())
                    .pensionType(PensionType.DC)
                    .accountName("우리은행 개인형IRP")
                    .principal(new BigDecimal("150000000.00"))
                    .personalContrib(new BigDecimal("10000000.00"))
                    .contribYear(2023)
                    .totalPersonalContrib(new BigDecimal("50000000.00"))
                    .build());

            // F. Asset Total 계산 및 업데이트 (자산 - 대출)
            // Service 로직을 통해 계산 및 업데이트
            assetManagementService.calculateAndUpdateAssetTotal(testUser);

            System.out.println("========== [INIT] 테스트 유저(testuser1) 생성 완료 ==========");
        }
    }
}
