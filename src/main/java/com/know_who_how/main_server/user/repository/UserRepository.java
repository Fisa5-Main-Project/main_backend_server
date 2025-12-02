package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.admin.dto.UserCountByMonthDto;
import com.know_who_how.main_server.global.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 아이디 중복 확인 API용
    boolean existsByLoginId(String loginId);
    Optional<User> findByLoginId(String loginId); // AuthService에서 사용

    // 전화번호 중복 확인 API용
    boolean existsByPhoneNum(String phoneNum);
    Optional<User> findByPhoneNum(String phoneNum); // AuthService에서 사용

    // OAuth2 로그인용
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // [Admin] 전체 사용자의 총 자산 합계를 계산
    @Query("SELECT SUM(u.assetTotal) FROM User u")
    Long sumTotalAsset();

    // [Admin] 특정 날짜 이후 가입한 사용자 수 계산
    Long countByCreatedDateAfter(LocalDateTime startDate);

    // [Admin] 월별 사용자 가입 수 집계
    @Query("SELECT new com.know_who_how.main_server.admin.dto.UserCountByMonthDto(YEAR(u.createdDate), MONTH(u.createdDate), COUNT(u)) " +
           "FROM User u " +
           "GROUP BY YEAR(u.createdDate), MONTH(u.createdDate) " +
           "ORDER BY YEAR(u.createdDate), MONTH(u.createdDate)")
    List<UserCountByMonthDto> findUserCountByMonth();
}
