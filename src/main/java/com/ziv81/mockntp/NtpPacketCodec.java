package com.ziv81.mockntp;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

final class NtpPacketCodec {

    private static final int NTP_PACKET_SIZE = 48;
    private static final long NTP_EPOCH_OFFSET_SECONDS = 2_208_988_800L;

    private NtpPacketCodec() {
    }

    static byte[] createResponse(byte[] request, int requestLength, Instant fixedTime) {
        if (requestLength < NTP_PACKET_SIZE) {
            throw new IllegalArgumentException("NTP request must be at least 48 bytes");
        }

        byte[] response = new byte[NTP_PACKET_SIZE];
        response[0] = 0x24;
        response[1] = 0x01;
        response[2] = request[2];
        response[3] = (byte) 0xEC;
        System.arraycopy("MOCK".getBytes(StandardCharsets.US_ASCII), 0, response, 12, 4);

        byte[] fixedTimestamp = toTimestampBytes(fixedTime);
        System.arraycopy(fixedTimestamp, 0, response, 16, 8);
        System.arraycopy(Arrays.copyOfRange(request, 40, 48), 0, response, 24, 8);
        System.arraycopy(fixedTimestamp, 0, response, 32, 8);
        System.arraycopy(fixedTimestamp, 0, response, 40, 8);
        return response;
    }

    static byte[] toTimestampBytes(Instant instant) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        long seconds = instant.getEpochSecond() + NTP_EPOCH_OFFSET_SECONDS;
        long fraction = (instant.getNano() * 0x1_0000_0000L) / 1_000_000_000L;
        buffer.putInt((int) seconds);
        buffer.putInt((int) fraction);
        return buffer.array();
    }

    static Instant readTimestamp(byte[] packet, int offset) {
        long seconds = Integer.toUnsignedLong(ByteBuffer.wrap(packet, offset, 4).getInt());
        long fraction = Integer.toUnsignedLong(ByteBuffer.wrap(packet, offset + 4, 4).getInt());
        long epochSeconds = seconds - NTP_EPOCH_OFFSET_SECONDS;
        long nanos = (fraction * 1_000_000_000L) >>> 32;
        return Instant.ofEpochSecond(epochSeconds, nanos);
    }
}
