package com.ziv81.mockntp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class MockNtpPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsFixedTimeFromConfiguration() {
        contextRunner.withPropertyValues(
                "mock.ntp.port=10123",
                "mock.ntp.fixed-time=2026-05-12T00:00:00Z")
                .run(context -> {
            MockNtpProperties properties = context.getBean(MockNtpProperties.class);
            assertThat(properties.getPort()).isEqualTo(10123);
            assertThat(properties.getFixedTime()).isEqualTo(Instant.parse("2026-05-12T00:00:00Z"));
            assertThat(properties.getStartTime()).isNull();
            assertThat(properties.getEndTime()).isNull();
        });
    }

    @Test
    void bindsCyclingTimeRangeFromConfiguration() {
        contextRunner.withPropertyValues(
                "mock.ntp.port=10123",
                "mock.ntp.start-time=2026-05-12T00:00:00Z",
                "mock.ntp.end-time=2026-05-12T00:05:00Z")
                .run(context -> {
                    MockNtpProperties properties = context.getBean(MockNtpProperties.class);
                    assertThat(properties.getPort()).isEqualTo(10123);
                    assertThat(properties.getFixedTime()).isNull();
                    assertThat(properties.getStartTime()).isEqualTo(Instant.parse("2026-05-12T00:00:00Z"));
                    assertThat(properties.getEndTime()).isEqualTo(Instant.parse("2026-05-12T00:05:00Z"));
                });
    }

    @Test
    void rejectsMixedFixedTimeAndCyclingRange() {
        MockNtpProperties properties = new MockNtpProperties();
        properties.setFixedTime(Instant.parse("2026-05-12T00:00:00Z"));
        properties.setStartTime(Instant.parse("2026-05-12T00:00:00Z"));
        properties.setEndTime(Instant.parse("2026-05-12T00:05:00Z"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configure either mock.ntp.fixed-time or mock.ntp.start-time/mock.ntp.end-time");
    }

    @Test
    void rejectsCyclingRangeWithoutBothBounds() {
        MockNtpProperties properties = new MockNtpProperties();
        properties.setStartTime(Instant.parse("2026-05-12T00:00:00Z"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mock.ntp.start-time and mock.ntp.end-time must both be configured");
    }

    @Test
    void rejectsCyclingRangeWhenEndIsNotAfterStart() {
        MockNtpProperties properties = new MockNtpProperties();
        properties.setStartTime(Instant.parse("2026-05-12T00:05:00Z"));
        properties.setEndTime(Instant.parse("2026-05-12T00:00:00Z"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mock.ntp.end-time must be after mock.ntp.start-time");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MockNtpProperties.class)
    static class TestConfiguration {
    }
}
