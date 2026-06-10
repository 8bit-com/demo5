package com.example.demo.udp;

import lombok.RequiredArgsConstructor;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class UdpPacketTransport {

    private static final String SERVER_HOST = "80.240.23.72";
    private static final int SERVER_PORT = 51888;
    private static final int BUFFER_SIZE = 2048;
    private static final int RECEIVE_TIMEOUT_MS = 100;

    private final Consumer<byte[]> packetConsumer;

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private volatile boolean running;

    public void start() {
        try {
            serverAddress = InetAddress.getByName(SERVER_HOST);
            socket = new DatagramSocket();
            socket.setSoTimeout(RECEIVE_TIMEOUT_MS);
            running = true;

            Thread receiverThread = new Thread(this::receiveLoop, "udp-server-receiver");
            receiverThread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void send(byte[] packet) {
        try {
            socket.send(new DatagramPacket(
                    packet,
                    packet.length,
                    serverAddress,
                    SERVER_PORT
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        running = false;

        if (socket != null) {
            socket.close();
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] data = copy(packet);
                packetConsumer.accept(data);
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (running) {
                    System.out.println("UDP receive error: " + e.getMessage());
                }
            }
        }
    }

    private byte[] copy(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }
}
