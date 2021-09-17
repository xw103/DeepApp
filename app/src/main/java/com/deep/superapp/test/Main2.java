package com.deep.superapp.test;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.deep.superapp.ITest1;
import com.deep.superapp.ITest2;

import io.sapp.core.internal.IPCMain;
import io.sapp.core.internal.RootIPC;

public class Main2 extends IPCMain {

    @Override
    public void main(String sourcePath, String[] args) {
        for (int i = 0; i < args.length; i++) {
            Log.e(String.format("(%s):args[%d]",sourcePath, i), args[i]);
        }
    }

    @Override
    public IBinder onBind() {
        return new ITest2.Stub() {
            @Override
            public String getTest() throws RemoteException {
                return "第二个ROOT进程";
            }
        };
    }
}
