package com.deep.superapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.deep.superapp.test.Main1;
import com.deep.superapp.test.Main2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.sapp.core.internal.IPCManager;
import io.sapp.core.internal.IPCServer;
import io.sapp.core.internal.RootIPCReceiver;
import io.sapp.core.internal.RootServer;
import io.sapp.core.utils.Shell;

public class MainActivity extends AppCompatActivity {

//    private final RootIPCReceiver<ITest1> ipcReceiver1 = new RootIPCReceiver<ITest1>(this, 0) {
//        @Override
//        public void onConnect(ITest1 ipc) {
//            try {
//                Log.e("test", ipc.getTest());
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this, 6);
//                    builder1.setTitle("提示");
//                    try {
//                        builder1.setMessage(ipc.getTest());
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                    builder1.setCancelable(false);
//                    builder1.setPositiveButton("确定", (dialog1, which1) -> {
//                        dialog1.dismiss();
//                    });
//                    AlertDialog d = builder1.create();
//                    int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
//                    d.getWindow().setType(type);
//                    d.show();
//                });
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void onDisconnect(ITest1 ipc) {
//
//        }
//    };
//
//    private final RootIPCReceiver<ITest2> ipcReceiver2 = new RootIPCReceiver<ITest2>(this, 1) {
//        @Override
//        public void onConnect(ITest2 ipc) {
//            try {
//                Log.e("test2", ipc.getTest());
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this, 6);
//                    builder1.setTitle("提示");
//                    try {
//                        builder1.setMessage(ipc.getTest());
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                    builder1.setCancelable(false);
//                    builder1.setPositiveButton("确定", (dialog1, which1) -> {
//                        dialog1.dismiss();
//                    });
//                    AlertDialog d = builder1.create();
//                    int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
//                    d.getWindow().setType(type);
//                    d.show();
//                });
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void onDisconnect(ITest2 ipc) {
//
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.text1);
        tv.setText(getPackageCodePath());
        new IPCServer<ITest1>(this) {
            @Override
            public void onConnect(ITest1 ipc) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this, 6);
                    builder1.setTitle("提示");
                    try {
                        builder1.setMessage(ipc.getTest());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    builder1.setCancelable(false);
                    builder1.setPositiveButton("确定", (dialog1, which1) -> {
                        dialog1.dismiss();
                    });
                    AlertDialog d = builder1.create();
                    int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                    d.getWindow().setType(type);
                    d.show();
                });
            }

            @Override
            public void onDisconnect(ITest1 ipc) {

            }

            @Override
            public void onLine(String line) {
                Log.e("shell:[su]", line);
            }
        }.setMainClass(Main1.class)
                .setParam(new String[]{"测试", "666", "888", "999"})
                .start();

        new IPCServer<ITest2>(this) {
            @Override
            public void onConnect(ITest2 ipc) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this, 6);
                    builder1.setTitle("提示");
                    try {
                        builder1.setMessage(ipc.getTest());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    builder1.setCancelable(false);
                    builder1.setPositiveButton("确定", (dialog1, which1) -> {
                        dialog1.dismiss();
                    });
                    AlertDialog d = builder1.create();
                    int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                    d.getWindow().setType(type);
                    d.show();
                });
            }

            @Override
            public void onDisconnect(ITest2 ipc) {

            }

            @Override
            public void onLine(String line) {
                Log.e("shell:[su]", line);
            }
        }.setMainClass(Main2.class)
                .setParam(new String[]{"测试2", "666", "888", "999", "222"})
                .start();
    }


}