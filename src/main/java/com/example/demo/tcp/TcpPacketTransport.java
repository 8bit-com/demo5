package com.example.demo.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class TcpPacketTransport {

    private static final String HOST = "80.240.23.72";
    private static final int PORT = 51890;
    private static final int MAX_PACKET_SIZE = 1200;
    private static final int FRAME_VPN = 2;
    private static final int TIMEOUT_MS = 5000;

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

            System.out.println("TCP VPN transport connected to " + HOST + ":" + PORT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized byte[] request(byte[] packet) {
        if (!running || socket == null || socket.isClosed()) {
            System.out.println("TCP VPN REQUEST SKIP: transport is not running");
            return null;
        }

        if (packet.length > MAX_PACKET_SIZE) {
            System.out.println("TCP packet skipped, size=" + packet.length);
            return null;
        }

        try {
            output.writeByte(FRAME_VPN);
            output.writeInt(packet.length);
            output.write(packet);
            output.flush();

            int responseFrameType = input.readUnsignedByte();
            int responseSize = input.readInt();

            if (responseFrameType != FRAME_VPN) {
                System.out.println("TCP VPN wrong frame type: " + responseFrameType);
                stop();
                return null;
            }

            if (responseSize <= 0 || responseSize > MAX_PACKET_SIZE) {
                stop();
                return null;
            }

            byte[] response = input.readNBytes(responseSize);

            if (response.length != responseSize) {
                stop();
                return null;
            }

            return response;
        } catch (Exception e) {
            System.out.println("TCP VPN request error: " + e.getMessage());
            stop();
            return null;
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
}
