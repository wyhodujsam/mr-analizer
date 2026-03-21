package com.mranalizer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MrAnalizerApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully.
        // The main class is already covered by @SpringBootTest loading the context.
    }
}
