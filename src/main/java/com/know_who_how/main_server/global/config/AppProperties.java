package com.know_who_how.main_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 전역 설정을 외부 설정으로 주입받기 위한 프로퍼티.
 * 현재는 프론트엔드 베이스 URL을 관리한다.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    /**
     * 프론트엔드 베이스 URL (예: http://localhost:3000)
     */
    private String frontendBaseUrl;

    /**
     * CORS에 허용할 오리진 목록 (예: http://localhost:3000, https://knowwhohow.site)
     */
    private java.util.List<String> allowedOrigins;
}
