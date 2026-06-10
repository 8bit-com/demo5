package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Client {

    private static final boolean START_WINTUN = false;

    private final TestPing testPing;
    private final ByteArrayEchoTest byteArrayEchoTest;
    private final WintunAdapterTest wintunAdapterTest;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        testPing.start();
        byteArrayEchoTest.start();

        if (START_WINTUN) {
            wintunAdapterTest.start();
        }
    }
}
