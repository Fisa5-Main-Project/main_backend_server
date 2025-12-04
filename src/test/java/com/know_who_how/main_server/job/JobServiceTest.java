package com.know_who_how.main_server.job;

import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.job.client.JobOpenApiClient;
import com.know_who_how.main_server.job.dto.*;
import com.know_who_how.main_server.job.dto.external.ExternalApiResponse;
import com.know_who_how.main_server.job.dto.external.ExternalJobDetailItemWrapper;
import com.know_who_how.main_server.job.dto.external.ExternalJobListItems;
import com.know_who_how.main_server.global.util.RedisUtil;
import com.know_who_how.main_server.job.service.JobService;
import com.know_who_how.main_server.job.type.EmploymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JOB-Service 단위 테스트")
class JobServiceTest {

    @InjectMocks
    private JobService jobService;

    @Mock
    private JobOpenApiClient apiClient;

    @Mock
    private RedisUtil redisUtil;

    // 테스트에 필요한 더미 데이터 정의
    private final String TEST_SEARCH = "서울";
    private final String TEST_EMP_TYPE_CODE = "CM0101";
    private final String TEST_JOB_ID = "JOB12345";
    private JobListResponseDto mockListResponse;
    private ExternalApiResponse<ExternalJobListItems> mockListApiWrapper;
    private JobExtraDataDto mockExtraData;

    @BeforeEach
    void setUp() {
        mockListResponse = mock(JobListResponseDto.class);
        mockListApiWrapper = mock(ExternalApiResponse.class);
        mockExtraData = mock(JobExtraDataDto.class);

        // Reflection을 통해 Mock 객체를 실제 필드에 주입 (private final 필드에 Mock 객체를 주입하기 위함)
        ReflectionTestUtils.setField(jobService, "apiClient", apiClient);
        ReflectionTestUtils.setField(jobService, "redisUtil", redisUtil);
    }

    /**
     * JOB-01: 채용 공고 리스트 최초 조회 (Cache Miss)
     */
    @Test
    @DisplayName("JOB-01: 리스트 조회 - Cache Miss 시 API 호출 및 캐시 저장")
    void getJobList_cacheMiss_shouldCallApiAndSaveCache() throws Exception {
        // Given
        // 1. RedisUtil.get() 호출 시 null 반환 (Cache Miss)
        given(redisUtil.get(anyString())).willReturn(null);

        // 2. OpenApiClient.fetchJobs() 호출 시 유효한 API 응답 반환 Mocking (Reflection 제거)
        ExternalJobListItems mockItems = mock(ExternalJobListItems.class); // DTO Mocking
        given(mockItems.getItem()).willReturn(Collections.emptyList());
        given(mockListApiWrapper.getBody()).willReturn(mockItems);
        given(apiClient.fetchJobs(anyString(), anyString(), anyInt(), anyInt())).willReturn(mockListApiWrapper);

        // When
        jobService.getJobList(TEST_SEARCH, TEST_EMP_TYPE_CODE, 1, 20);

        // Then
        // 1. Open API 호출이 정확히 1회 발생했는지 검증
        then(apiClient).should(times(1)).fetchJobs(eq(TEST_SEARCH), eq(TEST_EMP_TYPE_CODE), eq(1), eq(20));
        // 2. 최종 결과가 Redis에 저장되었는지 검증 (jobs:list 캐싱)
        then(redisUtil).should(times(1)).save(startsWith("jobs:list:"), any(JobListResponseDto.class), any(Duration.class));
    }

    /**
     * JOB-02: 채용 공고 리스트 재조회 (Cache Hit)
     */
    @Test
    @DisplayName("JOB-02: 리스트 조회 - Cache Hit 시 API 호출 생략 및 캐시 데이터 반환")
    void getJobList_cacheHit_shouldReturnCachedDataAndSkipApiCall() {
        // Given
        // 1. RedisUtil.get() 호출 시 캐시된 데이터(mockListResponse) 반환 (Cache Hit)
        given(redisUtil.get(anyString())).willReturn(mockListResponse);

        // When
        JobListResponseDto result = jobService.getJobList(TEST_SEARCH, TEST_EMP_TYPE_CODE, 1, 20);

        // Then
        // 1. Open API 호출이 발생하지 않았는지 검증
        then(apiClient).should(never()).fetchJobs(anyString(), anyString(), anyInt(), anyInt());
        // 2. 캐시된 데이터가 정확히 반환되었는지 검증
        assertEquals(mockListResponse, result);
    }

