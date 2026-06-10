package com.example.demo;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Client {
    private final TestPing testPing;
    private final ByteArrayEchoTest byteArrayEchoTest;
    private final WintunAdapterTest wintunAdapterTest;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        testPing.start();
        byteArrayEchoTest.start();
        wintunAdapterTest.start();
    }
}
