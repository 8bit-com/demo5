package com.example.demo.windows;

import java.util.Arrays;
import java.util.List;

// Настраивает Windows сеть для Wintun адаптера.
public class WintunNetworkConfigurator {

    // Имя Wintun адаптера в Windows.
    private static final String ADAPTER_NAME = "Demo5Vpn";

    // IP адрес клиента внутри VPN сети.
    private static final String CLIENT_IP = "10.8.0.2";

    // Маска VPN сети.
    private static final String MASK = "255.255.255.0";

    // IP сервера внутри VPN сети.
    private static final String SERVER_VPN_IP = "10.8.0.1";

    // Запускает Windows команды.
    private final WindowsCommand windowsCommand = new WindowsCommand();

    // Настраивает адрес и маршрут Wintun адаптера.
    public void configure() {
        // Удаляем старый маршрут, если он остался после прошлого запуска.
        deleteServerRoute();

        // Назначаем Wintun адаптеру IP адрес. Логику не меняем: это было и раньше.
        setAddress();

        // Находим Windows index Wintun интерфейса.
        int interfaceIndex = findInterfaceIndex();

        // Добавляем маршрут до 10.8.0.1 через Wintun. Логику маршрута не меняем.
        addServerRoute(interfaceIndex);
    }

    // Удаляет маршрут при остановке клиента.
    public void cleanup() {
        // Удаляем маршрут до VPN IP сервера.
        deleteServerRoute();
    }

    // Удаляет маршрут до VPN IP сервера.
    private void deleteServerRoute() {
        // route delete 10.8.0.1
        windowsCommand.runIgnoreError(List.of(
                "route",
                "delete",
                SERVER_VPN_IP
        ));
    }

    // Назначает IP адрес Wintun адаптеру.
    private void setAddress() {
        // netsh interface ip set address name=Demo5Vpn static 10.8.0.2 255.255.255.0
        windowsCommand.run(List.of(
                "netsh",
                "interface",
                "ip",
                "set",
                "address",
                "name=" + ADAPTER_NAME,
                "static",
                CLIENT_IP,
                MASK
        ));
    }

    // Ищет Windows interface index по имени адаптера.
    private int findInterfaceIndex() {
        // netsh interface ipv4 show interfaces
        String output = windowsCommand.run(List.of(
                "netsh",
                "interface",
                "ipv4",
                "show",
                "interfaces"
        ));

        // Ищем строку, которая заканчивается именем Demo5Vpn.
        return Arrays.stream(output.split("\\R"))
                .map(String::trim)
                .filter(line -> line.endsWith(ADAPTER_NAME))
                .map(this::parseInterfaceIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Interface not found: " + ADAPTER_NAME));
    }

    // Достаёт interface index из первой колонки строки netsh.
    private int parseInterfaceIndex(String line) {
        // Разбиваем строку netsh по пробелам.
        String[] parts = line.split("\\s+");

        // Первая колонка — это interface index.
        return Integer.parseInt(parts[0]);
    }

    // Добавляет маршрут до VPN IP сервера через Wintun интерфейс.
    private void addServerRoute(int interfaceIndex) {
        // route add 10.8.0.1 mask 255.255.255.255 0.0.0.0 if <index>
        windowsCommand.run(List.of(
                "route",
                "add",
                SERVER_VPN_IP,
                "mask",
                "255.255.255.255",
                "0.0.0.0",
                "if",
                String.valueOf(interfaceIndex)
        ));
    }
}
