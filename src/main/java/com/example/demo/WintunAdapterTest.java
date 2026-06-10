package com.example.demo;

import com.example.demo.wintun.Wintun;
import com.example.demo.wintun.WintunPacketReader;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import org.springframework.stereotype.Service;

@Service
public class WintunAdapterTest {

    private static final String ADAPTER_NAME = "Demo5Vpn";
    private static final String TUNNEL_TYPE = "Demo5";

    public void start() {
        Pointer adapter = Wintun.INSTANCE.WintunCreateAdapter(
                new WString(ADAPTER_NAME),
                new WString(TUNNEL_TYPE),
                Pointer.NULL
        );

        if (adapter == null) {
            throw new IllegalStateException("Wintun adapter was not created");
        }

        System.out.println("Wintun adapter created: " + ADAPTER_NAME);

        Thread readerThread = new Thread(
                new WintunPacketReader(adapter)::readLoop,
                "wintun-reader"
        );

        readerThread.start();
    }
}
