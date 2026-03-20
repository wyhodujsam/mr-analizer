package com.mranalizer.adapter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ProviderConfig {

    @Bean
    public WebClient gitHubWebClient(
            @Value("${mr-analizer.github.api-url:https://api.github.com}") String apiUrl,
            @Value("${mr-analizer.github.token:}") String token) {

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");

        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        return builder.build();
    }
}
