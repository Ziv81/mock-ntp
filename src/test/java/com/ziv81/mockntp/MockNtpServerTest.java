package com.ziv81.mockntp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class MockNtpServerTest {

    @Test
    void respondsWithConfiguredFixedTime() throws Exception {
        Instant fixedTime = Instant.parse("2026-05-12T00:00:00Z");
        Instant clientTime = Instant.parse("2025-01-01T00:00:00Z");

        MockNtpProperties properties = new MockNtpProperties();
        properties.setPort(0);
        properties.setFixedTime(fixedTime);

        MockNtpServer server = new MockNtpServer(properties);
        server.start();

        try (DatagramSocket client = new DatagramSocket()) {
            client.setSoTimeout(2_000);

            byte[] request = new byte[48];
            request[0] = 0x1B;
            request[2] = 6;
            byte[] clientTimestamp = NtpPacketCodec.toTimestampBytes(clientTime);
            System.arraycopy(clientTimestamp, 0, request, 40, 8);

            DatagramPacket outbound = new DatagramPacket(request, request.length, InetAddress.getLoopbackAddress(), server.getLocalPort());
            client.send(outbound);

            byte[] response = new byte[48];
            DatagramPacket inbound = new DatagramPacket(response, response.length);
            client.receive(inbound);

            assertThat(inbound.getLength()).isEqualTo(48);
            assertThat(response[0] & 0xFF).isEqualTo(0x24);
            assertThat(response[1] & 0xFF).isEqualTo(1);
            assertThat(response[2] & 0xFF).isEqualTo(6);
            assertThat(Arrays.copyOfRange(response, 24, 32)).isEqualTo(clientTimestamp);
            assertThat(NtpPacketCodec.readTimestamp(response, 16)).isEqualTo(fixedTime);
            assertThat(NtpPacketCodec.readTimestamp(response, 32)).isEqualTo(fixedTime);
            assertThat(NtpPacketCodec.readTimestamp(response, 40)).isEqualTo(fixedTime);
        }
        finally {
            server.stop();
        }
    }

    @Test
    void resolvesCyclingTimeWithinConfiguredRange() {
        Instant startTime = Instant.parse("2026-05-12T00:00:00Z");
        Duration cycleDuration = Duration.ofSeconds(10);

        assertThat(MockNtpServer.resolveResponseTime(startTime, cycleDuration, 0L))
                .isEqualTo(startTime);
        assertThat(MockNtpServer.resolveResponseTime(startTime, cycleDuration, 3_000_000_000L))
                .isEqualTo(startTime.plusSeconds(3));
        assertThat(MockNtpServer.resolveResponseTime(startTime, cycleDuration, 12_000_000_000L))
                .isEqualTo(startTime.plusSeconds(2));
    }

    @Test
    void respondsWithCyclingTimeRange() throws Exception {
        Instant startTime = Instant.parse("2026-05-12T00:00:00Z");
        Instant endTime = startTime.plusSeconds(2);
        Instant clientTime = Instant.parse("2025-01-01T00:00:00Z");

        MockNtpProperties properties = new MockNtpProperties();
        properties.setPort(0);
        properties.setStartTime(startTime);
        properties.setEndTime(endTime);

        MockNtpServer server = new MockNtpServer(properties);
        server.start();

        try (DatagramSocket client = new DatagramSocket()) {
            client.setSoTimeout(2_000);

            Instant firstResponseTime = sendRequestAndReadTransmitTime(client, server.getLocalPort(), clientTime);
            Thread.sleep(1_100);
            Instant secondResponseTime = sendRequestAndReadTransmitTime(client, server.getLocalPort(), clientTime);

            assertThat(firstResponseTime).isBetween(startTime, endTime);
            assertThat(secondResponseTime).isBetween(startTime, endTime);
            assertThat(secondResponseTime).isAfter(firstResponseTime);
        }
        finally {
            server.stop();
        }
    }

    private static Instant sendRequestAndReadTransmitTime(DatagramSocket client, int serverPort, Instant clientTime) throws Exception {
        byte[] request = new byte[48];
        request[0] = 0x1B;
        request[2] = 6;
        byte[] clientTimestamp = NtpPacketCodec.toTimestampBytes(clientTime);
        System.arraycopy(clientTimestamp, 0, request, 40, 8);

        DatagramPacket outbound = new DatagramPacket(request, request.length, InetAddress.getLoopbackAddress(), serverPort);
        client.send(outbound);

        byte[] response = new byte[48];
        DatagramPacket inbound = new DatagramPacket(response, response.length);
        client.receive(inbound);
        return NtpPacketCodec.readTimestamp(response, 40);
    }
}
