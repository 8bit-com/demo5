package com.example.demo.wintun;

import com.example.demo.tcp.TcpPacketTransport;
import com.sun.jna.Pointer;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

// Пересылает IP пакеты между Wintun адаптером и TCP соединением с сервером.
@RequiredArgsConstructor
public class WintunTcpForwarder {

    // Размер буфера Wintun сессии.
    private static final int SESSION_CAPACITY = 4 * 1024 * 1024;

    // Пауза, если Wintun сейчас не дал пакет.
    private static final int EMPTY_READ_SLEEP_MS = 10;

    // Номер IP протокола ICMP.
    private static final int ICMP_PROTOCOL = 1;

    // Тип ICMP: ping request.
    private static final int ICMP_ECHO_REQUEST = 8;

    // Тип ICMP: ping reply.
    private static final int ICMP_ECHO_REPLY = 0;

    // Указатель на Wintun адаптер.
    private final Pointer adapter;

    // Флаг работы пересылки.
    private final AtomicBoolean running = new AtomicBoolean(true);

    // TCP транспорт до VPS сервера.
    private final TcpPacketTransport tcpTransport = new TcpPacketTransport();

    // Обёртка над Wintun сессией.
    private WintunSession wintunSession;

    // Запускаем пересылку пакетов.
    public void start() {
        // Открываем Wintun сессию.
        Pointer rawSession = Wintun.INSTANCE.WintunStartSession(adapter, SESSION_CAPACITY);

        // Сохраняем Wintun сессию в читаемую Java обёртку.
        wintunSession = new WintunSession(rawSession);

        // Подключаем TCP транспорт к серверу.
        tcpTransport.connect();

        // Запускаем отдельный поток: сервер TCP -> Windows Wintun.
        Thread tcpToWintunThread = new Thread(this::copyTcpToWintun, "tcp-to-wintun");

        // Стартуем поток чтения ответов от сервера.
        tcpToWintunThread.start();

        // Пишем лог, что пересылка запущена.
        System.out.println("Wintun TCP forwarder started");

        try {
            // В текущем потоке гоняем пакеты Windows Wintun -> сервер TCP.
            copyWintunToTcp();
        } finally {
            // При выходе останавливаем ресурсы.
            stop();
        }
    }

    // Останавливаем пересылку и закрываем ресурсы.
    public void stop() {
        // Сбрасываем флаг работы.
        running.set(false);

        // Закрываем TCP соединение.
        tcpTransport.close();

        // Закрываем Wintun сессию, если она была открыта.
        if (wintunSession != null) {
            wintunSession.close();
            wintunSession = null;
        }
    }

    // Копируем пакеты из Windows через Wintun на сервер через TCP.
    private void copyWintunToTcp() {
        // Работаем, пока клиент не остановлен.
        while (running.get()) {
            // Читаем один IP пакет из Wintun.
            byte[] packetFromWindows = wintunSession.readPacket();

            // Если пакета сейчас нет, немного ждём и продолжаем.
            if (packetFromWindows == null) {
                sleepQuietly();
                continue;
            }

            // Если это ping request, печатаем его.
            printPing("CLIENT PING REQUEST", packetFromWindows, ICMP_ECHO_REQUEST);

            // Отправляем пакет на сервер.
            tcpTransport.sendPacket(packetFromWindows);
        }
    }

    // Копируем пакеты от сервера через TCP обратно в Windows через Wintun.
    private void copyTcpToWintun() {
        // Работаем, пока клиент не остановлен.
        while (running.get()) {
            // Читаем один IP пакет от сервера.
            byte[] packetFromServer = tcpTransport.readPacket();

            // Если пакет не прочитан, останавливаем пересылку.
            if (packetFromServer == null) {
                stop();
                return;
            }

            // Если это ping reply, печатаем его.
            printPing("CLIENT PING REPLY", packetFromServer, ICMP_ECHO_REPLY);

            // Записываем пакет в Wintun, то есть отдаём его Windows.
            wintunSession.writePacket(packetFromServer);
        }
    }

    // Спим без лишнего шума.
    private void sleepQuietly() {
        try {
            // Делаем короткую паузу.
            Thread.sleep(EMPTY_READ_SLEEP_MS);
        } catch (InterruptedException e) {
            // Восстанавливаем флаг прерывания потока.
            Thread.currentThread().interrupt();
        }
    }

    // Печатаем только нужный ICMP ping пакет.
    private void printPing(String prefix, byte[] packet, int expectedType) {
        // Если пакет меньше минимального IPv4+ICMP, это не ping.
        if (packet.length < 28) {
            return;
        }

        // Считаем длину IPv4 заголовка.
        int headerLength = (packet[0] & 0x0F) * 4;

        // Читаем номер IP протокола.
        int protocol = packet[9] & 0xFF;

        // Если это не ICMP или пакет слишком короткий, это не ping.
        if (protocol != ICMP_PROTOCOL || packet.length < headerLength + 8) {
            return;
        }

        // Читаем тип ICMP сообщения.
        int icmpType = packet[headerLength] & 0xFF;

        // Если тип ICMP не тот, который мы хотим печатать, выходим.
        if (icmpType != expectedType) {
            return;
        }

        // Читаем ICMP id.
        int icmpId = ((packet[headerLength + 4] & 0xFF) << 8) | (packet[headerLength + 5] & 0xFF);

        // Читаем ICMP sequence.
        int icmpSeq = ((packet[headerLength + 6] & 0xFF) << 8) | (packet[headerLength + 7] & 0xFF);

        // Печатаем ping пакет в понятном виде.
        System.out.println(prefix
                + ": src=" + ipToString(packet, 12)
                + ", dst=" + ipToString(packet, 16)
                + ", type=" + icmpType
                + ", id=" + icmpId
                + ", seq=" + icmpSeq
                + ", size=" + packet.length);
    }

    // Превращает 4 байта IP адреса в строку.
    private String ipToString(byte[] packet, int offset) {
        // Собираем IP адрес из четырёх байтов.
        return (packet[offset] & 0xFF) + "."
                + (packet[offset + 1] & 0xFF) + "."
                + (packet[offset + 2] & 0xFF) + "."
                + (packet[offset + 3] & 0xFF);
    }
}
