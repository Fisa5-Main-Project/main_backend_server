package com.know_who_how.main_server.user.service;

import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.user.dto.AssetDto;
import com.know_who_how.main_server.user.dto.PensionAssetDto;
import com.know_who_how.main_server.user.dto.UserResponseDto;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.PensionRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AssetsRepository assetsRepository;
    private final PensionRepository pensionRepository;

    public UserResponseDto getUserInfo(User user) {
        return UserResponseDto.from(resolveUser(user));
    }

    public List<AssetDto> getUserAssets(User user) {
        var managedUser = resolveUser(user);
        return assetsRepository.findByUser(managedUser)
                .stream()
                .map(AssetDto::from)
                .collect(Collectors.toList());
    }

    public List<PensionAssetDto> getUserPensionAssets(User user) {
        var managedUser = resolveUser(user);
        return assetsRepository.findByUser(managedUser)
                .stream()
                .filter(asset -> asset.getType() == AssetType.PENSION)
                .map(asset -> {
                    var pension = pensionRepository.findById(asset.getAssetId()).orElse(null);
                    return PensionAssetDto.from(asset, pension);
                })
                .collect(Collectors.toList());
    }

    private User resolveUser(User user) {
        if (user == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }
        return userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
