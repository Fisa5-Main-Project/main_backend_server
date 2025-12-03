package com.know_who_how.main_server.global.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
// Spring Data Elasticsearch Repository를 사용하지 않으므로 활성화하지 않음.
// 만약 @EnableElasticsearchRepositories를 사용하려면 basePackages를 명시하여 스캔 범위를 제한해야 함.
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.client.reactive.endpoints}")
    private String elasticsearchEndpoints; // application.yml에서 엔드포인트 설정 주입

    @Bean
    public RestClient getRestClient() {
        // 엔드포인트 문자열 파싱 (예: "localhost:9200")
        String[] parts = elasticsearchEndpoints.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        return RestClient.builder(
                new HttpHost(host, port, "http"))
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
