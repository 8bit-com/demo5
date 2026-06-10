package com.example.demo;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TestPing {
    public void start() {
        try {
            String host = "80.240.23.72";
            int port = 51888;

            int socketsCount = 40;
            int targetPps = 5000;
            int maxTestMs = 5000;

            InetAddress address = InetAddress.getByName(host);

            AtomicInteger sent = new AtomicInteger();
            AtomicInteger ok = new AtomicInteger();
            AtomicBoolean running = new AtomicBoolean(true);

            DatagramSocket[] sockets = new DatagramSocket[socketsCount];

            for (int i = 0; i < socketsCount; i++) {
                sockets[i] = new DatagramSocket();
                sockets[i].setSoTimeout(100);
                sockets[i].setSendBufferSize(1024 * 1024);
                sockets[i].setReceiveBufferSize(1024 * 1024);
            }

            long startMs = System.currentTimeMillis();

            Thread[] receivers = new Thread[socketsCount];

            for (int i = 0; i < socketsCount; i++) {
                DatagramSocket socket = sockets[i];

                receivers[i] = new Thread(() -> {
                    byte[] buf = new byte[2048];

                    while (running.get()) {
                        try {
                            DatagramPacket p = new DatagramPacket(buf, buf.length);
                            socket.receive(p);
                            ok.incrementAndGet();
                        } catch (SocketTimeoutException ignored) {
                        } catch (Exception ignored) {
                            return;
                        }
                    }
                });

                receivers[i].start();
            }

            Thread sender = new Thread(() -> {
                int i = 0;

                long intervalNs = 1_000_000_000L / targetPps;
                long nextSendNs = System.nanoTime();

                while (System.currentTimeMillis() - startMs < maxTestMs) {
                    try {
                        DatagramSocket socket = sockets[i % socketsCount];

                        byte[] data = ("PING-" + i).getBytes();

                        socket.send(new DatagramPacket(
                                data,
                                data.length,
                                address,
                                port
                        ));

                        sent.incrementAndGet();
                        i++;

                        nextSendNs += intervalNs;

                        while (System.nanoTime() < nextSendNs) {
                            Thread.onSpinWait();
                        }

                    } catch (Exception ignored) {
                    }
                }
            });

            sender.start();
            sender.join();

            Thread.sleep(1000);
            running.set(false);

            for (DatagramSocket socket : sockets) {
                socket.close();
            }

            for (Thread receiver : receivers) {
                receiver.join();
            }

            long timeMs = System.currentTimeMillis() - startMs;

            int s = sent.get();
            int r = ok.get();
            int lost = s - r;

            System.out.println("TEST PING: " +  "sockets=" + socketsCount + "targetPps=" + targetPps
                    + "sent=" + s + "ok=" + r + "lost=" + lost + "timeMs=" + timeMs
                    + "sendRps=" + (s * 1000L / Math.max(1, timeMs)) + "okRps=" + (r * 1000L / Math.max(1, timeMs))
                    + "lossPercent=" + (lost * 100.0 / Math.max(1, s)));

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
