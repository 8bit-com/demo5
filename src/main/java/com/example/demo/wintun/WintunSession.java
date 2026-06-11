package com.example.demo.wintun;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

// Обёртка над одной Wintun сессией.
public class WintunSession {

    // Указатель на открытую Wintun сессию.
    private Pointer session;

    // Создаём объект вокруг уже открытой Wintun сессии.
    public WintunSession(Pointer session) {
        // Сохраняем указатель на Wintun сессию.
        this.session = session;
    }

    // Читаем один IP пакет из Wintun адаптера.
    public byte[] readPacket() {
        // Если сессия уже закрыта, читать нечего.
        if (session == null) {
            return null;
        }

        // Создаём переменную, куда Wintun запишет размер пакета.
        IntByReference packetSize = new IntByReference();

        // Просим Wintun дать следующий пакет от Windows.
        Pointer packetPointer = Wintun.INSTANCE.WintunReceivePacket(session, packetSize);

        // Если пакета сейчас нет, возвращаем null.
        if (packetPointer == null) {
            return null;
        }

        try {
            // Копируем пакет из native памяти Wintun в обычный Java byte[].
            return packetPointer.getByteArray(0, packetSize.getValue());
        } finally {
            // Освобождаем native буфер, который Wintun дал для чтения.
            Wintun.INSTANCE.WintunReleaseReceivePacket(session, packetPointer);
        }
    }

    // Записываем один IP пакет обратно в Wintun адаптер.
    public boolean writePacket(byte[] packet) {
        // Если сессия уже закрыта, писать некуда.
        if (session == null) {
            return false;
        }

        // Просим Wintun выделить native буфер под пакет.
        Pointer sendPointer = Wintun.INSTANCE.WintunAllocateSendPacket(session, packet.length);

        // Если буфер выделить не удалось, сообщаем об ошибке.
        if (sendPointer == null) {
            System.out.println("Wintun send packet allocation failed");
            return false;
        }

        // Копируем Java byte[] в native буфер Wintun.
        sendPointer.write(0, packet, 0, packet.length);

        // Отдаём пакет в Windows через Wintun адаптер.
        Wintun.INSTANCE.WintunSendPacket(session, sendPointer);

        // Сообщаем вызывающему коду, что запись выполнена.
        return true;
    }

    // Закрываем Wintun сессию.
    public void close() {
        // Если сессия уже закрыта, ничего не делаем.
        if (session == null) {
            return;
        }

        // Закрываем Wintun сессию.
        Wintun.INSTANCE.WintunEndSession(session);

        // Сбрасываем указатель, чтобы не закрыть сессию второй раз.
        session = null;
    }
}
