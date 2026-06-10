package com.example.demo;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

@Service
public class Client {

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        String serverHost = "127.0.0.1";
        int serverPort = 51888;

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000);

            byte[] ping = "PING".getBytes(StandardCharsets.UTF_8);

            DatagramPacket request = new DatagramPacket(
                    ping,
                    ping.length,
                    InetAddress.getByName(serverHost),
                    serverPort
            );

            socket.send(request);
            System.out.println("PING sent");

            byte[] buffer = new byte[2048];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);

            socket.receive(response);

            String message = new String(
                    response.getData(),
                    response.getOffset(),
                    response.getLength(),
                    StandardCharsets.UTF_8
            );

            System.out.println("Received: " + message);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
