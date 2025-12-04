package com.know_who_how.main_server.global.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
// Spring Data Elasticsearch Repository를 사용하지 않으므로 활성화하지 않음.
// 만약 @EnableElasticsearchRepositories를 사용하려면 basePackages를 명시하여 스캔 범위를 제한해야 함.
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String[] elasticsearchUris; // application.yml에서 엔드포인트 설정 주입 (배열로 변경)

    @Bean
    public RestClient getRestClient() {
        // spring.elasticsearch.uris는 쉼표로 구분된 URI 목록을 가질 수 있으므로, 각 URI를 HttpHost로 변환
        HttpHost[] httpHosts = Arrays.stream(elasticsearchUris)
                .map(HttpHost::create)
                .filter(Objects::nonNull) // 유효하지 않은 URI는 필터링
                .toArray(HttpHost[]::new);

        if (httpHosts.length == 0) {
            throw new IllegalStateException("No valid Elasticsearch URIs configured.");
        }

        // RestClient.builder는 여러 HttpHost를 인자로 받을 수 있음
        return RestClient.builder(httpHosts)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(5000) // 연결 타임아웃 5초
                        .setSocketTimeout(60000)) // 소켓 타임아웃 60초
                .build();
    }

    @Bean
    public ElasticsearchTransport getElasticsearchTransport() {
        // RestClient를 사용하여 전송 계층 생성
        return new RestClientTransport(
                getRestClient(),
                new JacksonJsonpMapper()
        );
    }

    @Bean
    public ElasticsearchClient getElasticsearchClient() {
        // 전송 계층을 사용하여 ElasticsearchClient 생성
        return new ElasticsearchClient(getElasticsearchTransport());
    }
}
