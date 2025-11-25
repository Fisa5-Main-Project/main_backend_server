package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.config.MydataProperties;
import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.Asset.Pension.Pension;
import com.know_who_how.main_server.global.entity.Asset.Pension.PensionType;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.dto.MydataDto;
import com.know_who_how.main_server.mydata.dto.MydataResponse;
import com.know_who_how.main_server.mydata.repository.MydataRepository;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.PensionRepository;
import com.know_who_how.main_server.user.repository.UserInfoRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MydataService {

    private final MydataProperties mydataProps;
    private final WebClient mydataWebClient;
    private final MydataRepository mydataRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final AssetsRepository assetsRepository;
    private final PensionRepository pensionRepository;

    /**
     * Fetch MyData from RS using stored access token, persist assets/pensions,
     * update assetTotal and mydata flags.
     */
    @Transactional
    public MydataDto getMyData(User user, HttpServletRequest request) {
        String accessToken = extractMydataAccessToken(user, request);
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }

        String url = mydataProps.getRs().getMyDataApi();
        WebClient plainClient = WebClient.builder().build();

        MydataResponse response = plainClient
                .get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(MydataResponse.class)
                .block();

        if (response != null && response.getData() != null && response.getData().getAssets() != null) {
            response.getData().getAssets().forEach(asset -> {
                if (asset.getAssetType() != null && asset.getAssetType().equalsIgnoreCase("SAVINGS")) {
                    asset.setAssetType("SAVING");
                }
            });
        }

        MydataDto data = response != null ? response.getData() : null;
        if (data != null && user != null) {
            persistAssetsAndPensions(user, data);
            updateUserWithMydata(user, data);
        }
        return data;
    }

    private String extractMydataAccessToken(User user, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object token = session.getAttribute("MYDATA_ACCESS_TOKEN");
            if (token instanceof String tokenStr && !tokenStr.isBlank()) {
                return tokenStr;
            }
        }
        if (user != null) {
            return mydataRepository.findById(user.getUserId())
                    .map(com.know_who_how.main_server.global.entity.MyData.Mydata::getAccessToken)
                    .orElse(null);
        }
        return null;
    }

    private void persistAssetsAndPensions(User user, MydataDto data) {
        var managedUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Asset> existingAssets = assetsRepository.findByUser(managedUser);
        if (!existingAssets.isEmpty()) {
            List<Long> assetIds = existingAssets.stream()
                    .map(Asset::getAssetId)
                    .collect(Collectors.toList());
            pensionRepository.deleteAllById(assetIds);
            assetsRepository.deleteAll(existingAssets);
        }

        List<MydataDto.AssetDto> assetDtos = data.getAssets() != null ? data.getAssets() : Collections.emptyList();

        List<AssetMapping> assetMappings = assetDtos.stream()
                .map(dto -> new AssetMapping(dto, toAssetEntity(dto, managedUser)))
                .filter(mapping -> mapping.asset() != null)
                .collect(Collectors.toList());

        List<Asset> assetsToSave = assetMappings.stream()
                .map(AssetMapping::asset)
                .collect(Collectors.toList());
        assetsRepository.saveAll(assetsToSave);

        List<Pension> pensionsToSave = assetMappings.stream()
                .filter(mapping -> mapping.asset().getType() == AssetType.PENSION)
                .filter(mapping -> mapping.dto().getPensionDetails() != null)
                .map(mapping -> toPensionEntity(mapping.dto(), mapping.asset()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        pensionRepository.saveAll(pensionsToSave);
    }

    private Asset toAssetEntity(MydataDto.AssetDto dto, User user) {
        if (dto.getAssetType() == null || dto.getBalance() == null) {
            return null;
        }
        AssetType type;
        try {
            type = AssetType.valueOf(dto.getAssetType());
        } catch (IllegalArgumentException e) {
            return null;
        }
        return Asset.builder()
                .user(user)
                .type(type)
                .balance(dto.getBalance())
                .bankCode(dto.getBankCode())
                .build();
    }

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
                updatedAt = LocalDateTime.parse(pensionAssetDto.getUpdatedAt(), DateTimeFormatter.ISO_DATE_TIME);
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

    private record AssetMapping(MydataDto.AssetDto dto, Asset asset) {
    }

    private void updateUserWithMydata(User user, MydataDto data) {
        var managedUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<MydataDto.AssetDto> assetDtos = data.getAssets() != null ? data.getAssets() : Collections.emptyList();
        List<MydataDto.LiabilityDto> liabilityDtos = data.getLiabilities() != null ? data.getLiabilities() : Collections.emptyList();

        long assetSum = sumAssetBalances(assetDtos);
        long liabilitySum = sumLiabilityBalances(liabilityDtos);
        long netTotal = assetSum - liabilitySum;

        managedUser.updateAssetTotal(netTotal);
        managedUser.markMydataRegistered();
        userRepository.save(managedUser);

        userInfoRepository.findByUser(managedUser).ifPresent(info -> {
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
