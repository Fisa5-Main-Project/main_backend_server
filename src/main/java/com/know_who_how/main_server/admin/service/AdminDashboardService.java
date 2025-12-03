package com.know_who_how.main_server.admin.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.know_who_how.main_server.admin.dto.*;
import com.know_who_how.main_server.admin.dto.StatCardResponseDto.ChangeType;
import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final AssetsRepository assetsRepository;
    private final ElasticsearchClient elasticsearchClient;

    public List<StatCardResponseDto> getStatCardsData() {
        long totalUsers = userRepository.count();
        Long totalAssetLong = Optional.ofNullable(userRepository.sumTotalAsset()).orElse(0L);
        BigDecimal totalAsset = BigDecimal.valueOf(totalAssetLong);
        
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Long newUsersThisMonth = userRepository.countByCreatedDateAfter(startOfMonth);

        long dau = 0;
        try {
            dau = getDauCount();
        } catch (IOException e) {
            log.error("Elasticsearch에서 DAU를 가져오는 데 실패했습니다: {}", e.getMessage());
            dau = 0;
        }

        // DAU 계산 후 값 확인용 로그
        log.info("DAU Count from getDauCount(): {}", dau);

        return Arrays.asList(
            new StatCardResponseDto("총 가입자 수", BigDecimal.valueOf(totalUsers), 14.2, ChangeType.increase, "vs 지난 달"),
            new StatCardResponseDto("일일 활성 사용자", BigDecimal.valueOf(dau), 8.3, ChangeType.increase, "vs 지난 달"),
            new StatCardResponseDto("신규 가입자 (이번 달)", BigDecimal.valueOf(newUsersThisMonth), 12.1, ChangeType.increase, "vs 지난 달"),
            new StatCardResponseDto("총 자산 규모", totalAsset, 5.7, ChangeType.increase, "vs 지난 달")
        );
    }

    public List<UserGrowthResponseDto> getUserGrowthData() {
        List<UserCountByMonthDto> userCounts = userRepository.findUserCountByMonth();
        Map<String, Double> monthlyDauMap = new HashMap<>();
        try {
            monthlyDauMap = getMonthlyAverageDau();
        } catch (IOException e) {
            log.error("Elasticsearch에서 월별 평균 DAU를 가져오는 데 실패했습니다: {}", e.getMessage());
        }

        List<UserGrowthResponseDto> userGrowthData = new ArrayList<>();
        long cumulativeUsers = 0;

        for (UserCountByMonthDto dto : userCounts) {
            cumulativeUsers += dto.getCount();
            String monthKey = String.format("%d-%02d", dto.getYear(), dto.getMonth());
            String monthName = LocalDate.of(dto.getYear(), dto.getMonth(), 1).format(DateTimeFormatter.ofPattern("MMM"));
            
            long dau = monthlyDauMap.getOrDefault(monthKey, 0.0).longValue();

            userGrowthData.add(new UserGrowthResponseDto(
                monthName,
                cumulativeUsers,
                dto.getCount(),
                dau
            ));
        }
        return userGrowthData;
    }

    public List<AssetDistributionResponseDto> getAssetDistributionData() {
        List<Asset> allAssets = assetsRepository.findAll();

        Map<AssetType, List<Asset>> assetsByType = allAssets.stream()
                .collect(Collectors.groupingBy(Asset::getType));

        return Arrays.stream(AssetType.values())
                .map(type -> {
                    List<Asset> assets = assetsByType.getOrDefault(type, Collections.emptyList());
                    BigDecimal totalValue = assets.stream()
                            .map(asset -> type == AssetType.LOAN ? asset.getBalance().abs() : asset.getBalance())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long userCount = assets.stream().map(Asset::getUser).distinct().count();
                    BigDecimal average = userCount > 0 ? totalValue.divide(BigDecimal.valueOf(userCount), RoundingMode.HALF_UP) : BigDecimal.ZERO;

                    return new AssetDistributionResponseDto(type.name(), totalValue, average);
                })
                .collect(Collectors.toList());
    }

    public List<AssetByAgeResponseDto> getAverageAssetByAgeData() {
        List<User> allUsers = userRepository.findAll();
        Map<String, List<User>> usersByAgeGroup = allUsers.stream()
                .filter(user -> user.getBirth() != null)
                .collect(Collectors.groupingBy(this::getAgeGroupFromBirth));

        List<AssetByAgeResponseDto> result = usersByAgeGroup.entrySet().stream()
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
        
        // 연령대 순서로 정렬
        final List<String> ageGroupOrder = Arrays.asList("20대 이하", "30대", "40대", "50대", "60대", "70대 이상");
        result.sort(Comparator.comparingInt(dto -> ageGroupOrder.indexOf(dto.getAgeGroup())));

        return result;
    }
    
    public List<DetailedAssetResponseDto> getDetailedAssetData() {
        List<Asset> allAssets = assetsRepository.findAll();
        BigDecimal totalOverallAsset = allAssets.stream()
                .map(asset -> asset.getType() == AssetType.LOAN ? asset.getBalance().abs() : asset.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<AssetType, List<Asset>> assetsByType = allAssets.stream()
                .collect(Collectors.groupingBy(Asset::getType));
        
        return Arrays.stream(AssetType.values())
                .map(type -> {
                    List<Asset> assets = assetsByType.getOrDefault(type, Collections.emptyList());
                    BigDecimal totalAssetForType = assets.stream()
                            .map(asset -> type == AssetType.LOAN ? asset.getBalance().abs() : asset.getBalance())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    long userCount = assets.stream().map(Asset::getUser).distinct().count();
                    BigDecimal averageBalance = userCount > 0 ? totalAssetForType.divide(BigDecimal.valueOf(userCount), RoundingMode.HALF_UP) : BigDecimal.ZERO;

                    double ratio = totalOverallAsset.compareTo(BigDecimal.ZERO) > 0 ?
                            totalAssetForType.divide(totalOverallAsset, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0.0;

                    return new DetailedAssetResponseDto(type.name(), totalAssetForType, averageBalance, ratio);
                })
                .collect(Collectors.toList());
    }

    private String getAgeGroupFromBirth(User user) {
        int age = Period.between(user.getBirth(), LocalDate.now()).getYears();
        if (age < 30) return "20대 이하";
        if (age < 40) return "30대";
        if (age < 50) return "40대";
        if (age < 60) return "50대";
        if (age < 70) return "60대";
        return "70대 이상";
    }

    private long getDauCount() throws IOException {
        ZoneId kstZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime todayStartKst = LocalDate.now(kstZoneId).atStartOfDay(kstZoneId);
        ZonedDateTime tomorrowStartKst = todayStartKst.plusDays(1);

        long startTime = todayStartKst.toInstant().toEpochMilli();
        long endTime = tomorrowStartKst.toInstant().toEpochMilli();

                Query timestampRangeQuery = Query.of(q -> q

                        .range(r -> r

                                .field("@timestamp")

                                .gte(JsonData.of(startTime))

                                .lt(JsonData.of(endTime))

                        )

                );
        Query messageTermQuery = Query.of(q -> q.term(t -> t.field("message.keyword").value("USER_LOGIN_SUCCESS")));
        Query finalQuery = Query.of(q -> q.bool(b -> b.filter(timestampRangeQuery, messageTermQuery)));
        Aggregation uniqueUsersAggregation = Aggregation.of(a -> a.cardinality(c -> c.field("ActiveUserId.keyword")));
        
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("know-who-how-logs-*")
                .size(0)
                .query(finalQuery)
                .aggregations("unique_users", uniqueUsersAggregation)
                .build();

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        // Elasticsearch 쿼리 결과 원본 확인용 로그
        log.info("Elasticsearch Raw Response: {}", response.toString());

        Aggregate aggregate = response.aggregations().get("unique_users");
        if (aggregate != null && aggregate.isCardinality()) {
            return aggregate.cardinality().value();
        }
        return 0;
    }

    /**
     * 월별 평균 DAU(일일 활성 사용자)를 계산합니다.
     * Elasticsearch에서 일별 DAU를 조회한 후, Java 코드에서 월별 평균을 집계합니다.
     *
     * @return 월별("yyyy-MM" 형식) 평균 DAU 맵
     * @throws IOException Elasticsearch 쿼리 실행 중 발생할 수 있는 예외
     */
    private Map<String, Double> getMonthlyAverageDau() throws IOException {
        Query messageQuery = Query.of(q -> q.term(t -> t.field("message.keyword").value("USER_LOGIN_SUCCESS")));

        // 1. Elasticsearch 쿼리: 지정된 메시지를 가진 로그를 일별로 집계하여 각 일의 고유 사용자 수(DAU)를 계산합니다.
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("know-who-how-logs-*") // Logstash에서 사용한 인덱스 패턴
                .size(0) // 문서 자체는 필요 없고 집계 결과만 필요
                .query(messageQuery) // USER_LOGIN_SUCCESS 메시지를 가진 로그만 필터링
                .aggregations("dau_over_time", a -> a // 집계 이름: dau_over_time
                        .dateHistogram(h -> h // 날짜 히스토그램 집계 (일별)
                                .field("@timestamp")
                                .calendarInterval(CalendarInterval.Day) // 일별로 버킷 생성
                                .format("yyyy-MM-dd") // 버킷 키 포맷
                        )
                        .aggregations("unique_users", subAgg -> subAgg // 각 일별 버킷 내에서 고유 사용자 수 계산
                                .cardinality(c -> c.field("ActiveUserId.keyword")) // ActiveUserId 필드의 고유 값 세기
                        )
                )
                .build();

        log.info("Elasticsearch Daily DAU Query for Monthly Average: {}", searchRequest.toString());

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);
        
        // 2. Java 코드에서 월별 평균 DAU 계산
        // Elasticsearch 응답에서 일별 DAU 데이터를 추출하여 월별로 그룹화합니다.
        Map<String, List<Long>> dailyDauByMonth = new HashMap<>();
        Aggregate byDayAgg = response.aggregations().get("dau_over_time"); // 일별 DAU 집계 결과 가져오기

        if (byDayAgg != null && byDayAgg.isDateHistogram()) {
            for (DateHistogramBucket dayBucket : byDayAgg.dateHistogram().buckets().array()) {
                String dayKey = dayBucket.keyAsString().substring(0, 7); // "yyyy-MM-dd"에서 "yyyy-MM" 형식으로 월 추출
                Aggregate uniqueUsersAgg = dayBucket.aggregations().get("unique_users");
                if (uniqueUsersAgg != null && uniqueUsersAgg.isCardinality()) {
                    long dau = uniqueUsersAgg.cardinality().value();
                    dailyDauByMonth.computeIfAbsent(dayKey, k -> new ArrayList<>()).add(dau);
                }
            }
        }

        // 3. 월별 평균 DAU 계산
        // 각 월에 대해 일별 DAU 값들의 평균을 계산합니다.
        return dailyDauByMonth.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // 월 (yyyy-MM)
                        entry -> entry.getValue().stream() // 해당 월의 일별 DAU 목록
                                .mapToLong(Long::longValue)
                                .average() // 평균 계산
                                .orElse(0.0) // 일별 DAU가 없는 경우 0.0
                ));
    }
}
