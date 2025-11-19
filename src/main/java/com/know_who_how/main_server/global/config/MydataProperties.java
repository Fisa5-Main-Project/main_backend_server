package com.know_who_how.main_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "my-client-id")
@Getter
@Setter
public class MydataProperties {

    private AsProperties as;
    private RsProperties rs;

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private int timeoutMs;

    @Getter @Setter
    public static class AsProperties {
        // mydata.as.token-uri
        private String tokenUri;
    }

    @Getter @Setter
    public static class RsProperties {
        // mydata.rs.pension-api, mydata.rs.asset-api
        private String pensionApi;
        private String assetApi;
    }
}
