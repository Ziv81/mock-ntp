package com.ziv81.mockntp;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mock.ntp")
public class MockNtpProperties {

    private int port = 123;
    private Instant fixedTime;
    private Instant startTime;
    private Instant endTime;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Instant getFixedTime() {
        return fixedTime;
    }

    public void setFixedTime(Instant fixedTime) {
        this.fixedTime = fixedTime;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    boolean usesFixedTime() {
        return fixedTime != null;
    }

    boolean usesTimeRange() {
        return startTime != null || endTime != null;
    }

    Duration getCycleDuration() {
        return Duration.between(startTime, endTime);
    }

    void validate() {
        if (usesFixedTime()) {
            if (usesTimeRange()) {
                throw new IllegalStateException("Configure either mock.ntp.fixed-time or mock.ntp.start-time/mock.ntp.end-time");
            }
            return;
        }

        if (!usesTimeRange()) {
            throw new IllegalStateException("mock.ntp.fixed-time or mock.ntp.start-time/mock.ntp.end-time must be configured");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalStateException("mock.ntp.start-time and mock.ntp.end-time must both be configured");
        }

        Duration cycleDuration = getCycleDuration();
        if (cycleDuration.isZero() || cycleDuration.isNegative()) {
            throw new IllegalStateException("mock.ntp.end-time must be after mock.ntp.start-time");
        }

        try {
            cycleDuration.toNanos();
        }
        catch (ArithmeticException exception) {
            throw new IllegalStateException("mock.ntp.start-time to mock.ntp.end-time range is too large", exception);
        }
    }
}
