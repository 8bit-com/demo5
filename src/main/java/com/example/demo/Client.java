package com.example.demo;

import com.example.demo.tcp.TcpByteArrayEchoTest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Client {

    private final TcpByteArrayEchoTest tcpByteArrayEchoTest;
    private final WintunAdapterTest wintunAdapterTest;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        tcpByteArrayEchoTest.start();
        wintunAdapterTest.start();
    }
}