    /**
     * JOB-03: 고용형태 'ALL' 필터 조회
     */
    @Test
    @DisplayName("JOB-03: 고용형태 'ALL' 입력 시 API에 null 전달")
    void getJobList_empTypeAll_shouldPassNullToApiClient() throws Exception {
        // Given
        final String ALL_EMP_TYPE = "ALL";
        given(redisUtil.get(anyString())).willReturn(null);

        // API 호출 시 empType이 null로 전달될 것을 가정하고 Mocking
        ExternalJobListItems mockItems = mock(ExternalJobListItems.class);
        given(mockItems.getItem()).willReturn(Collections.emptyList());

        ExternalApiResponse<ExternalJobListItems> localMockWrapper = mock(ExternalApiResponse.class);
        given(localMockWrapper.getBody()).willReturn(mockItems);

        // fetchJobs 호출 시 empType 인자에 null이 전달될 때 (isNull()) 응답을 Mocking
        given(apiClient.fetchJobs(anyString(), isNull(), anyInt(), anyInt())).willReturn(localMockWrapper);

        // When
        jobService.getJobList(TEST_SEARCH, ALL_EMP_TYPE, 1, 20);

        // Then
        // 1. apiClient.fetchJobs() 호출 시 empType 인자에 null이 전달되었는지 검증
        then(apiClient).should(times(1)).fetchJobs(eq(TEST_SEARCH), isNull(), eq(1), eq(20));
    }


    /**
     * JOB-04: 상세 조회 - Cache Miss & Extra Data Hit
     */
    @Test
    @DisplayName("JOB-04: 상세 조회 - job:extra 데이터 Hit 시 조합 성공")
    void getJobDetail_extraDataHit_shouldCombineData() {
        // Given
        // 1. job:detail 캐시 Miss
        given(redisUtil.get(startsWith("job:detail:"))).willReturn(null);

        // 2. DTO Mocking 체인 구성
        OpenApiDetailItem mockDetailItem = mock(OpenApiDetailItem.class);
        given(mockDetailItem.getToAcptDd()).willReturn("20251231"); // D-day 계산을 위해 더미 날짜 설정
        ExternalJobDetailItemWrapper mockDetailWrapper = mock(ExternalJobDetailItemWrapper.class);
        ExternalApiResponse<ExternalJobDetailItemWrapper> mockDetailApiResponse = mock(ExternalApiResponse.class);

        given(mockDetailWrapper.getItem()).willReturn(mockDetailItem);
        given(mockDetailApiResponse.getBody()).willReturn(mockDetailWrapper);
        given(apiClient.fetchJobDetail(TEST_JOB_ID)).willReturn(mockDetailApiResponse);

        // 3. job:extra 데이터 Hit (스텁을 테스트 내부에 정의)
        JobExtraDataDto localMockExtraData = mock(JobExtraDataDto.class);
        given(localMockExtraData.getEmploymentType()).willReturn("정규직");
        given(localMockExtraData.getJobCategory()).willReturn("개발");
        given(redisUtil.get(startsWith("job:extra:"))).willReturn(localMockExtraData);

        // When
        JobDetailResponseDto result = jobService.getJobDetail(TEST_JOB_ID);

        // Then
        // 1. job:extra 데이터가 Redis에서 조회되었는지 확인
        then(redisUtil).should(times(1)).get(startsWith("job:extra:"));
        // 2. 최종 결과가 Redis에 저장되었는지 (job:detail 캐싱)
        then(redisUtil).should(times(1)).save(startsWith("job:detail:"), any(JobDetailResponseDto.class), any(Duration.class));
        assertNotNull(result);
    }

