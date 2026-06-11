package com.example.demo.tcp;

import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class TcpByteArrayEchoTest {

    private static final String HOST = "80.240.23.72";
    private static final int PORT = 51890;
    private static final int TIMEOUT_MS = 5000;

    private final SecureRandom random = new SecureRandom();

    public void start() {
        int[] sizes = {64, 256, 512, 1024, 1200};

        try (Socket socket = new Socket(HOST, PORT)) {
            socket.setSoTimeout(TIMEOUT_MS);
            socket.setTcpNoDelay(true);

            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            for (int size : sizes) {
                testSize(size, input, output);
            }
        } catch (Exception e) {
            System.out.println("TCP BYTE TEST failed: " + e.getMessage());
        }
    }

    private void testSize(int size, DataInputStream input, DataOutputStream output) throws Exception {
        byte[] request = new byte[size];
        random.nextBytes(request);

        output.writeInt(request.length);
        output.write(request);
        output.flush();

        int responseSize = input.readInt();
        byte[] response = input.readNBytes(responseSize);

        System.out.println(
                "TCP BYTE TEST: size=" + size +
                        ", received=" + response.length +
                        ", equals=" + Arrays.equals(request, response)
        );
    }
}
