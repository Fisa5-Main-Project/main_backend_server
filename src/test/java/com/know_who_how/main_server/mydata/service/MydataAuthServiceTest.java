package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.config.MydataProperties;
import com.know_who_how.main_server.global.entity.Mydata.Mydata;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.util.RedisUtil;
import com.know_who_how.main_server.mydata.repository.MydataRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MydataAuthServiceTest {

    @Mock
    private MydataRepository mydataRepository;
    @Mock
    private WebClient mydataAuthWebClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisUtil redisUtil;

    private MydataProperties props;

    private MydataAuthService service;

    @BeforeEach
    void setUp() {
        props = new MydataProperties();
        MydataProperties.AsProperties as = new MydataProperties.AsProperties();
        as.setAuthorizeUri("http://auth.example.com/oauth2/authorize");
        as.setTokenUri("http://auth.example.com/oauth2/token");
        props.setAs(as);
        props.setClientId("client-id");
        props.setClientSecret("client-secret");
        props.setRedirectUri("http://localhost:3000/callback");

        service = Mockito.spy(new MydataAuthService(props, mydataRepository, mydataAuthWebClient, userRepository, redisUtil));
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
    @DisplayName("authorize URL을 정상 조합한다")
    void buildAuthorizeUrl_success() {
        String url = service.buildAuthorizeUrl();

        assertThat(url).contains(props.getAs().getAuthorizeUri());
        assertThat(url).contains("client_id=" + props.getClientId());
        assertThat(url).contains("redirect_uri=" + props.getRedirectUri());
        assertThat(url).contains("scope=");
    }

    @Test
    @DisplayName("authorizeUri 미설정이면 예외 발생")
    void buildAuthorizeUrl_missingAuthorizeUri() {
        props.getAs().setAuthorizeUri(null);
        assertThatThrownBy(() -> service.buildAuthorizeUrl())
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MYDATA_SERVER_ERROR);
    }

    @Test
    @DisplayName("handleCallback - 기존 링크 없음 -> 새 Mydata 저장, Redis 저장")
    void handleCallback_createsNewLink() {
        long userId = 1L;
        Map<String, Object> tokenResponse = Map.of(
                "access_token", "acc",
                "refresh_token", "ref",
                "scope", "openid profile",
                "expires_in", 3600
        );
        doReturn(tokenResponse).when(service).exchangeCodeForToken("code-123");
        when(mydataRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(newUser(userId));

        service.handleCallback(userId, "code-123", "state");

        verify(redisUtil).save(eq("mydata:access:" + userId), eq("acc"), eq(Duration.ofSeconds(3600)));
        verify(mydataRepository).save(any(Mydata.class));
    }

    @Test
    @DisplayName("handleCallback - 기존 링크 존재 -> RefreshToken 업데이트")
    void handleCallback_updatesExistingLink() {
        long userId = 2L;
        Map<String, Object> tokenResponse = Map.of(
                "access_token", "new-acc",
                "refresh_token", "new-ref",
                "scope", "openid profile",
                "expires_in", 1800
        );
        doReturn(tokenResponse).when(service).exchangeCodeForToken("code-abc");
        Mydata existing = Mydata.builder()
                .user(newUser(userId))
                .refreshToken("old-ref")
                .scope("old")
                .build();
        when(mydataRepository.findById(userId)).thenReturn(Optional.of(existing));

        service.handleCallback(userId, "code-abc", "state");

        verify(redisUtil).save(eq("mydata:access:" + userId), eq("new-acc"), eq(Duration.ofSeconds(1800)));
        assertThat(existing.getRefreshToken()).isEqualTo("new-ref");
        verify(mydataRepository, never()).save(any());
    }
}
