package com.know_who_how.main_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "coolsms")
@Getter
@Setter
public class CoolSmsProperties {
    private String apiKey;
    private String apiSecret;
    private String fromNumber; // 발신 번호
}
