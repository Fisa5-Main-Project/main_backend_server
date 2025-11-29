package com.know_who_how.main_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mydata")
@Getter
@Setter
public class MydataProperties {

    private AsProperties as;
    private RsProperties rs;

    private String clientId;
    private String clientSecret;
    private String redirectUri;

    @Getter @Setter
    public static class AsProperties {
        // AS 서버 엔드포인트
        private String authorizeUri;
        // mydata.as.token-uri
        private String tokenUri;
    }

    @Getter @Setter
    public static class RsProperties {
        // my-client-id.rs.my-data-api
        private String baseUrl;
    }
}