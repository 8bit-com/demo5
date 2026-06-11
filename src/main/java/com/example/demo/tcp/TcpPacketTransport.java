package com.example.demo.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

// TCP транспорт для передачи сырых IP пакетов между клиентом и сервером.
public class TcpPacketTransport {

    // IP адрес VPS сервера.
    private static final String HOST = "80.240.23.72";

    // TCP порт сервера.
    private static final int PORT = 51890;

    // Максимальный размер IP пакета, который разрешаем отправлять.
    private static final int MAX_PACKET_SIZE = 1200;

    // Тип TCP кадра: VPN пакет.
    private static final int FRAME_VPN = 2;

    // TCP сокет до сервера.
    private Socket socket;

    // Поток чтения из TCP сокета.
    private DataInputStream input;

    // Поток записи в TCP сокет.
    private DataOutputStream output;

    // Флаг, что транспорт сейчас подключен.
    private volatile boolean connected;

    // Подключаемся к серверу.
    public synchronized void connect() {
        // Если уже подключены, второй раз не подключаемся.
        if (isConnected()) {
            return;
        }

        try {
            // Закрываем старый сокет, если он остался после ошибки.
            close();

            // Открываем TCP соединение до VPS сервера.
            socket = new Socket(HOST, PORT);

            // Отключаем задержку Nagle, чтобы маленькие пакеты не ждали накопления.
            socket.setTcpNoDelay(true);

            // Создаём поток чтения ответов от сервера.
            input = new DataInputStream(socket.getInputStream());

            // Создаём поток записи пакетов на сервер.
            output = new DataOutputStream(socket.getOutputStream());

            // Помечаем транспорт как подключенный.
            connected = true;

            // Пишем понятный лог подключения.
            System.out.println("TCP VPN connected to " + HOST + ":" + PORT);
        } catch (Exception e) {
            // Закрываем всё, если подключение не удалось.
            close();

            // Пробрасываем ошибку наружу, чтобы вызывающий код мог попробовать позже.
            throw new RuntimeException(e);
        }
    }

    // Проверяет, есть ли сейчас живое TCP соединение.
    public boolean isConnected() {
        // Возвращаем true только если флаг стоит и сокет открыт.
        return connected && socket != null && !socket.isClosed();
    }

    // Отправляем один IP пакет на сервер.
    public synchronized void sendPacket(byte[] packet) {
        // Если транспорт не подключен, пакет отправлять нельзя.
        if (!isConnected()) {
            return;
        }

        // Если пакет слишком большой, не шлём его в TCP.
        if (packet.length > MAX_PACKET_SIZE) {
            System.out.println("TCP packet skipped, size=" + packet.length);
            return;
        }

        try {
            // Пишем тип кадра: VPN пакет.
            output.writeByte(FRAME_VPN);

            // Пишем размер IP пакета.
            output.writeInt(packet.length);

            // Пишем сам IP пакет.
            output.write(packet);

            // Принудительно отправляем данные в TCP.
            output.flush();
        } catch (Exception e) {
            // При ошибке записи закрываем только TCP, Wintun этим не трогаем.
            System.out.println("TCP packet send error: " + errorText(e));
            close();
        }
    }

    // Читаем один IP пакет от сервера.
    public byte[] readPacket() {
        // Если транспорт не подключен, читать нечего.
        if (!isConnected()) {
            return null;
        }

        try {
            // Читаем тип кадра от сервера.
            int frameType = input.readUnsignedByte();

            // Читаем размер IP пакета.
            int size = input.readInt();

            // Если сервер прислал не VPN кадр, закрываем TCP соединение.
            if (frameType != FRAME_VPN) {
                System.out.println("TCP VPN wrong frame type: " + frameType);
                close();
                return null;
            }

            // Если размер неправильный, закрываем TCP соединение.
            if (size <= 0 || size > MAX_PACKET_SIZE) {
                close();
                return null;
            }

            // Читаем IP пакет указанного размера.
            byte[] packet = input.readNBytes(size);

            // Если пакет прочитан не полностью, закрываем TCP соединение.
            if (packet.length != size) {
                close();
                return null;
            }

            // Возвращаем пакет вызывающему коду.
            return packet;
        } catch (Exception e) {
            // При ошибке чтения закрываем только TCP, Wintun этим не трогаем.
            if (connected) {
                System.out.println("TCP packet read error: " + errorText(e));
            }
            close();
            return null;
        }
    }

    // Закрываем TCP транспорт.
    public synchronized void close() {
        // Помечаем транспорт как отключенный.
        connected = false;

        try {
            // Закрываем TCP сокет, если он был открыт.
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
            // Ошибка закрытия не важна.
        }

        // Обнуляем ссылки на ресурсы.
        socket = null;
        input = null;
        output = null;
    }

    // Делает текст ошибки понятным даже если getMessage() вернул null.
    private String errorText(Exception e) {
        // Возвращаем имя класса ошибки и её сообщение.
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
