package com.know_who_how.main_server.user.service;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.dto.UserAssetResponseDto;
import com.know_who_how.main_server.user.dto.UserResponseDto;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final AssetsRepository assetsRepository;

    public UserResponseDto getUserInfo(User user) {
        return UserResponseDto.from(user);
    }

    public List<UserAssetResponseDto> getUserAssets(User user) {
        List<Asset> assets = assetsRepository.findByUser(user);
        return assets.stream()
                .map(UserAssetResponseDto::from)
                .collect(Collectors.toList());
    }
}
