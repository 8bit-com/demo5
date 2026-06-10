package com.example.demo.wintun;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

public interface Wintun extends Library {

    Wintun INSTANCE = Native.load("wintun", Wintun.class);

    HANDLE WintunCreateAdapter(WString name, WString tunnelType, Pointer requestedGuid);

    void WintunCloseAdapter(HANDLE adapter);

    HANDLE WintunStartSession(HANDLE adapter, DWORD capacity);

    void WintunEndSession(HANDLE session);

    Pointer WintunReceivePacket(HANDLE session, IntByReference packetSize);

    void WintunReleaseReceivePacket(HANDLE session, Pointer packet);
}
