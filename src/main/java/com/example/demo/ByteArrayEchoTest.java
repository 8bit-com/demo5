package com.example.demo;

import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class ByteArrayEchoTest {

    private static final String HOST = "80.240.23.72";
    private static final int PORT = 51888;
    private static final int TIMEOUT_MS = 5000;
    private static final int BUFFER_SIZE = 2048;

    private final SecureRandom random = new SecureRandom();

    public void start() {
        testSize(64);
        testSize(256);
        testSize(512);
        testSize(1024);
        testSize(1200);
        testSize(1300);
        testSize(1400);
    }

    private void testSize(int size) {
        try {
            byte[] sentData = randomBytes(size);
            byte[] receivedData = sendAndReceive(sentData);

            System.out.println(
                    "BYTE TEST: size=" + size +
                            ", received=" + receivedData.length +
                            ", equals=" + Arrays.equals(sentData, receivedData)
            );
        } catch (SocketTimeoutException e) {
            System.out.println("BYTE TEST: size=" + size + ", timeout=true");
        } catch (Exception e) {
            System.out.println("BYTE TEST: size=" + size + ", error=" + e.getMessage());
        }
    }

    private byte[] randomBytes(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    private byte[] sendAndReceive(byte[] data) throws Exception {
        InetAddress address = InetAddress.getByName(HOST);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            socket.send(new DatagramPacket(
                    data,
                    data.length,
                    address,
                    PORT
            ));

            DatagramPacket response = new DatagramPacket(
                    new byte[BUFFER_SIZE],
                    BUFFER_SIZE
            );

            socket.receive(response);

            return Arrays.copyOfRange(
                    response.getData(),
                    response.getOffset(),
                    response.getOffset() + response.getLength()
            );
        }
    }
}