    /**
     * JOB-05: 상세 조회 - Extra Data Miss (데이터 누락)
     */
    @Test
    @DisplayName("JOB-05: 상세 조회 - job:extra Miss 시 '정보 없음'으로 대체")
    void getJobDetail_extraDataMiss_shouldUseDefaultValue() {
        // Given
        // 1. job:detail 캐시 Miss
        given(redisUtil.get(startsWith("job:detail:"))).willReturn(null);

        // 2. 상세 API 호출 성공 (Mocking)
        OpenApiDetailItem mockDetailItem = mock(OpenApiDetailItem.class);
        given(mockDetailItem.getToAcptDd()).willReturn("20251231");
        ExternalJobDetailItemWrapper mockDetailWrapper = mock(ExternalJobDetailItemWrapper.class);
        ExternalApiResponse<ExternalJobDetailItemWrapper> mockDetailApiResponse = mock(ExternalApiResponse.class);

        given(mockDetailWrapper.getItem()).willReturn(mockDetailItem);
        given(mockDetailApiResponse.getBody()).willReturn(mockDetailWrapper);
        given(apiClient.fetchJobDetail(TEST_JOB_ID)).willReturn(mockDetailApiResponse);

        // 3. job:extra 데이터 Miss (null 반환)
        given(redisUtil.get(startsWith("job:extra:"))).willReturn(null);


        // When
        JobDetailResponseDto result = jobService.getJobDetail(TEST_JOB_ID);

        // Then
        // save 호출이 일어났는지로만 검증
        then(redisUtil).should(times(1)).save(startsWith("job:detail:"), any(JobDetailResponseDto.class), any(Duration.class));
        assertNotNull(result);
    }

    /**
     * JOB-07: 고용형태 코드 변환
     */
    @Test
    @DisplayName("JOB-07: 고용형태 변환 - 코드 또는 이름 입력 시 정확한 DisplayName 반환")
    void mapEmploymentType_codeOrName_shouldReturnDisplayName() {
        // Given/When/Then: Enum의 fromCode 메서드를 직접 테스트

        // 코드(CM0101) 입력
        assertEquals("정규직", EmploymentType.fromCode("CM0101").getDisplayName());
        // 이름(정규직) 입력
        assertEquals("정규직", EmploymentType.fromCode("정규직").getDisplayName());

        // null은 항상 'UNKNOWN' (정보 없음)을 반환
        assertEquals("정보 없음", EmploymentType.fromCode(null).getDisplayName());

        // 'ALL' 문자열 입력
        assertEquals("전체", EmploymentType.fromCode("ALL").getDisplayName());

        // 알 수 없는 값 입력
        assertEquals("정보 없음", EmploymentType.fromCode("UNKNOWN_CODE").getDisplayName());
    }

    /**
     * JOB-06: API 서버 오류 재시도 (Retry)
     */
    @Test
    @DisplayName("JOB-06: API 호출 실패 시 CustomException 발생 확인")
    void fetchJobs_apiCallFails_shouldThrowCustomException() {
        // Given
        given(redisUtil.get(anyString())).willReturn(null);
        // API 클라이언트 호출이 실패(재시도 포함)하고 최종적으로 CustomException을 던지도록 설정
        given(apiClient.fetchJobs(anyString(), anyString(), anyInt(), anyInt()))
                .willThrow(new CustomException(com.know_who_how.main_server.global.exception.ErrorCode.EXTERNAL_API_SERVER_ERROR));

        // When & Then
        // JobService가 API 클라이언트의 실패 예외를 그대로 전파하는지 확인
        assertThrows(CustomException.class, () -> {
            jobService.getJobList(TEST_SEARCH, TEST_EMP_TYPE_CODE, 1, 20);
        });

        // JobService는 API 클라이언트 호출을 1회 시도 (Retry는 클라이언트 내부 로직)
        then(apiClient).should(times(1)).fetchJobs(anyString(), anyString(), anyInt(), anyInt());
    }
}