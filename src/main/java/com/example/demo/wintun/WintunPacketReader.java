package com.example.demo.wintun;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class WintunPacketReader {

    private static final int SESSION_CAPACITY = 4 * 1024 * 1024;
    private static final int EMPTY_READ_SLEEP_MS = 10;

    private final Pointer adapter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Pointer session;

    public void readLoop() {
        session = Wintun.INSTANCE.WintunStartSession(
                adapter,
                SESSION_CAPACITY
        );

        System.out.println("Wintun session started");

        try {
            while (running.get()) {
                byte[] packet = readPacket(session);

                if (packet == null) {
                    sleepQuietly();
                    continue;
                }

                System.out.println("TUN packet received, size=" + packet.length);
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running.set(false);

        if (session != null) {
            Wintun.INSTANCE.WintunEndSession(session);
            session = null;
        }
    }

    private byte[] readPacket(Pointer session) {
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

    private void sleepQuietly() {
        try {
            Thread.sleep(EMPTY_READ_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
