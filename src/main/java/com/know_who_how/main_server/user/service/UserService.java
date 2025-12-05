package com.know_who_how.main_server.user.service;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.Asset.Pension.Pension;
import com.know_who_how.main_server.global.entity.Keyword.Keyword;
import com.know_who_how.main_server.global.entity.Keyword.UserKeyword;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.repository.MydataRepository;
import com.know_who_how.main_server.user.dto.*;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.KeywordRepository;
import com.know_who_how.main_server.user.repository.PensionRepository;
import com.know_who_how.main_server.user.repository.RefreshTokenRepository;
import com.know_who_how.main_server.user.repository.UserInfoRepository;
import com.know_who_how.main_server.user.repository.UserKeywordRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import com.know_who_how.main_server.user.repository.UserTermRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AssetsRepository assetsRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final KeywordRepository keywordRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserTermRepository userTermRepository;
    private final UserInfoRepository userInfoRepository;
    private final PensionRepository pensionRepository;
    private final MydataRepository mydataRepository;

    public UserResponseDto getUserInfo(User user) {
        return UserResponseDto.from(user);
    }

    @Transactional
    public void addUserAssets(User user, UserAssetAddRequest request) {
        User foundUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Asset> assets = new java.util.ArrayList<>();
        if (request.getRealEstate() != null) {
            assets.add(Asset.builder()
                    .user(foundUser)
                    .type(AssetType.REAL_ESTATE)
                    .balance(new java.math.BigDecimal(request.getRealEstate()))
                    .build());
        }

        if (request.getCar() != null) {
            assets.add(Asset.builder()
                    .user(foundUser)
                    .type(AssetType.AUTOMOBILE)
                    .balance(new java.math.BigDecimal(request.getCar()))
                    .build());
        }

        if (!assets.isEmpty()) {
            assetsRepository.saveAll(assets);
        }
    }

    public List<UserAssetResponseDto> getUserAssets(User user) {
        List<Asset> assets = assetsRepository.findByUser(user);
        return assets.stream()
                .map(UserAssetResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateProfile(User user, ProfileUpdateRequestDto requestDto) {
        User foundUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        foundUser.updatePhoneNum(requestDto.getPhoneNum());
    }

    public List<UserKeywordDto> getUserKeywords(User user) {
        return userKeywordRepository.findByUser(user).stream()
                .map(userKeyword -> UserKeywordDto.from(userKeyword.getKeyword()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateInvestmentTendancy(User user, InvestmentTendencyUpdateRequestDto requestDto) {
        User foundUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        foundUser.updateInvestmentTendancy(requestDto.getInvestmentTendancy());
    }

    @Transactional
    public void updateUserKeywords(User user, UserKeywordsUpdateRequestDto requestDto) {
        User foundUser = userRepository.findById(user.getUserId()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        userKeywordRepository.deleteAllByUser(foundUser);
        List<Keyword> keywords = keywordRepository.findAllById(requestDto.getKeywordIds());
        if (keywords.size() != requestDto.getKeywordIds().size()) {
            throw new IllegalArgumentException("존재하지 않는 키워드 ID가 포함되어 있습니다.");
        }
        List<UserKeyword> newUserKeywords = keywords.stream()
                .map(keyword -> UserKeyword.builder()
                        .user(foundUser)
                        .keyword(keyword)
                        .build())
                .collect(Collectors.toList());
        userKeywordRepository.saveAll(newUserKeywords);
    }

    @Transactional
    public void withdrawUser(User user) {
        // 연관된 자식 데이터부터 삭제
        userKeywordRepository.deleteAllByUser(user);
        userTermRepository.deleteAllByUser(user);
        assetsRepository.deleteAllByUser(user);
        userInfoRepository.deleteByUser(user);
        refreshTokenRepository.deleteByUser(user);
        mydataRepository.deleteByUser(user);
        // 마지막으로 User 엔티티 삭제
        userRepository.delete(user);
    }

    public List<PensionAssetDto> getUserPensionAssets(User user) {
        var managedUser = resolveUser(user);
        List<com.know_who_how.main_server.global.entity.Asset.Asset> pensionAssets = assetsRepository.findByUser(managedUser)
                .stream()
                .filter(asset -> asset.getType() == AssetType.PENSION)
                .collect(Collectors.toList());

        List<Long> pensionAssetIds = pensionAssets.stream()
                .map(com.know_who_how.main_server.global.entity.Asset.Asset::getAssetId)
                .collect(Collectors.toList());

        Map<Long, com.know_who_how.main_server.global.entity.Asset.Pension.Pension> pensionsById = pensionRepository.findAllById(pensionAssetIds)
                .stream()
                .collect(Collectors.toMap(com.know_who_how.main_server.global.entity.Asset.Pension.Pension::getAssetId, p -> p));

        return pensionAssets.stream()
                .map(asset -> PensionAssetDto.from(asset, pensionsById.get(asset.getAssetId())))
                .collect(Collectors.toList());
    }

    private User resolveUser(User user) {
        if (user == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }
        return userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
    @Transactional
    public void completeMyDataRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.markMydataRegistered();
    }
}