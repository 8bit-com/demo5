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
    private static final int FRAME_VPN = 2;
    private static final int ICMP_PROTOCOL = 1;
    private static final int ICMP_ECHO_REQUEST = 8;

    private final Consumer<byte[]> packetConsumer;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private volatile boolean running;

    public void start() {
        try {
            socket = new Socket(HOST, PORT);
            socket.setTcpNoDelay(true);

            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            running = true;

            System.out.println("TCP VPN transport connected to " + HOST + ":" + PORT);

            Thread receiverThread = new Thread(this::receiveLoop, "tcp-packet-receiver");
            receiverThread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void send(byte[] packet) {
        if (!running || socket == null || socket.isClosed()) {
            System.out.println("TCP VPN SEND SKIP: transport is not running");
            return;
        }

        if (packet.length > MAX_PACKET_SIZE) {
            System.out.println("TCP packet skipped, size=" + packet.length);
            return;
        }

        try {
            printPingSend(packet);
            output.writeByte(FRAME_VPN);
            output.writeInt(packet.length);
            output.write(packet);
            output.flush();
        } catch (Exception e) {
            System.out.println("TCP packet send error: " + e.getMessage());
            stop();
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
                int frameType = input.readUnsignedByte();
                int size = input.readInt();

                if (frameType != FRAME_VPN) {
                    System.out.println("TCP VPN wrong frame type: " + frameType);
                    stop();
                    return;
                }

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

    private void printPingSend(byte[] packet) {
        if (packet.length < 28) {
            return;
        }

        int headerLength = (packet[0] & 0x0F) * 4;
        int protocol = packet[9] & 0xFF;

        if (protocol != ICMP_PROTOCOL || packet.length < headerLength + 8) {
            return;
        }

        int icmpType = packet[headerLength] & 0xFF;
        if (icmpType != ICMP_ECHO_REQUEST) {
            return;
        }

        System.out.println("TCP VPN SEND PING: size=" + packet.length);
    }
}
