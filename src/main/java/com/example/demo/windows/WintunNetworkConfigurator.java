package com.example.demo.windows;

import java.util.Arrays;
import java.util.List;

public class WintunNetworkConfigurator {

    private static final String ADAPTER_NAME = "Demo5Vpn";
    private static final String CLIENT_IP = "10.8.0.2";
    private static final String MASK = "255.255.255.0";
    private static final String TEST_ROUTE = "10.8.0.1";
    private static final String SERVER_IP = "80.240.23.72";
    private static final String HOME_GATEWAY = "192.168.1.1";

    private final WindowsCommand windowsCommand = new WindowsCommand();

    public void configure() {
        deleteTestRoute();
        deleteServerRoute();
        addServerRoute();
        setAddress();
        int interfaceIndex = findInterfaceIndex();
        addTestRoute(interfaceIndex);
    }

    public void cleanup() {
        deleteTestRoute();
        deleteServerRoute();
    }

    private void deleteTestRoute() {
        windowsCommand.runIgnoreError(List.of(
                "route",
                "delete",
                TEST_ROUTE
        ));
    }

    private void deleteServerRoute() {
        windowsCommand.runIgnoreError(List.of(
                "route",
                "delete",
                SERVER_IP
        ));
    }

    private void addServerRoute() {
        windowsCommand.run(List.of(
                "route",
                "add",
                SERVER_IP,
                "mask",
                "255.255.255.255",
                HOME_GATEWAY,
                "metric",
                "1"
        ));
    }

    private void setAddress() {
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

    private int findInterfaceIndex() {
        String output = windowsCommand.run(List.of(
                "netsh",
                "interface",
                "ipv4",
                "show",
                "interfaces"
        ));

        return Arrays.stream(output.split("\\R"))
                .map(String::trim)
                .filter(line -> line.endsWith(ADAPTER_NAME))
                .map(this::parseInterfaceIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Interface not found: " + ADAPTER_NAME));
    }

    private int parseInterfaceIndex(String line) {
        String[] parts = line.split("\\s+");
        return Integer.parseInt(parts[0]);
    }

    private void addTestRoute(int interfaceIndex) {
        windowsCommand.run(List.of(
                "route",
                "add",
                TEST_ROUTE,
                "mask",
                "255.255.255.255",
                "0.0.0.0",
                "if",
                String.valueOf(interfaceIndex)
        ));
    }
}
