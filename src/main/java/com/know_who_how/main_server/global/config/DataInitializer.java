package com.know_who_how.main_server.global.config;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.Asset.Pension.Pension;
import com.know_who_how.main_server.global.entity.Asset.Pension.PensionType;
import com.know_who_how.main_server.global.entity.User.Gender;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.PensionRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AssetsRepository assetsRepository;
    private final PensionRepository pensionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initTestUser();
    }

    private void initTestUser() {
        // 1. Create User if not exists
        User user = userRepository.findByLoginId("testuser1").orElseGet(() -> {
            User newUser = User.builder()
                    .loginId("testuser1")
                    .password(passwordEncoder.encode("password123!"))
                    .name("홍길동")
                    .phoneNum("01011112222")
                    .birth(LocalDate.of(2001, 3, 24))
                    .gender(Gender.M)
                    .investmentTendancy(InvestmentTendancy.적극투자형)
                    .userMydataRegistration(true)
                    .build();
            return userRepository.save(newUser);
        });

//         2. Create Assets if not exists
        if (assetsRepository.findByUser(user).isEmpty()) {
            createAsset(user, AssetType.CURRENT, new BigDecimal("5000000"), "002");
            createAsset(user, AssetType.SAVING, new BigDecimal("1200000"), "002");
            createAsset(user, AssetType.INVEST, new BigDecimal("25000000"), "240");
            Asset pensionAsset = createAsset(user, AssetType.PENSION, new BigDecimal("150000000"), "001");
            createAsset(user, AssetType.AUTOMOBILE, new BigDecimal("30000000"), null);
            createAsset(user, AssetType.REAL_ESTATE, new BigDecimal("800000000"), null);
            createAsset(user, AssetType.LOAN, new BigDecimal("300000000"), "011");

            // 3. Create Pension Data linked to the PENSION asset
            createPension(pensionAsset);
        }
    }

    private Asset createAsset(User user, AssetType type, BigDecimal balance, String bankCode) {
        return assetsRepository.save(Asset.builder()
                .user(user)
                .type(type)
                .balance(balance)
                .bankCode(bankCode)
                .build());
    }

    private void createPension(Asset asset) {
        pensionRepository.save(Pension.builder()
                .asset(asset)
                .updatedAt(LocalDateTime.now())
                .pensionType(PensionType.DC)
                .accountName("우리은행 개인형IRP")
                .principal(new BigDecimal("150000000.00"))
                .personalContrib(new BigDecimal("10000000.00"))
                .contribYear(2023)
                .totalPersonalContrib(new BigDecimal("50000000.00"))
                .build());
    }
}