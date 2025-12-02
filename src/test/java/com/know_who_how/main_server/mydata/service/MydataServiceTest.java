package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.config.MydataProperties;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.util.RedisUtil;
import com.know_who_how.main_server.mydata.dto.MydataDto;
import com.know_who_how.main_server.mydata.dto.MydataResponse;
import com.know_who_how.main_server.mydata.repository.MydataRepository;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.PensionRepository;
import com.know_who_how.main_server.user.repository.UserInfoRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MydataServiceTest {

    @Mock
    private MydataAuthService mydataAuthService;
    @Mock
    private MydataRepository mydataRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private AssetsRepository assetsRepository;
    @Mock
    private PensionRepository pensionRepository;
    @Mock
    private RedisUtil redisUtil;

    @Mock
    private WebClient mydataRsWebClient;
    @Mock
    private WebClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock
    private WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private MydataProperties props;
    private MydataService service;

    @BeforeEach
    void setUp() {
        props = new MydataProperties();
        var rs = new MydataProperties.RsProperties();
        rs.setBaseUrl("http://rs.example.com");
        props.setRs(rs);

        service = new MydataService(mydataAuthService, props, mydataRepository, userRepository,
                userInfoRepository, assetsRepository, pensionRepository, redisUtil);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "mydataRsWebClient", mydataRsWebClient);
    }

    private User newUser(long id) {
        try {
            Constructor<User> ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            User user = ctor.newInstance();
            var field = User.class.getDeclaredField("userId");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("로그인 사용자 없으면 NOT_LOGIN_USER 예외")
    void getMyData_withoutUser_throws() {
        assertThatThrownBy(() -> service.getMyData(null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_LOGIN_USER);
    }

    @Test
    @DisplayName("Redis에 AccessToken 없으면 MYDATA_NOT_LINKED 예외")
    void getMyData_withoutToken_throws() {
        User user = newUser(1L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn(null);

        assertThatThrownBy(() -> service.getMyData(user))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MYDATA_NOT_LINKED);
    }

    @Test
    @DisplayName("Redis 토큰 공백이면 MYDATA_NOT_LINKED 예외")
    void getMyData_blankToken_throws() {
        User user = newUser(2L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn("   ");

        assertThatThrownBy(() -> service.getMyData(user))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MYDATA_NOT_LINKED);
    }

    @Test
    @DisplayName("정상 호출 시 자산/부채 반영 및 저장 로직 수행")
    void getMyData_success_flow() {
        User user = newUser(10L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn("token");

        MydataDto.AssetDto saving = new MydataDto.AssetDto();
        saving.setAssetType("SAVINGS");
        saving.setBalance(BigDecimal.valueOf(1000));
        saving.setBankCode("001");
        saving.setUpdatedAt(LocalDateTime.now().toString());

        MydataDto.AssetDto pensionAsset = new MydataDto.AssetDto();
        pensionAsset.setAssetType("PENSION");
        pensionAsset.setBalance(BigDecimal.valueOf(2000));
        pensionAsset.setBankCode("002");
        pensionAsset.setUpdatedAt(LocalDateTime.now().toString());
        MydataDto.PensionDetailsDto details = new MydataDto.PensionDetailsDto();
        details.setPensionType("IRP");
        details.setAccountName("acc");
        details.setPersonalContrib(BigDecimal.ONE);
        details.setCompanyContrib(BigDecimal.TEN);
        details.setTotalPersonalContrib(BigDecimal.valueOf(5));
        details.setContribYear(2025);
        pensionAsset.setPensionDetails(details);

        MydataDto.LiabilityDto liability = new MydataDto.LiabilityDto();
        liability.setBalance(BigDecimal.valueOf(500));
        liability.setBankCode("003");
        liability.setLiabilityType("LOAN");

        MydataResponse response = new MydataResponse();
        MydataDto data = new MydataDto();
        data.setAssets(List.of(saving, pensionAsset));
        data.setLiabilities(List.of(liability));
        response.setData(data);

        stubWebClientResponse(response);

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(assetsRepository.findByUser(user)).thenReturn(Collections.emptyList());
        when(userInfoRepository.findByUser(user)).thenReturn(Optional.of(
                UserInfo.builder().user(user)
                        .fixedMonthlyCost(1L)
                        .annualIncome(1L)
                        .targetRetiredAge(1)
                        .numDependents(1)
                        .mydataStatus(UserInfo.MyDataStatus.NONE)
                        .build()
        ));

        service.getMyData(user);

        verify(assetsRepository).saveAll(anyList());
        verify(pensionRepository).saveAll(anyList());
        verify(userRepository, atLeastOnce()).save(user);
        verify(userInfoRepository).save(any(UserInfo.class));
    }

    @Test
    @DisplayName("AccessToken 만료 시 refresh 후 재호출")
    void getMyData_refresh_on_unauthorized() {
        User user = newUser(20L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn("expired-token");
        MydataResponse response = new MydataResponse();
        response.setData(new MydataDto());
        stubWebClientResponseWithRetry(response, true);

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(assetsRepository.findByUser(user)).thenReturn(Collections.emptyList());
        when(mydataAuthService.refreshAccessToken(user.getUserId())).thenReturn("new-token");
        when(userInfoRepository.findByUser(user)).thenReturn(Optional.empty());

        service.getMyData(user);

        verify(mydataAuthService).refreshAccessToken(user.getUserId());
        verify(assetsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Refresh 실패 시 MYDATA_EXPIRED 예외")
    void getMyData_refresh_fail_throws() {
        User user = newUser(30L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn("expired-token");
        stubWebClientResponseWithRetry(null, false);
        when(mydataAuthService.refreshAccessToken(user.getUserId())).thenThrow(new CustomException(ErrorCode.MYDATA_EXPIRED));

        assertThatThrownBy(() -> service.getMyData(user))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MYDATA_EXPIRED);
    }

    @Test
    @DisplayName("RS 500 에러 시 MYDATA_SERVER_ERROR 예외")
    void getMyData_serverError_throws() {
        User user = newUser(40L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn("token");
        stubWebClientError(500);

        assertThatThrownBy(() -> service.getMyData(user))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MYDATA_SERVER_ERROR);
    }

    @Test
    @DisplayName("기존 자산이 있으면 삭제 후 재저장")
    void getMyData_deletesExistingAssets() {
        User user = newUser(50L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn("token");

        MydataDto.AssetDto saving = new MydataDto.AssetDto();
        saving.setAssetType("SAVING");
        saving.setBalance(BigDecimal.ONE);
        saving.setBankCode("111");
        MydataResponse response = new MydataResponse();
        MydataDto data = new MydataDto();
        data.setAssets(List.of(saving));
        data.setLiabilities(Collections.emptyList());
        response.setData(data);
        stubWebClientResponse(response);

        var existingAsset = com.know_who_how.main_server.global.entity.Asset.Asset.builder()
                .assetId(99L).user(user).type(com.know_who_how.main_server.global.entity.Asset.AssetType.SAVING)
                .balance(BigDecimal.TEN).build();

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(assetsRepository.findByUser(user)).thenReturn(List.of(existingAsset));
        when(userInfoRepository.findByUser(user)).thenReturn(Optional.empty());

        service.getMyData(user);

        verify(pensionRepository).deleteAllById(List.of(99L));
        verify(assetsRepository).deleteAll(List.of(existingAsset));
        verify(assetsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("잘못된 자산 타입은 무시되고 저장하지 않는다")
    void getMyData_invalidAssetType_skipped() {
        User user = newUser(60L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn("token");

        MydataDto.AssetDto invalid = new MydataDto.AssetDto();
        invalid.setAssetType("UNKNOWN");
        invalid.setBalance(BigDecimal.ONE);

        MydataResponse response = new MydataResponse();
        MydataDto data = new MydataDto();
        data.setAssets(List.of(invalid));
        data.setLiabilities(Collections.emptyList());
        response.setData(data);
        stubWebClientResponse(response);

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(assetsRepository.findByUser(user)).thenReturn(Collections.emptyList());
        when(userInfoRepository.findByUser(user)).thenReturn(Optional.empty());

        ArgumentCaptor<List> assetCaptor = ArgumentCaptor.forClass(List.class);
        service.getMyData(user);

        verify(assetsRepository).saveAll(assetCaptor.capture());
        List<?> saved = assetCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(saved).isEmpty();
    }

    @Test
    @DisplayName("RS 응답이 null이면 persistence를 수행하지 않는다")
    void getMyData_nullResponse_returnsNull() {
        User user = newUser(70L);
        when(redisUtil.get("mydata:access:" + user.getUserId())).thenReturn("token");
        stubWebClientResponse(null);

        var result = service.getMyData(user);

        org.assertj.core.api.Assertions.assertThat(result).isNull();
        verifyNoInteractions(assetsRepository);
        verifyNoInteractions(pensionRepository);
        verify(userRepository, never()).save(any());
    }

    private void stubWebClientResponse(MydataResponse response) {
        doReturn(uriSpec).when(mydataRsWebClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(headersSpec).when(headersSpec).headers(any());
        doReturn(responseSpec).when(headersSpec).retrieve();
        Mono<MydataResponse> mono = (response == null) ? Mono.empty() : Mono.just(response);
        when(responseSpec.bodyToMono(eq(MydataResponse.class))).thenReturn(mono);
    }

    private void stubWebClientResponseWithRetry(MydataResponse response, boolean secondSuccess) {
        doReturn(uriSpec).when(mydataRsWebClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(headersSpec).when(headersSpec).headers(any());
        doReturn(responseSpec).when(headersSpec).retrieve();

        AtomicInteger count = new AtomicInteger();
        when(responseSpec.bodyToMono(eq(MydataResponse.class))).thenAnswer(invocation -> {
            if (count.getAndIncrement() == 0) {
                return Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
                        "Unauthorized", 401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));
            }
            if (secondSuccess) {
                return Mono.just(response);
            }
            return Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
                    "Unauthorized", 401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));
        });
    }

    private void stubWebClientError(int status) {
        doReturn(uriSpec).when(mydataRsWebClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(headersSpec).when(headersSpec).headers(any());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.bodyToMono(eq(MydataResponse.class))).thenReturn(Mono.error(
                new org.springframework.web.reactive.function.client.WebClientResponseException(
                        "err", status, "err", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
                )));
    }
}
