package com.mranalizer.adapter.out.llm;

import com.mranalizer.domain.model.LlmAssessment;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "none", matchIfMissing = true)
public class NoOpLlmAdapter implements LlmAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(NoOpLlmAdapter.class);
    private static final String PROVIDER = "none";

    public NoOpLlmAdapter() {
        log.info("LLM adapter: none (no-op). MR analysis will use rule-based scoring only.");
    }

    @Override
    public LlmAssessment analyze(MergeRequest mr) {
        return new LlmAssessment(0.0, null, PROVIDER);
    }

    @Override
    public String getProviderName() {
        return PROVIDER;
    }
}
