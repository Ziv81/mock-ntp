package com.ziv81.mockntp;

import java.time.Instant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mock.ntp")
public class MockNtpProperties {

    private int port = 123;
    private Instant fixedTime;

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
}
