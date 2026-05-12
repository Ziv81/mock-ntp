package com.ziv81.mockntp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class MockNtpPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "mock.ntp.port=10123",
                    "mock.ntp.fixed-time=2026-05-12T00:00:00Z");

    @Test
    void bindsFixedTimeFromConfiguration() {
        contextRunner.run(context -> {
            MockNtpProperties properties = context.getBean(MockNtpProperties.class);
            assertThat(properties.getPort()).isEqualTo(10123);
            assertThat(properties.getFixedTime()).isEqualTo(Instant.parse("2026-05-12T00:00:00Z"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MockNtpProperties.class)
    static class TestConfiguration {
    }
}
