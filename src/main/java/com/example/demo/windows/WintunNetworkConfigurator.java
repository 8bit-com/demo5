package com.example.demo.windows;

import java.util.List;

public class WintunNetworkConfigurator {

    private static final String ADAPTER_NAME = "Demo5Vpn";
    private static final String CLIENT_IP = "10.8.0.2";
    private static final String MASK = "255.255.255.0";
    private static final String TEST_ROUTE = "10.8.0.1";

    private final WindowsCommand windowsCommand = new WindowsCommand();

    public void configure() {
        setAddress();
        addTestRoute();
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

    private void addTestRoute() {
        windowsCommand.run(List.of(
                "route",
                "add",
                TEST_ROUTE,
                "mask",
                "255.255.255.255",
                CLIENT_IP
        ));
    }
}
