package com.know_who_how.main_server.user.service;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetSource;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.mydata.repository.MydataRepository;
import com.know_who_how.main_server.user.dto.UserAssetAddRequest;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.KeywordRepository;
import com.know_who_how.main_server.user.repository.PensionRepository;
import com.know_who_how.main_server.user.repository.RefreshTokenRepository;
import com.know_who_how.main_server.user.repository.UserInfoRepository;
import com.know_who_how.main_server.user.repository.UserKeywordRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import com.know_who_how.main_server.user.repository.UserTermRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AssetsRepository assetsRepository;
    @Mock private UserKeywordRepository userKeywordRepository;
    @Mock private KeywordRepository keywordRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserTermRepository userTermRepository;
    @Mock private UserInfoRepository userInfoRepository;
    @Mock private PensionRepository pensionRepository;
    @Mock private MydataRepository mydataRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자가 직접 입력한 부동산과 자동차는 MANUAL 자산으로 저장한다")
    void addUserAssets_marksAssetsAsManual() {
        User authenticatedUser = org.mockito.Mockito.mock(User.class);
        User managedUser = org.mockito.Mockito.mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(managedUser));

        userService.addUserAssets(authenticatedUser, new UserAssetAddRequest(300_000_000L, 25_000_000L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Asset>> captor = ArgumentCaptor.forClass(List.class);
        verify(assetsRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(Asset::getType, Asset::getSource)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(AssetType.REAL_ESTATE, AssetSource.MANUAL),
                        org.assertj.core.groups.Tuple.tuple(AssetType.AUTOMOBILE, AssetSource.MANUAL)
                );
    }
}
