package com.example.demo;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Service
public class TestPing {
    public void start() {
        try {
            String serverIp = "80.240.23.72"; // сюда IP твоего сервера
            int port = 51888;

            DatagramSocket socket = new DatagramSocket();

            byte[] data = "PING".getBytes();

            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    InetAddress.getByName(serverIp),
                    port
            );

            socket.send(packet);

            System.out.println("PING sent");

            byte[] buffer = new byte[1024];

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);

            socket.receive(response);

            String text = new String(
                    response.getData(),
                    0,
                    response.getLength()
            );

            System.out.println("Response: " + text);

            socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
