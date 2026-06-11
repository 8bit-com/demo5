package com.example.demo;

import com.example.demo.windows.WintunNetworkConfigurator;
import com.example.demo.wintun.Wintun;
import com.example.demo.wintun.WintunTcpForwarder;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import org.springframework.stereotype.Service;

// Главный класс VPN клиента: создаёт Wintun адаптер, настраивает Windows сеть и запускает пересылку пакетов.
@Service
public class VpnClientRunner {

    // Имя Wintun адаптера в Windows.
    private static final String ADAPTER_NAME = "Demo5Vpn";

    // Тип Wintun адаптера.
    private static final String TUNNEL_TYPE = "Demo5";

    // Настраивает IP адрес и маршрут Windows для Wintun адаптера.
    private final WintunNetworkConfigurator networkConfigurator = new WintunNetworkConfigurator();

    // Указатель на Wintun адаптер.
    private Pointer adapter;

    // Класс, который пересылает пакеты между Wintun и TCP сервером.
    private WintunTcpForwarder tcpForwarder;

    // Запускает VPN клиент.
    public void start() {
        // Создаём Wintun адаптер.
        adapter = createAdapter();

        // Настраиваем IP адрес и маршрут в Windows.
        networkConfigurator.configure();

        // Запускаем пересылку пакетов между Wintun и TCP сервером.
        startForwarder();

        // Добавляем очистку ресурсов при остановке приложения.
        addShutdownCleanup();
    }

    // Создаёт Wintun адаптер.
    private Pointer createAdapter() {
        // Вызываем native метод WintunCreateAdapter.
        Pointer createdAdapter = Wintun.INSTANCE.WintunCreateAdapter(
                new WString(ADAPTER_NAME),
                new WString(TUNNEL_TYPE),
                Pointer.NULL
        );

        // Если адаптер не создан, дальше запускать VPN нельзя.
        if (createdAdapter == null) {
            throw new IllegalStateException("Wintun adapter was not created");
        }

        // Пишем понятный лог создания адаптера.
        System.out.println("Wintun adapter created: " + ADAPTER_NAME);

        // Возвращаем созданный адаптер.
        return createdAdapter;
    }

    // Запускает пересылку пакетов в отдельном потоке.
    private void startForwarder() {
        // Создаём пересылку пакетов для текущего Wintun адаптера.
        tcpForwarder = new WintunTcpForwarder(adapter);

        // Создаём поток, чтобы не блокировать Spring Boot main thread.
        Thread forwarderThread = new Thread(tcpForwarder::start, "wintun-tcp-forwarder");

        // Запускаем поток пересылки.
        forwarderThread.start();
    }

    // Добавляет shutdown hook для корректного освобождения ресурсов.
    private void addShutdownCleanup() {
        // При остановке JVM вызываем cleanup().
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup, "wintun-cleanup"));
    }

    // Останавливает пересылку, удаляет маршрут и закрывает Wintun адаптер.
    private void cleanup() {
        // Пишем лог начала очистки.
        System.out.println("Wintun cleanup started");

        // Останавливаем пересылку пакетов.
        if (tcpForwarder != null) {
            tcpForwarder.stop();
        }

        // Удаляем маршрут Windows, который добавляли при запуске.
        networkConfigurator.cleanup();

        // Закрываем Wintun адаптер.
        if (adapter != null) {
            Wintun.INSTANCE.WintunCloseAdapter(adapter);
            adapter = null;
        }

        // Пишем лог завершения очистки.
        System.out.println("Wintun cleanup finished");
    }
}
