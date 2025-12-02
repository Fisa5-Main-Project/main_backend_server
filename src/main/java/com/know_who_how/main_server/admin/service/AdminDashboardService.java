package com.know_who_how.main_server.admin.service;

import com.know_who_how.main_server.admin.dto.*;
import com.know_who_how.main_server.admin.dto.StatCardResponseDto.ChangeType;
import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final AssetsRepository assetsRepository;

    // 대시보드 상단의 주요 지표 데이터
    public List<StatCardResponseDto> getStatCardsData() {
        long totalUsers = userRepository.count();
        Long totalAssetLong = Optional.ofNullable(userRepository.sumTotalAsset()).orElse(0L);
        BigDecimal totalAsset = BigDecimal.valueOf(totalAssetLong);
        
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Long newUsersThisMonth = userRepository.countByCreatedDateAfter(startOfMonth);

        return Arrays.asList(
            new StatCardResponseDto("총 가입자 수", BigDecimal.valueOf(totalUsers), 14.2, ChangeType.increase, "vs 지난 달"),
            new StatCardResponseDto("일일 활성 사용자", new BigDecimal("2100"), 8.3, ChangeType.increase, "vs 지난 달"), // 목업 데이터 유지
            new StatCardResponseDto("신규 가입자 (이번 달)", BigDecimal.valueOf(newUsersThisMonth), 12.1, ChangeType.increase, "vs 지난 달"),
            new StatCardResponseDto("총 자산 규모", totalAsset, 5.7, ChangeType.increase, "vs 지난 달")
        );
    }

    // 사용자 증가 추이 데이터
    public List<UserGrowthResponseDto> getUserGrowthData() {
        List<UserCountByMonthDto> userCounts = userRepository.findUserCountByMonth();
        List<UserGrowthResponseDto> userGrowthData = new ArrayList<>();
        long cumulativeUsers = 0;

        for (UserCountByMonthDto dto : userCounts) {
            cumulativeUsers += dto.getCount();
            String monthName = LocalDate.of(dto.getYear(), dto.getMonth(), 1).format(DateTimeFormatter.ofPattern("MMM"));
            
            // DAU는 현재 목업 값을 유지
            long dau = (long)(cumulativeUsers * (0.4 + (Math.random() * 0.3))); // 임의의 목업 DAU

            userGrowthData.add(new UserGrowthResponseDto(
                monthName,
                cumulativeUsers,
                dto.getCount(), // 신규 가입자 (해당 월)
                dau
            ));
        }
        return userGrowthData;
    }

    // DAU 데이터 (목업) - getUserGrowthData와 동일한 데이터 구조 사용
    public List<UserGrowthResponseDto> getDauData() {
        return getUserGrowthData(); // 실제 구현에서는 별도의 DAU 집계 로직이 들어갈 수 있음
    }

    // 자산 타입별 분포 데이터
    public List<AssetDistributionResponseDto> getAssetDistributionData() {
        List<Asset> allAssets = assetsRepository.findAll();

        Map<AssetType, List<Asset>> assetsByType = allAssets.stream()
                .collect(Collectors.groupingBy(Asset::getType));

        return assetsByType.entrySet().stream()
                .map(entry -> {
                    AssetType type = entry.getKey();
                    List<Asset> assets = entry.getValue();
                    BigDecimal totalValue = assets.stream()
                            .map(Asset::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    // 사용자 수로 나누어 평균 계산
                    long userCount = assets.stream().map(Asset::getUser).distinct().count();
                    BigDecimal average = userCount > 0 ? totalValue.divide(BigDecimal.valueOf(userCount), RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    
                    return new AssetDistributionResponseDto(type.name(), totalValue, average);
                })
                .collect(Collectors.toList());
    }

    // 연령대별 평균 자산 데이터
    public List<AssetByAgeResponseDto> getAverageAssetByAgeData() {
        List<User> allUsers = userRepository.findAll();
        Map<String, List<User>> usersByAgeGroup = allUsers.stream()
                .filter(user -> user.getBirth() != null)
                .collect(Collectors.groupingBy(this::getAgeGroupFromBirth));

        return usersByAgeGroup.entrySet().stream()
                .map(entry -> {
                    String ageGroup = entry.getKey();
                    List<User> usersInGroup = entry.getValue();
                    BigDecimal totalAssetInGroup = usersInGroup.stream()
                            .map(user -> Optional.ofNullable(user.getAssetTotal()).orElse(0L))
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal averageAsset = usersInGroup.isEmpty() ? BigDecimal.ZERO :
                            totalAssetInGroup.divide(BigDecimal.valueOf(usersInGroup.size()), RoundingMode.HALF_UP);
                    
                    return new AssetByAgeResponseDto(ageGroup, averageAsset);
                })
                .collect(Collectors.toList());
    }

    // 자산 타입별 상세 통계 테이블 데이터
    public List<DetailedAssetResponseDto> getDetailedAssetData() {
        List<Asset> allAssets = assetsRepository.findAll();
        BigDecimal totalOverallAsset = allAssets.stream()
                .map(Asset::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<AssetType, List<Asset>> assetsByType = allAssets.stream()
                .collect(Collectors.groupingBy(Asset::getType));
        
        return assetsByType.entrySet().stream()
                .map(entry -> {
                    AssetType type = entry.getKey();
                    List<Asset> assets = entry.getValue();
                    BigDecimal totalAssetForType = assets.stream()
                            .map(Asset::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    long userCount = assets.stream().map(Asset::getUser).distinct().count();
                    BigDecimal averageBalance = userCount > 0 ? totalAssetForType.divide(BigDecimal.valueOf(userCount), RoundingMode.HALF_UP) : BigDecimal.ZERO;

                    double ratio = totalOverallAsset.compareTo(BigDecimal.ZERO) > 0 ?
                            totalAssetForType.divide(totalOverallAsset, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0.0;

                    return new DetailedAssetResponseDto(type.name(), totalAssetForType, averageBalance, ratio);
                })
                .collect(Collectors.toList());
    }

    // --- Helper Methods ---

    private String getAgeGroupFromBirth(User user) {
        int age = Period.between(user.getBirth(), LocalDate.now()).getYears();
        if (age < 30) return "20대 이하"; // 20대 미만 -> 20대 이하
        if (age < 40) return "30대";
        if (age < 50) return "40대";
        if (age < 60) return "50대";
        if (age < 70) return "60대"; // 60대 이상 -> 60대
        return "70대 이상"; // 70대 이상 추가
    }
}
