package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

// Запускает VPN клиент после старта Spring Boot приложения.
@Service
@RequiredArgsConstructor
public class Client {

    // Основной класс VPN клиента.
    private final VpnClientRunner vpnClientRunner;

    // Spring вызывает этот метод, когда приложение полностью запустилось.
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        // Запускаем VPN клиент.
        vpnClientRunner.start();
    }
}
