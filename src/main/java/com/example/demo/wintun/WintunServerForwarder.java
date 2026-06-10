package com.example.demo.wintun;

import com.example.demo.udp.UdpPacketTransport;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class WintunServerForwarder {

    private static final int SESSION_CAPACITY = 4 * 1024 * 1024;
    private static final int EMPTY_READ_SLEEP_MS = 10;

    private final Pointer adapter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final UdpPacketTransport udpPacketTransport = new UdpPacketTransport(this::writeToTun);

    private Pointer session;

    public void start() {
        session = Wintun.INSTANCE.WintunStartSession(adapter, SESSION_CAPACITY);
        udpPacketTransport.start();

        System.out.println("Wintun server forwarder started");

        try {
            while (running.get()) {
                byte[] packet = readFromTun();

                if (packet == null) {
                    sleepQuietly();
                    continue;
                }

                udpPacketTransport.send(packet);
                System.out.println("TUN packet sent to server, size=" + packet.length);
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running.set(false);
        udpPacketTransport.stop();

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

        Pointer sendPointer = Wintun.INSTANCE.WintunAllocateSendPacket(session, packet.length);

        if (sendPointer == null) {
            System.out.println("Wintun send packet allocation failed");
            return;
        }

        sendPointer.write(0, packet, 0, packet.length);
        Wintun.INSTANCE.WintunSendPacket(session, sendPointer);

        System.out.println("Server packet written to TUN, size=" + packet.length);
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(EMPTY_READ_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
