/* Copyright 2018 Jorrit 'Chainfire' Jongma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sapp.core.internal;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess", "BooleanMethodIsAlwaysInverted", "FieldCanBeLocal", "Convert2Diamond"})
public class RootIPC {
    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }

    private final String packageName;
    private final IBinder userIPC;
    private final int code;

    private final Object helloWaiter = new Object();
    private final Object byeWaiter = new Object();

    private class Connection {
        private final IBinder binder;
        private final IBinder.DeathRecipient deathRecipient;

        public Connection(IBinder binder, IBinder.DeathRecipient deathRecipient) {
            this.binder = binder;
            this.deathRecipient = deathRecipient;
        }

        public IBinder getBinder() {
            return binder;
        }

        public IBinder.DeathRecipient getDeathRecipient() {
            return deathRecipient;
        }
    }

    private final List<Connection> connections = new ArrayList<Connection>();
    private volatile boolean connectionSeen = false;

    public RootIPC(String packageName, IBinder ipc, int code, int connection_timeout_ms, boolean blocking) throws TimeoutException {
        this.packageName = packageName;
        userIPC = ipc;
        this.code = code;
        broadcastIPC();

        if (connection_timeout_ms < 0) connection_timeout_ms = 30 * 1000;
        if (connection_timeout_ms > 0) {
            synchronized (helloWaiter) {
                if (!haveClientsConnected()) {
                    try {
                        helloWaiter.wait(connection_timeout_ms);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (!haveClientsConnected()) {
                    throw new TimeoutException("连接超时");
                }
            }
        }

        if (blocking) {
            synchronized (byeWaiter) {
                while (!haveAllClientsDisconnected()) {
                    try {
                        byeWaiter.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }

    //判断是否连接
    public boolean haveClientsConnected() {
        synchronized (connections) {
            return connectionSeen;
        }
    }

    //判断连接是否关闭
    public boolean haveAllClientsDisconnected() {
        synchronized (connections) {
            return connectionSeen && (getConnectionCount() == 0);
        }
    }

    public void broadcastIPC() {
        Intent intent = new Intent();
        intent.setPackage(packageName);
        intent.setAction(RootIPCReceiver.BROADCAST_ACTION);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        Bundle bundle = new Bundle();
        bundle.putBinder(RootIPCReceiver.BROADCAST_BINDER, binder);
        bundle.putInt(RootIPCReceiver.BROADCAST_CODE, code);
        intent.putExtra(RootIPCReceiver.BROADCAST_EXTRA, bundle);

        Reflection.sendBroadcast(intent);
    }

    //获取客户端连接数量
    public int getConnectionCount() {
        synchronized (connections) {
            pruneConnections();
            return connections.size();
        }
    }

    //删除无效连接
    private void pruneConnections() {
        synchronized (connections) {
            if (connections.size() == 0) return;

            for (int i = connections.size() - 1; i >= 0; i--) {
                Connection conn = connections.get(i);
                if (!conn.getBinder().isBinderAlive()) {
                    connections.remove(i);
                }
            }

            if (!connectionSeen && (connections.size() > 0)) {
                connectionSeen = true;
                synchronized (helloWaiter) {
                    helloWaiter.notifyAll();
                }
            }

            if (connections.size() == 0) {
                synchronized (byeWaiter) {
                    byeWaiter.notifyAll();
                }
            }
        }
    }

    //获取基于IBinder的连接
    private Connection getConnection(IBinder binder) {
        synchronized (connections) {
            pruneConnections();
            for (Connection conn : connections) {
                if (conn.getBinder() == binder) {
                    return conn;
                }
            }
            return null;
        }
    }

    //获取基于DeathRecipient的连接
    private Connection getConnection(IBinder.DeathRecipient deathRecipient) {
        synchronized (connections) {
            pruneConnections();
            for (Connection conn : connections) {
                if (conn.getDeathRecipient() == deathRecipient) {
                    return conn;
                }
            }
            return null;
        }
    }

    private final IBinder binder = new IRootIPC.Stub() {
        @Override
        public void addBinder(IBinder self) {//主进程连接
            // 主进程死亡
            IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    Connection conn = getConnection(this);
                    if (conn != null) {
                        removeBinder(conn.getBinder());
                    }
                }
            };
            try {
                self.linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                self = null;
            }

            //记录连接
            if (self != null) {
                synchronized (connections) {
                    connections.add(new Connection(self, deathRecipient));
                    connectionSeen = true;
                }
                synchronized (helloWaiter) {
                    helloWaiter.notifyAll();
                }
            }
        }

        @Override
        public IBinder getIPC() {
            return userIPC;
        }

        @Override
        public void removeBinder(IBinder self) {
            synchronized (connections) {
                Connection conn = getConnection(self);
                if (conn != null) {
                    try {
                        conn.getBinder().unlinkToDeath(conn.getDeathRecipient(), 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    connections.remove(conn);
                }
            }
            synchronized (byeWaiter) {
                byeWaiter.notifyAll();
            }
        }
    };
}
