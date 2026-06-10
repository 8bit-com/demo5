package com.example.demo.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpPacketSender {

    private static final String SERVER_HOST = "80.240.23.72";
    private static final int SERVER_PORT = 51888;

    private final InetAddress serverAddress;
    private final DatagramSocket socket;

    public UdpPacketSender() {
        try {
            this.serverAddress = InetAddress.getByName(SERVER_HOST);
            this.socket = new DatagramSocket();
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

    public void close() {
        socket.close();
    }
}
