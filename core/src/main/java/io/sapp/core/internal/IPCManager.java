package io.sapp.core.internal;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class IPCManager {
    private static Map<String,RootIPCReceiver> ipcs = new HashMap<String,RootIPCReceiver>();

    public static RootIPCReceiver getIPCReceiver(Class<?> clz){
        return ipcs.get(clz.getName());
    }

    private static void addIPCReceiver(Class<?> clz,RootIPCReceiver ipcReceiver){
        ipcs.put(clz.getName(),ipcReceiver);
    }
}
