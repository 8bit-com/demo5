package com.example.demo;

import com.example.demo.windows.WintunNetworkConfigurator;
import com.example.demo.wintun.Wintun;
import com.example.demo.wintun.WintunPacketReader;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import org.springframework.stereotype.Service;

@Service
public class WintunAdapterTest {

    private static final String ADAPTER_NAME = "Demo5Vpn";
    private static final String TUNNEL_TYPE = "Demo5";

    private final WintunNetworkConfigurator networkConfigurator = new WintunNetworkConfigurator();

    private Pointer adapter;
    private WintunPacketReader packetReader;

    public void start() {
        adapter = createAdapter();

        networkConfigurator.configure();
        startReader();
        addShutdownCleanup();
    }

    private Pointer createAdapter() {
        Pointer createdAdapter = Wintun.INSTANCE.WintunCreateAdapter(
                new WString(ADAPTER_NAME),
                new WString(TUNNEL_TYPE),
                Pointer.NULL
        );

        if (createdAdapter == null) {
            throw new IllegalStateException("Wintun adapter was not created");
        }

        System.out.println("Wintun adapter created: " + ADAPTER_NAME);
        return createdAdapter;
    }

    private void startReader() {
        packetReader = new WintunPacketReader(adapter);

        Thread readerThread = new Thread(
                packetReader::readLoop,
                "wintun-reader"
        );

        readerThread.start();
    }

    private void addShutdownCleanup() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup, "wintun-cleanup"));
    }

    private void cleanup() {
        System.out.println("Wintun cleanup started");

        if (packetReader != null) {
            packetReader.stop();
        }

        networkConfigurator.cleanup();

        if (adapter != null) {
            Wintun.INSTANCE.WintunCloseAdapter(adapter);
            adapter = null;
        }

        System.out.println("Wintun cleanup finished");
    }
}
