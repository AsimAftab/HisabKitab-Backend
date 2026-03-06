package com.example.hisabkitabbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")
@Slf4j
public class KeepAliveScheduler {

    private final RestClient restClient;
    private final String baseUrl;

    public KeepAliveScheduler(@Value("${APP_BASE_URL}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.create();
    }

    @Scheduled(fixedRate = 840_000) // 14 minutes
    public void pingHealth() {
        try {
            String url = baseUrl + "/actuator/health";
            restClient.get().uri(url).retrieve().toBodilessEntity();
            log.info("Keep-alive ping successful: {}", url);
        } catch (Exception e) {
            log.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
