package com.mranalizer.adapter.config;

import com.mranalizer.domain.port.out.LlmAnalyzer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    private final LlmAnalyzer llmAnalyzer;

    public LlmConfig(LlmAnalyzer llmAnalyzer) {
        this.llmAnalyzer = llmAnalyzer;
    }

    @PostConstruct
    void logActiveAdapter() {
        log.info("Active LLM adapter: {}", llmAnalyzer.getProviderName());
    }
}
