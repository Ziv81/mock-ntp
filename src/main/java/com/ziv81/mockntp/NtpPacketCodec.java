package com.ziv81.mockntp;

import java.nio.ByteBuffer;
import java.time.Instant;

final class NtpPacketCodec {

    private static final int NTP_PACKET_SIZE = 48;
    private static final long NTP_EPOCH_OFFSET_SECONDS = 2_208_988_800L;
    // 16.16 fixed-point values for small, non-zero root delay/dispersion in server replies.
    private static final int ROOT_DELAY_FIXED_POINT = 0x0000_0100;
    private static final int ROOT_DISPERSION_FIXED_POINT = 0x0000_0200;
    // Use slightly older reference/receive timestamps so response timing looks realistic.
    private static final int REFERENCE_TIMESTAMP_OFFSET_SECONDS = 2;
    private static final int RECEIVE_TIMESTAMP_OFFSET_SECONDS = 1;
    private static final int REFERENCE_ID_LOCALHOST = 0x7F00_0001;

    private NtpPacketCodec() {
    }

    static byte[] createResponse(byte[] request, int requestLength, Instant fixedTime) {
        if (requestLength < NTP_PACKET_SIZE) {
            throw new IllegalArgumentException("NTP request must be at least 48 bytes");
        }

        byte[] response = new byte[NTP_PACKET_SIZE];
        response[0] = 0x24;
        response[1] = 0x02;
        response[2] = request[2];
        response[3] = (byte) 0xEC;
        ByteBuffer.wrap(response, 4, 4).putInt(ROOT_DELAY_FIXED_POINT);
        ByteBuffer.wrap(response, 8, 4).putInt(ROOT_DISPERSION_FIXED_POINT);
        ByteBuffer.wrap(response, 12, 4).putInt(REFERENCE_ID_LOCALHOST);

        byte[] referenceTimestamp = toTimestampBytes(fixedTime.minusSeconds(REFERENCE_TIMESTAMP_OFFSET_SECONDS));
        byte[] receiveTimestamp = toTimestampBytes(fixedTime.minusSeconds(RECEIVE_TIMESTAMP_OFFSET_SECONDS));
        byte[] transmitTimestamp = toTimestampBytes(fixedTime);
        System.arraycopy(referenceTimestamp, 0, response, 16, 8);
        System.arraycopy(request, 40, response, 24, 8);
        System.arraycopy(receiveTimestamp, 0, response, 32, 8);
        System.arraycopy(transmitTimestamp, 0, response, 40, 8);
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
