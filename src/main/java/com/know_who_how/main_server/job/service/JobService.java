package com.know_who_how.main_server.job.service;

import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.util.RedisUtil;
import com.know_who_how.main_server.job.client.JobOpenApiClient;
import com.know_who_how.main_server.job.dto.*;
import com.know_who_how.main_server.job.dto.external.ExternalApiResponse;
import com.know_who_how.main_server.job.dto.external.ExternalJobDetailItemWrapper;
import com.know_who_how.main_server.job.dto.external.ExternalJobListItems;
import com.know_who_how.main_server.job.type.ApplyMethod;
import com.know_who_how.main_server.job.type.EmploymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


// API 호출, Redis 캐싱, 데이터 조합 등 비즈니스 로직
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobOpenApiClient apiClient;
    private final RedisUtil redisUtil;

    // Redis 키 접두사
    private static final String KEY_JOB_LIST = "jobs:list:"; // 목록 페이지 전체 캐시
        // ex. jobs:list:search:서울:emp:CM0101:p:1:s:10
    private static final String KEY_JOB_DETAIL = "job:detail:"; // 상세 페이지 전체 캐시
        // ex. job:detail:{jobId}
    private static final String KEY_JOB_EXTRA = "job:extra:"; // 고용형태, 직종 캐시용
        // ex. job:extra:{jobId}
        // getJobList(목록 조회)가 캐시해두면 getJobDetail(상세 조회)에서 사용


    // 1. 채용공고 리스트 조회
    public JobListResponseDto getJobList(String search, String empType, int page, int size) {
        String cacheKey = String.format("%ssearch:%s:emp:%s:p:%d:s:%d",
                KEY_JOB_LIST, search, empType, page, size);

        // 1. 캐시 확인 (유지)
        JobListResponseDto cachedData = (JobListResponseDto) redisUtil.get(cacheKey);
        if (cachedData != null) {
            // 캐시된 값이 있으면 Open API 호출 없이 해당 값으로 반환
            log.info("[Cache HIT] Key: {}", cacheKey);
            return cachedData;
        }

        log.info("[Cache MISS] Key: {}", cacheKey);

        // 2. Open API 호출
        ExternalApiResponse<ExternalJobListItems> openApiDataWrapper = apiClient.fetchJobs(search, empType, page, size);

        // openApiDataWrapper에서 body만 꺼내서 openApiData에 담기.
        // (openApiDataWrappper == null이면 NullPointerException 나는걸 방지)
        ExternalJobListItems openApiData = (openApiDataWrapper != null) ? openApiDataWrapper.getBody() : null;


        if (openApiData == null || openApiData.getItem() == null) {
            log.warn("Open API 'getJobList' 응답이 비어있거나 'item' 리스트가 null입니다.");
            // 빈 응답 객체 반환
            return new JobListResponseDto(
                    PaginationDto.builder().totalCount(0).currentPage(page).itemsPerPage(size).totalPages(0).build(),
                    Collections.emptyList()
            );
        }

        // 실제 리스트 가져오기
        List<OpenApiJobItem> items = openApiData.getItem(); // 위에서 null 체크 했으므로 바로 사용

        // 3. 상세보기를 위한 '고용형태', '직종' 정보 캐싱
        cacheExtraJobData(items);

        // 4. 클라이언트 응답 DTO로 매핑
        JobListResponseDto responseDto = mapToJobListDto(openApiData, items, page, size);

        // 5. 리스트 결과 캐시 저장 (10분) - 최신성이 중요하고 자주 바뀌기 때문에
        redisUtil.save(cacheKey, responseDto, Duration.ofMinutes(10));

        return responseDto;
    }


    // 2. 채용 공고 상세 조회
    public JobDetailResponseDto getJobDetail(String jobId) {
        String cacheKey = KEY_JOB_DETAIL + jobId;

        // 1. 상세보기 캐시 확인
        JobDetailResponseDto cachedData = (JobDetailResponseDto) redisUtil.get(cacheKey);
        if (cachedData != null) {
            log.info("[Cache HIT] Key: {}", cacheKey);
            return cachedData;
        }

        log.info("[Cache MISS] Key: {}", cacheKey);

        // 2. [API 호출] 상세보기 API 호출
        ExternalApiResponse<ExternalJobDetailItemWrapper> detailWrapper = apiClient.fetchJobDetail(jobId);

        if (detailWrapper == null || detailWrapper.getBody() == null || detailWrapper.getBody().getItem() == null) {
            // 상세보기 API가 데이터를 못 찾는 경우 JOB_NOT_FOUND
            throw new CustomException(ErrorCode.JOB_NOT_FOUND);
        }
        OpenApiDetailItem detailItem = detailWrapper.getBody().getItem();


        // 3. [Redis 조회] 리스트 조회 시 캐싱해둔 '고용형태', '직종' 데이터 조회
        String extraDataKey = KEY_JOB_EXTRA + jobId;
        JobExtraDataDto extraData = (JobExtraDataDto) redisUtil.get(extraDataKey);

        JobExtraDataDto transformedExtraData;

        if (extraData == null) {
            // 사용자가 리스트 조회를 거치지 않고 상세 URL로 바로 접근한 경우
            // '고용 형태', '직종'은 정보 없음으로 처리
            log.warn("[Cache MISS] 'job:extra' 데이터 없음. Key: {}", extraDataKey);
            transformedExtraData = new JobExtraDataDto(EmploymentType.UNKNOWN.getDisplayName(), "정보 없음");
        }else{
            log.info("[Cache HIT] Key: {}", extraDataKey);
            transformedExtraData = JobExtraDataDto.builder()
                    .employmentType(mapEmploymentType(extraData.getEmploymentType()))
                    .jobCategory(extraData.getJobCategory())
                    .build();

        }

        // 4. 2개 데이터(API 응답 + Redis 캐시)를 조합하여 DTO 매핑
        JobDetailResponseDto responseDto = mapToJobDetailDto(detailItem, transformedExtraData);

        // 5. 최종 DTO 캐시 저장 (1시간)  - 앱 사용할 동안 불편함 없도록
        redisUtil.save(cacheKey, responseDto, Duration.ofHours(1));

        return responseDto;
    }


    // --- 데이터 캐싱 헬퍼 메서드 ---

    /**
     * 리스트 조회를 통해 얻은 '고용형태', '직종'을 Redis에 캐시
     * (getJobDetail에서 사용하기 위함)
     */
    private void cacheExtraJobData(List<OpenApiJobItem> items) {
        if (items.isEmpty()) {
            return;
        }

        log.info("[Redis Caching] {}개의 'job:extra' 데이터 저장 시도...", items.size());
        items.forEach(item -> {
            String cacheKey = KEY_JOB_EXTRA + item.getJobId();
            JobExtraDataDto extraData = JobExtraDataDto.builder()
                    .employmentType(item.getEmplymShpNm())
                    .jobCategory(item.getJobclsNm())
                    .build();
            // TTL 24시간 (리스트에서 한번이라도 조회되면 24시간 동안 상세정보 보장) - 거의 바뀢지 않는 데이터 이기 때문에 24h로 설정
            redisUtil.save(cacheKey, extraData, Duration.ofHours(24));
        });
    }

    // --- 데이터 가공(Mapping) 헬퍼 메서드 ---

    // 리스트 DTO 매핑 (OpenApiJobItem -> JobItemDto)
    private JobListResponseDto mapToJobListDto(ExternalJobListItems openApiData, List<OpenApiJobItem> items, int page, int size) {

        List<JobItemDto> jobs = items.stream()
                .map(item -> JobItemDto.builder()
                        .id(item.getJobId())
                        .title(item.getRecrtTitle())
                        .companyName(item.getOranNm())
                        .deadlineStatus(calculateListDeadLineStatus(item.getToDd(), item.getDeadline()))
                        .employmentType(mapEmploymentType(item.getEmplymShpNm()))
                        .jobCategory(item.getJobclsNm())
                        .build())
                .collect(Collectors.toList());

        int totalCount = openApiData.getTotalCount();
        PaginationDto pagination = PaginationDto.builder()
                .totalCount(totalCount)
                .currentPage(page)
                .itemsPerPage(size)
                .totalPages((int) Math.ceil((double) totalCount / size))
                .build();

        return new JobListResponseDto(pagination, jobs);
    }

    /**
     * 상세 DTO 매핑 (2개 DTO 조합)
     *
     * @param detailItem 상세 API 응답
     * @param extraData  Redis에서 가져온 추가 데이터 (고용형태, 직종)
     * @return JobDetailResponseDto
     */
    private JobDetailResponseDto mapToJobDetailDto(OpenApiDetailItem detailItem, JobExtraDataDto extraData) {

        // 마감일 계산 (yyyyMMdd -> D-day)
        String deadlineStatus = calculateDday(detailItem.getToAcptDd());
        // 표시용 날짜 (yyyyMMdd -> yyyy-MM-dd)
        String displayEndDate = formatDisplayDate(detailItem.getToAcptDd());

        return JobDetailResponseDto.builder()
                .id(detailItem.getJobId())
                .title(detailItem.getWantedTitle())
                .companyName(detailItem.getPlbizNm())
                .location(detailItem.getPlDetAddr())
                .deadlineStatus(deadlineStatus) // "D-7", "오늘 마감" 등
                .description(detailItem.getDetCnts())
                .applyMethod(mapApplyMethod(detailItem.getAcptMthdCd()))
                .homepageUrl(detailItem.getHomepage())
                .endDate(displayEndDate) // "yyyy-MM-dd"
                .employmentType(extraData.getEmploymentType()) // Redis에서 가져온 값
                .jobCategory(extraData.getJobCategory())       // Redis에서 가져온 값
                .build();
    }


    /**
     * 리스트용 마감 상태 (접수 마감, 접수중, 오늘 마감)
     *
     * @param toDd     리스트 API의 종료일 (yyyy-MM-dd)
     * @param deadline 리스트 API의 상태값 (마감/접수중)
     * @return "접수 마감", "접수중", "오늘 마감", "상시 모집" 중 하나
     */
    private String calculateListDeadLineStatus(String toDd, String deadline) {
        // 1. 마감 상태가 명시적인 경우
        if (deadline != null && (deadline.contains("마감") || deadline.contains("종료"))) {
            return "접수 마감";
        }

        // 2. 마감 상태가 아니면 toDd로 "오늘 마감", "접수 마감" 확인
        // 리스트 API의 toDd는 'yyyy-MM-dd' 형식
        if (toDd != null && !toDd.isEmpty()) {
            try {
                LocalDate endDate = LocalDate.parse(toDd, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), endDate);

                if (daysLeft < 0) {
                    return "접수 마감"; // 날짜 지남
                } else if (daysLeft == 0) {
                    return "오늘 마감";
                }
                // D-day가 1일 이상 남은 경우
                return "접수중";
            } catch (Exception e) {
                log.warn("D-day parsing error for date: {}", toDd);
                return "접수중"; // 파싱 실패 시 일단 접수중으로
            }
        }

        // 3. 'deadline'이 "마감"도 아니고 'toDd'도 없는 경우
        if (deadline != null && deadline.contains("접수중")) {
            return "접수중";
        }

        // 'deadline'과 'toDd' 둘 다 애매한 경우 (날짜 정보가 없는 경우)
        return "상시 모집";
    }


    /**
     * 상세용 마감 상태(D-day, 오늘 마감 등)
     *
     * @param toDd 상세 API의 종료일 (yyyyMMdd)
     * @return "D-7", "오늘 마감", "접수 마감", "상시 모집"
     */
    private String calculateDday(String toDd) {
        // 상세 API의 toAcptDd는 'yyyyMMdd' 형식
        if (toDd == null || toDd.isEmpty()) {
            return "상시 모집";
        }

        try {
            LocalDate endDate = LocalDate.parse(toDd, DateTimeFormatter.ofPattern("yyyyMMdd"));
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), endDate);

            if (daysLeft < 0) {
                return "접수 마감"; // 날짜 지난 경우
            } else if (daysLeft == 0) {
                return "오늘 마감";
            } else {
                return "D-" + daysLeft;
            }
        } catch (Exception e) {
            log.warn("D-day parsing error for date: {}", toDd);
            return "날짜 확인";
        }
    }

    // 날짜 포맷 변환 (yyyyMMdd -> yyyy-MM-dd)
    private String formatDisplayDate(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isEmpty()) {
            return "상시 모집";
        }
        try {
            // 상세 API는 yyyyMMdd 형식
            LocalDate date = LocalDate.parse(yyyyMMdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            log.warn("D-date parsing error for date: {}", yyyyMMdd);
            return yyyyMMdd; // 파싱 실패시 원본 반환
        }
    }

    /**
     * 접수방법 코드(acptMthdCd)를 한글 문자열로 변환
     *
     * @param acptMthdCd (CM0801, CM0802 ...)
     * @return (온라인, 이메일 ...)
     */
    private String mapApplyMethod(String acptMthdCd) {
        return ApplyMethod.fromCode(acptMthdCd).getDisplayName();
    }

    /**
     * 고용형태 코드나 이름을 한글 문자여롤 변환
     *
     * @param codeOrName 고용형태코드나 이름 (CM0101, 정규직 등)
     * @return (정규직, 계약직, ...)
     */
    private String mapEmploymentType(String codeOrName) {
        return EmploymentType.fromCode(codeOrName).getDisplayName();
    }
}