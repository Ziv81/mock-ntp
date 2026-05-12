package com.ziv81.mockntp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
}
