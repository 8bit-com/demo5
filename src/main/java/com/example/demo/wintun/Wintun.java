package com.example.demo.wintun;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;

public interface Wintun extends Library {

    Wintun INSTANCE = Native.load("wintun", Wintun.class);

    Pointer WintunCreateAdapter(WString name, WString tunnelType, Pointer requestedGuid);

    void WintunCloseAdapter(Pointer adapter);

    Pointer WintunStartSession(Pointer adapter, int capacity);

    void WintunEndSession(Pointer session);

    Pointer WintunReceivePacket(Pointer session, IntByReference packetSize);

    void WintunReleaseReceivePacket(Pointer session, Pointer packet);

    Pointer WintunAllocateSendPacket(Pointer session, int packetSize);

    void WintunSendPacket(Pointer session, Pointer packet);
}
