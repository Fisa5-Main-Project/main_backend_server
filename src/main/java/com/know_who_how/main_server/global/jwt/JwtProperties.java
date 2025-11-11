package com.know_who_how.main_server.global.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String header;
    private String secret;
    private long accessTokenValidityInSeconds;
    private long refreshTokenValidityInSeconds;

}