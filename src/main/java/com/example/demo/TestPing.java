package com.example.demo;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

@Service
public class TestPing {
    public void start() {
        try {
            String host = "80.240.23.72";
            int port = 51888;
            int count = 10_000;

            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(100);

            InetAddress address = InetAddress.getByName(host);

            long start = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                byte[] data = ("PING-" + i).getBytes();
                socket.send(new DatagramPacket(data, data.length, address, port));
            }

            int ok = 0;
            long receiveUntil = System.currentTimeMillis() + 5000;

            while (System.currentTimeMillis() < receiveUntil) {
                try {
                    DatagramPacket response = new DatagramPacket(new byte[2048], 2048);
                    socket.receive(response);
                    ok++;
                } catch (SocketTimeoutException ignored) {
                }
            }

            long time = System.currentTimeMillis() - start;
            int lost = count - ok;

            System.out.println("TEST PING: sent=" + count + ", ok=" + ok + ", lost="
                    + lost + ", timeMs=" + time + ", sendRps=" + (count * 1000L / Math.max(1, time)) + ", lossPercent=" + (lost * 100.0 / count));

            socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
