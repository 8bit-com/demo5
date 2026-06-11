package com.example.demo.wintun;

import com.example.demo.tcp.TcpPacketTransport;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class WintunTcpForwarder {

    private static final int SESSION_CAPACITY = 4 * 1024 * 1024;
    private static final int EMPTY_READ_SLEEP_MS = 10;
    private static final int ICMP_PROTOCOL = 1;
    private static final int ICMP_ECHO_REQUEST = 8;
    private static final int ICMP_ECHO_REPLY = 0;

    private final Pointer adapter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final TcpPacketTransport tcpPacketTransport = new TcpPacketTransport(this::writeToTun);

    private Pointer session;

    public void start() {
        session = Wintun.INSTANCE.WintunStartSession(adapter, SESSION_CAPACITY);
        tcpPacketTransport.start();

        System.out.println("Wintun TCP forwarder started");

        try {
            while (running.get()) {
                byte[] packet = readFromTun();

                if (packet == null) {
                    sleepQuietly();
                    continue;
                }

                printPing("CLIENT PING REQUEST", packet, ICMP_ECHO_REQUEST);
                tcpPacketTransport.send(packet);
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running.set(false);
        tcpPacketTransport.stop();

        if (session != null) {
            Wintun.INSTANCE.WintunEndSession(session);
            session = null;
        }
    }

    private byte[] readFromTun() {
        IntByReference packetSize = new IntByReference();
        Pointer packetPointer = Wintun.INSTANCE.WintunReceivePacket(session, packetSize);

        if (packetPointer == null) {
            return null;
        }

        try {
            return packetPointer.getByteArray(0, packetSize.getValue());
        } finally {
            Wintun.INSTANCE.WintunReleaseReceivePacket(session, packetPointer);
        }
    }

    private void writeToTun(byte[] packet) {
        if (session == null) {
            return;
        }

        printPing("CLIENT PING REPLY", packet, ICMP_ECHO_REPLY);

        Pointer sendPointer = Wintun.INSTANCE.WintunAllocateSendPacket(session, packet.length);

        if (sendPointer == null) {
            System.out.println("Wintun send packet allocation failed");
            return;
        }

        sendPointer.write(0, packet, 0, packet.length);
        Wintun.INSTANCE.WintunSendPacket(session, sendPointer);
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(EMPTY_READ_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void printPing(String prefix, byte[] packet, int expectedType) {
        if (packet.length < 28) {
            return;
        }

        int headerLength = (packet[0] & 0x0F) * 4;
        int protocol = packet[9] & 0xFF;

        if (protocol != ICMP_PROTOCOL || packet.length < headerLength + 8) {
            return;
        }

        int icmpType = packet[headerLength] & 0xFF;
        if (icmpType != expectedType) {
            return;
        }

        int icmpId = ((packet[headerLength + 4] & 0xFF) << 8) | (packet[headerLength + 5] & 0xFF);
        int icmpSeq = ((packet[headerLength + 6] & 0xFF) << 8) | (packet[headerLength + 7] & 0xFF);

        System.out.println(prefix
                + ": src=" + ipToString(packet, 12)
                + ", dst=" + ipToString(packet, 16)
                + ", type=" + icmpType
                + ", id=" + icmpId
                + ", seq=" + icmpSeq
                + ", size=" + packet.length);
    }

    private String ipToString(byte[] packet, int offset) {
        return (packet[offset] & 0xFF) + "."
                + (packet[offset + 1] & 0xFF) + "."
                + (packet[offset + 2] & 0xFF) + "."
                + (packet[offset + 3] & 0xFF);
    }
}
