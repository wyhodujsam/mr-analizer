package com.mranalizer.adapter.config;

import com.mranalizer.domain.port.out.LlmAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LlmConfigTest {

    @Autowired
    private LlmAnalyzer llmAnalyzer;

    @Test
    void llmAnalyzerBeanExists() {
        assertNotNull(llmAnalyzer);
    }

    @Test
    void noOpAdapterIsActiveInTestProfile() {
        assertEquals("none", llmAnalyzer.getProviderName());
    }
}
