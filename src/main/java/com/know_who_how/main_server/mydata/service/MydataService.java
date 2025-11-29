package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.config.MydataProperties;
import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.Asset.Pension.Pension;
import com.know_who_how.main_server.global.entity.Asset.Pension.PensionType;
import com.know_who_how.main_server.global.entity.Mydata.Mydata;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MydataService {

    private final MydataAuthService mydataAuthService;
    private final MydataProperties mydataProps;
    private final MydataRepository mydataRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final AssetsRepository assetsRepository;
    private final PensionRepository pensionRepository;

    private final RedisUtil redisUtil;

    /**
     * AS(인증 서버) 호출용 WebClient
     * - /oauth2/token (authorization_code, refresh_token)
     * - /oauth2/revoke 등
     * 아직 이 클래스에서 직접 사용하진 않지만,
     * 향후 토큰 갱신/폐기 로직에서 사용할 예정.
     */
    @Autowired
    @Qualifier("mydataAuthWebClient")
    private WebClient mydataAuthWebClient;

    /**
     * RS(리소스 서버, MyData API) 호출용 WebClient
     * - /api/my-data/summary
     * - /api/my-data/assets/pension 등
     */
    @Autowired
    @Qualifier("mydataRsWebClient")
    private WebClient mydataRsWebClient;


    /**
     * MyData RS에서 자산/부채/연금 데이터를 조회하고,
     * 우리 서비스 DB(assets, pension, users, users_info)를 갱신한 뒤,
     * 최종 MydataDto를 반환한다.
     * - MyData 토큰은 Mydata 테이블에서만 조회
     */
    @Transactional
    public MydataDto getMyData(User user) {
        // 1) 로그인 여부 확인
        if (user == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }

        // 2) DB(MyData 테이블)에서 연동된 마이데이터 Access Token 조회
        String key = "mydata:access:" + user.getUserId();
        Object tokenObj = redisUtil.get(key);
        if (tokenObj == null) {
            throw new CustomException(ErrorCode.MYDATA_NOT_LINKED);
        }
        String accessToken = tokenObj.toString();

        if (accessToken.isBlank()) {
            log.info("Access Token이 만료되었습니다. Refresh Token으로 갱신을 시도하세요.");
            throw new CustomException(ErrorCode.MYDATA_NOT_LINKED);
        }


        // 3) RS(MyData API) 호출
        String url = mydataProps.getRs().getBaseUrl() + "/api/v1/my-data";
        log.info("Calling Mydata Rs: {}", url);
        MydataResponse response;

        try {
            response = callMyDataApi(url, accessToken);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.info("Access Token 만료 감지. Refresh Token으로 갱신을 시도합니다. UserID: {}", user.getUserId());

                try {
                    String newAccessToken = mydataAuthService.refreshAccessToken(user.getUserId());
                    response = callMyDataApi(url, newAccessToken);
                } catch (Exception refreshEx) {
                    log.error("Refresh Token 갱신 실패. 재로그인 필요.", refreshEx);
                    throw new CustomException(ErrorCode.MYDATA_EXPIRED);
                }
            } else {
                log.error("MyData RS request error: {}", e.getMessage(), e);
                throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
            }
        }

        // 4) RS에서 내려온 자산 타입 정규화 ("SAVINGS" -> "SAVING")
        if (response != null
                && response.getData() != null
                && response.getData().getAssets() != null) {

            response.getData().getAssets().forEach(asset -> {
                if (asset.getAssetType() != null
                        && asset.getAssetType().equalsIgnoreCase("SAVINGS")) {
                    asset.setAssetType("SAVING");
                }
            });
        }

        // 5) 응답 데이터 추출 및 DB 반영
        MydataDto data = (response != null) ? response.getData() : null;

        if (data != null) {
            // 자산/퇴직연금 테이블(assets, pension) 갱신
            persistAssetsAndPensions(user, data);
            // users / users_info 의 assetTotal, mydata_status 등 갱신
            updateUserWithMydata(user, data);
        }

        // 6) 최종 DTO 반환 (프론트에서 바로 사용)
        return data;
    }

    private MydataResponse callMyDataApi(String url, String accessToken) {
        return mydataRsWebClient
                .get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(MydataResponse.class)
                .block();
    }


    /**
     * RS에서 내려온 자산/연금 정보를
     * - assets
     * - pension
     * 테이블에 반영한다.
     */
    private void persistAssetsAndPensions(User user, MydataDto data) {
        var managedUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 자산/연금 데이터 삭제 (MyData 기준으로 완전히 재생성)
        List<Asset> existingAssets = assetsRepository.findByUser(managedUser);
        if (!existingAssets.isEmpty()) {
            List<Long> assetIds = existingAssets.stream()
                    .map(Asset::getAssetId)
                    .collect(Collectors.toList());
            pensionRepository.deleteAllById(assetIds);
            assetsRepository.deleteAll(existingAssets);
        }

        List<MydataDto.AssetDto> assetDtos =
                data.getAssets() != null ? data.getAssets() : Collections.emptyList();

        // MyData AssetDto -> Asset 엔티티로 매핑
        List<AssetMapping> assetMappings = assetDtos.stream()
                .map(dto -> new AssetMapping(dto, toAssetEntity(dto, managedUser)))
                .filter(mapping -> mapping.asset() != null)
                .collect(Collectors.toList());

        // Asset 저장
        List<Asset> assetsToSave = assetMappings.stream()
                .map(AssetMapping::asset)
                .collect(Collectors.toList());
        assetsRepository.saveAll(assetsToSave);

        // Asset 중 PENSION 타입만 골라 Pension 엔티티로 저장
        List<Pension> pensionsToSave = assetMappings.stream()
                .filter(mapping -> mapping.asset().getType() == AssetType.PENSION)
                .filter(mapping -> mapping.dto().getPensionDetails() != null)
                .map(mapping -> toPensionEntity(mapping.dto(), mapping.asset()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        pensionRepository.saveAll(pensionsToSave);
    }

    /**
     * 단일 자산 DTO를 Asset 엔티티로 변환.
     */
    private Asset toAssetEntity(MydataDto.AssetDto dto, User user) {
        if (dto.getAssetType() == null || dto.getBalance() == null) {
            return null;
        }

        AssetType type;
        try {
            type = AssetType.valueOf(dto.getAssetType());
        } catch (IllegalArgumentException e) {
            // Enum에 없는 타입이면 무시
            return null;
        }

        return Asset.builder()
                .user(user)
                .type(type)
                .balance(dto.getBalance())
                .bankCode(dto.getBankCode())
                .build();
    }

    /**
     * 자산 DTO + Asset 엔티티를 기반으로 Pension 엔티티 생성.
     * (자산 타입이 PENSION인 경우에만 호출)
     */
    private Pension toPensionEntity(MydataDto.AssetDto pensionAssetDto, Asset asset) {
        MydataDto.PensionDetailsDto details = pensionAssetDto.getPensionDetails();
        if (details == null) {
            return null;
        }

        PensionType pensionType = null;
        if (details.getPensionType() != null) {
            try {
                pensionType = PensionType.valueOf(details.getPensionType());
            } catch (IllegalArgumentException ignored) {
            }
        }

        LocalDateTime updatedAt = null;
        if (pensionAssetDto.getUpdatedAt() != null) {
            try {
                updatedAt = LocalDateTime.parse(
                        pensionAssetDto.getUpdatedAt(),
                        DateTimeFormatter.ISO_DATE_TIME
                );
            } catch (Exception ignored) {
            }
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }

        return Pension.builder()
                .asset(asset)
                .updatedAt(updatedAt)
                .pensionType(pensionType)
                .accountName(details.getAccountName())
                .principal(details.getTotalPersonalContrib())
                .companyContrib(details.getCompanyContrib())
                .personalContrib(details.getPersonalContrib())
                .contribYear(details.getContribYear())
                .totalPersonalContrib(details.getTotalPersonalContrib())
                .build();
    }

    /**
     * AssetDto와 Asset 엔티티를 같이 들고 다니기 위한 내부 record.
     */
    private record AssetMapping(MydataDto.AssetDto dto, Asset asset) {
    }

    /**
     * MyData로부터 조회한 자산/부채를 기반으로
     * - users.assetTotal (순자산)
     * - users.user_mydata_registration
     * - users_info.mydata_status
     * 등을 갱신한다.
     */
    private void updateUserWithMydata(User user, MydataDto data) {
        var managedUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<MydataDto.AssetDto> assetDtos =
                data.getAssets() != null ? data.getAssets() : Collections.emptyList();
        List<MydataDto.LiabilityDto> liabilityDtos =
                data.getLiabilities() != null ? data.getLiabilities() : Collections.emptyList();

        long assetSum = sumAssetBalances(assetDtos);
        long liabilitySum = sumLiabilityBalances(liabilityDtos);
        long netTotal = assetSum - liabilitySum;

        // 순자산 반영
        managedUser.updateAssetTotal(netTotal);
        // MyData 연동 여부 플래그
        managedUser.markMydataRegistered();
        userRepository.save(managedUser);

        userInfoRepository.findByUser(managedUser).ifPresent(info -> {
            // CONNECTED, DISCONNECTED, NONE
            info.updateMydataStatus(UserInfo.MyDataStatus.CONNECTED);
            userInfoRepository.save(info);
        });
    }

    private long sumAssetBalances(List<MydataDto.AssetDto> assets) {
        return assets.stream()
                .map(MydataDto.AssetDto::getBalance)
                .filter(Objects::nonNull)
                .mapToLong(BigDecimal::longValue)
                .sum();
    }

    private long sumLiabilityBalances(List<MydataDto.LiabilityDto> liabilities) {
        return liabilities.stream()
                .map(MydataDto.LiabilityDto::getBalance)
                .filter(Objects::nonNull)
                .mapToLong(BigDecimal::longValue)
                .sum();
    }
}
