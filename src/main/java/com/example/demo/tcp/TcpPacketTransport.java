package com.example.demo.tcp;

import lombok.RequiredArgsConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class TcpPacketTransport {

    private static final String HOST = "80.240.23.72";
    private static final int PORT = 51890;
    private static final int MAX_PACKET_SIZE = 1200;
    private static final int TIMEOUT_MS = 5000;

    private final Consumer<byte[]> packetConsumer;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private volatile boolean running;

    public void start() {
        try {
            socket = new Socket(HOST, PORT);
            socket.setSoTimeout(TIMEOUT_MS);
            socket.setTcpNoDelay(true);

            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            running = true;

            Thread receiverThread = new Thread(this::receiveLoop, "tcp-packet-receiver");
            receiverThread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void send(byte[] packet) {
        if (packet.length > MAX_PACKET_SIZE) {
            System.out.println("TCP packet skipped, size=" + packet.length);
            return;
        }

        try {
            output.writeInt(packet.length);
            output.write(packet);
            output.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        running = false;

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void receiveLoop() {
        while (running) {
            try {
                int size = input.readInt();

                if (size <= 0 || size > MAX_PACKET_SIZE) {
                    stop();
                    return;
                }

                byte[] packet = input.readNBytes(size);

                if (packet.length != size) {
                    stop();
                    return;
                }

                packetConsumer.accept(packet);
            } catch (Exception e) {
                if (running) {
                    System.out.println("TCP packet receive error: " + e.getMessage());
                }
                stop();
            }
        }
    }
}
