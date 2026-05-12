package com.ziv81.mockntp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
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

    public MockNtpServer(MockNtpProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        if (properties.getFixedTime() == null) {
            throw new IllegalStateException("mock.ntp.fixed-time must be configured");
        }

        try {
            socket = new DatagramSocket(properties.getPort());
        }
        catch (SocketException exception) {
            throw new IllegalStateException("Failed to start mock NTP server", exception);
        }

        executor = Executors.newSingleThreadExecutor(runnable -> {
            return new Thread(runnable, "mock-ntp-server");
        });
        running = true;
        executor.submit(this::listen);
        logger.info("Mock NTP server listening on UDP port {} with fixed time {}", socket.getLocalPort(), properties.getFixedTime());
    }

    private void listen() {
        while (running) {
            try {
                byte[] requestBuffer = new byte[256];
                DatagramPacket request = new DatagramPacket(requestBuffer, requestBuffer.length);
                socket.receive(request);
                byte[] responseBuffer = NtpPacketCodec.createResponse(request.getData(), request.getLength(), properties.getFixedTime());
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
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    int getLocalPort() {
        DatagramSocket currentSocket = socket;
        return currentSocket == null ? -1 : currentSocket.getLocalPort();
    }
}
