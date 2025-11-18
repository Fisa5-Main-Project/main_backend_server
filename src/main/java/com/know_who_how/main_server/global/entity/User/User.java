package com.know_who_how.main_server.global.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * users 테이블에 매핑되는 엔티티 클래스.
 * Spring Security의 UserDetails를 구현하여 인증에 사용됩니다.
 */
@Entity
@Table(name = "users")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_num", nullable = false)
    private String phoneNum;

    @Column(nullable = false)
    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender; // 분리된 Gender Enum 사용

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "asset_total")
    private Long assetTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_tendancy")
    private InvestmentTendancy investmentTendancy;

    // === 소셜 로그인 연동 필드 ===
    @Column(name = "provider")
    private String provider; // OAuth2 제공자 (예: "kakao")

    @Column(name = "provider_id", unique = true)
    private String providerId; // OAuth2 제공자별 고유 ID

    @Column(name = "user_mydata_registration", nullable = false)
    private boolean userMydataRegistration; // 마이데이터 연동 여부 (기본값: false)

    // Spring Security 권한 (DB에 저장하지 않음)
    @Transient // DB 컬럼에 매핑하지 않음
    private List<String> roles = new ArrayList<>();

    @Builder
    public User(String loginId, String password, String phoneNum, LocalDate birth, Gender gender, String name, Long assetTotal, InvestmentTendancy investmentTendancy, String provider, String providerId, boolean userMydataRegistration) {
        this.loginId = loginId;
        this.password = password;
        this.phoneNum = phoneNum;
        this.birth = birth;
        this.gender = gender;
        this.name = name;
        this.assetTotal = assetTotal;
        this.investmentTendancy = investmentTendancy;
        this.provider = provider;
        this.providerId = providerId;
        this.userMydataRegistration = userMydataRegistration;
        this.roles.add("ROLE_USER"); // 회원가입 시 기본 권한
    }

    // 소셜 로그인 정보를 업데이트하는 메서드
    public void updateSocialInfo(String provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    public void updateAssetTotal(Long assetTotal) {
        this.assetTotal = assetTotal;
    }

    // === UserDetails 인터페이스 구현 메서드 ===

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 'roles' 리스트를 Spring Security가 인식할 수 있는 GrantedAuthority 컬렉션으로 변환
        return this.roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        // Spring Security에서 username은 loginId를 사용
        return this.loginId;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    // --- 계정 상태 관련 (기본값 true) ---

    @Override
    public boolean isAccountNonExpired() {
        // 계정이 만료되지 않았는지
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정이 잠기지 않았는지
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호가 만료되지 않았는지
        return true;
    }

    @Override
    public boolean isEnabled() {
        
        // 계정이 활성화되었는지
        return true;
    }
}