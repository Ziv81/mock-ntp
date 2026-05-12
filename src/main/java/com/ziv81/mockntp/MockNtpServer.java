package com.ziv81.mockntp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class MockNtpServer implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MockNtpServer.class);

    private final MockNtpProperties properties;

    private volatile boolean running;
    private volatile DatagramSocket socket;
    private volatile ExecutorService executor;
    private volatile long startedAtNanos;
    private volatile Instant cycleStartTime;

    public MockNtpServer(MockNtpProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        properties.validate();

        try {
            socket = new DatagramSocket(properties.getPort());
        }
        catch (SocketException exception) {
            throw new IllegalStateException("Failed to start mock NTP server", exception);
        }

        executor = Executors.newSingleThreadExecutor(runnable -> {
            return new Thread(runnable, "mock-ntp-server");
        });
        startedAtNanos = System.nanoTime();
        cycleStartTime = properties.getStartTime();
        running = true;
        executor.submit(this::listen);
        logger.info("Mock NTP server listening on UDP port {} with {}", socket.getLocalPort(), describeTimeBehavior());
    }

    private void listen() {
        while (running) {
            try {
                byte[] requestBuffer = new byte[256];
                DatagramPacket request = new DatagramPacket(requestBuffer, requestBuffer.length);
                socket.receive(request);
                Instant responseTime = currentResponseTime();
                System.out.printf(
                        "Received NTP request from %s:%d length=%d responding-time=%s%n",
                        request.getAddress().getHostAddress(),
                        request.getPort(),
                        request.getLength(),
                        responseTime);
                byte[] responseBuffer = NtpPacketCodec.createResponse(request.getData(), request.getLength(), responseTime);
                DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length, request.getAddress(), request.getPort());
                socket.send(response);
            }
            catch (IllegalArgumentException exception) {
                logger.warn("Ignoring invalid NTP request: {}", exception.getMessage());
            }
            catch (SocketException exception) {
                if (running) {
                    logger.error("Mock NTP server socket error", exception);
                }
            }
            catch (IOException exception) {
                if (running) {
                    logger.error("Mock NTP server I/O error", exception);
                }
            }
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        startedAtNanos = 0L;
        cycleStartTime = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    int getLocalPort() {
        DatagramSocket currentSocket = socket;
        return currentSocket == null ? -1 : currentSocket.getLocalPort();
    }

    private String describeTimeBehavior() {
        if (properties.usesFixedTime()) {
            return "fixed time " + properties.getFixedTime();
        }
        return "cycling time range " + properties.getStartTime() + " to " + properties.getEndTime();
    }

    private Instant currentResponseTime() {
        if (properties.usesFixedTime()) {
            return properties.getFixedTime();
        }
        return resolveResponseTime(cycleStartTime, properties.getCycleDuration(), System.nanoTime() - startedAtNanos);
    }

    static Instant resolveResponseTime(Instant cycleStartTime, Duration cycleDuration, long elapsedNanos) {
        long cycleNanos = cycleDuration.toNanos();
        long normalizedElapsed = Math.floorMod(elapsedNanos, cycleNanos);
        return cycleStartTime.plusNanos(normalizedElapsed);
    }
}
