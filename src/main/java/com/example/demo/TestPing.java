package com.example.demo;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TestPing {
    private static final String SERVER_IP = "80.240.23.72";
    //private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 51888;

    public void start() {
        try {
            DatagramSocket socket = new DatagramSocket();
            ExecutorService sendPool = Executors.newFixedThreadPool(8);

            new Thread(() -> receive(socket)).start();

            for (int i = 0; i < 100; i++) {
                int finalI = i;
                sendPool.execute(() -> send(socket, finalI));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void send(DatagramSocket socket, int i) {
        try {
            socket.send(getPacket(i));
            System.out.println("PING sent " + i);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void receive(DatagramSocket socket) {
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);
                printResponse(response);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
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

    private DatagramPacket getPacket(int i) {
        String message = "PING " + i;
        byte[] data = message.getBytes();
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(
                    data,
                    data.length,
                    InetAddress.getByName(SERVER_IP),
                    PORT
            );
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        }
        return packet;
    }
}
