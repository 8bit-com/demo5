package com.example.demo;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

@Service
public class TestPing {
    private static final String SERVER_IP = "80.240.23.72";
    private static final int PORT = 51888;

    public void start() {
        try {
            DatagramSocket socket = new DatagramSocket();

            for (int i = 0; i < 1000; i++) {
                send(socket, i);

                receive(socket);
            }

            socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void send(DatagramSocket socket, int i) throws IOException {
        socket.send(getPacket(i));
        System.out.println("PING sent " + i);
    }

    private void receive(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[1024];

        DatagramPacket response = new DatagramPacket(buffer, buffer.length);

        socket.receive(response);

        printResponse(response);
    }

    private void printResponse(DatagramPacket response) {
        String text = new String(
                response.getData(),
                response.getOffset(),
                response.getLength(),
                StandardCharsets.UTF_8
        );
        System.out.println("Response: " + text);
    }

    private DatagramPacket getPacket(int i) throws UnknownHostException {
        String message = "PING " + i;
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getByName(SERVER_IP),
                PORT
        );
        return packet;
    }
}
