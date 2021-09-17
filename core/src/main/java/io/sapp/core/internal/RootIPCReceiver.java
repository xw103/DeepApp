package io.sapp.core.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@SuppressWarnings({"unused", "WeakerAccess", "Convert2Diamond", "TryWithIdenticalCatches"})
public abstract class RootIPCReceiver<T> {

    public abstract void onConnect(T ipc);

    public abstract void onDisconnect(T ipc);

    public static final String BROADCAST_ACTION = "RootIPCReceiver.BROADCAST";
    public static final String BROADCAST_EXTRA = "RootIPCReceiver.BROADCAST.EXTRA";
    public static final String BROADCAST_BINDER = "binder";
    public static final String BROADCAST_CODE = "code";

    private final HandlerThread handlerThread;
    private final Handler handler;

    private final int code;
    private final Class<T> clazz;
    private final IBinder self = new Binder();
    private final Object binderSync = new Object();
    private final Object eventSync = new Object();

    private volatile WeakReference<Context> context;
    private volatile IBinder binder = null;
    private volatile IRootIPC ipc = null;
    private volatile T userIPC = null;
    private volatile boolean inEvent = false;
    private volatile boolean disconnectAfterEvent = false;

    private final IntentFilter filter = new IntentFilter(BROADCAST_ACTION);

    private final IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            synchronized (binderSync) {
                clearBinder();
                binderSync.notifyAll();
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            IBinder received = null;

            if ((intent.getAction() != null) && intent.getAction().equals(BROADCAST_ACTION)) {
                Bundle bundle = intent.getBundleExtra(BROADCAST_EXTRA);
                received = bundle.getBinder(BROADCAST_BINDER);
                int code = bundle.getInt(BROADCAST_CODE);
                if ((code == RootIPCReceiver.this.code) && (received != null)) {
                    try {
                        received.linkToDeath(deathRecipient, 0);
                    } catch (RemoteException e) {
                        received = null;
                    }
                } else {
                    received = null;
                }
            }

            if (received != null) {
                synchronized (binderSync) {
                    binder = received;
                    ipc = IRootIPC.Stub.asInterface(binder);
                    try {
                        userIPC = getInterfaceFromBinder(ipc.getIPC());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    try {
                        ipc.addBinder(self);
                        handler.post(onConnectRunnable);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    binderSync.notifyAll();
                }
            }
        }
    };

    private final Runnable onConnectRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (binderSync) {
                doOnConnect();
            }
        }
    };

    public RootIPCReceiver(Context context, int code) {
        this(context, code, null);
    }

    @SuppressWarnings("unchecked")
    public RootIPCReceiver(Context context, int code, Class<T> clazz) {
        if (clazz == null) {
            Type superClass = getClass().getGenericSuperclass();
            Type tType = ((ParameterizedType)superClass).getActualTypeArguments()[0];
            this.clazz = (Class<T>)tType;
        } else {
            this.clazz = clazz;
        }
        this.code = code;
        handlerThread = new HandlerThread("DepthExploit:RootIPCReceiver#" + String.valueOf(code));
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        setContext(context);
    }

    public void setContext(Context context) {
        if (this.context != null) {
            Context oldContext = this.context.get();
            if (oldContext != null) {
                oldContext.unregisterReceiver(receiver);
            }
        }
        this.context = null;
        if (context != null) {
            if (context instanceof ContextWrapper) {
                if (((ContextWrapper)context).getBaseContext() == null) return;
            }
            this.context = new WeakReference<Context>(context);
            context.registerReceiver(receiver, filter, null, handler);
        }
    }

    private T getInterfaceFromBinder(IBinder binder) {
        return (new Reflection.InterfaceRetriever<T>()).getInterfaceFromBinder(clazz, binder);
    }

    private void doOnConnect() {
        //只能在synchronized(binderSync)内调用
        if ((binder != null) && (userIPC != null)) {
            synchronized (eventSync) {
                disconnectAfterEvent = false;
                inEvent = true;
            }
            onConnect(userIPC);
            synchronized (eventSync) {
                inEvent = false;
                if (disconnectAfterEvent) {
                    disconnect();
                }
            }
        }
    }

    private void doOnDisconnect() {
        //只能在synchronized(binderSync)内调用
        if ((binder != null) && (userIPC != null)) {
            onDisconnect(userIPC);
        }
    }

    private void clearBinder() {
        doOnDisconnect();
        if (binder != null) {
            try {
                binder.unlinkToDeath(deathRecipient, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        binder = null;
        ipc = null;
        userIPC = null;
    }

    private boolean isInEvent() {
        synchronized (eventSync) {
            return inEvent;
        }
    }

    public boolean isConnected() {
        return (getIPC() != null);
    }

    public boolean isDisconnectScheduled() {
        synchronized (eventSync) {
            if (disconnectAfterEvent) {
                return true;
            }
        }
        return false;
    }

    public void disconnect() {
        synchronized (eventSync) {
            if (inEvent) {
                disconnectAfterEvent = true;
                return;
            }
        }

        synchronized (binderSync) {
            if (ipc != null) {
                try {
                    ipc.removeBinder(self);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            clearBinder();
        }
    }

    //释放所有资源
    public void release() {
        disconnect();
        if (this.context != null) {
            Context context = this.context.get();
            if (context != null) {
                context.unregisterReceiver(receiver);
            }
        }
        handlerThread.quitSafely();
    }

    public T getIPC() {
        if (isDisconnectScheduled()) return null;
        if (isInEvent()) {
            return userIPC;
        }

        synchronized (binderSync) {
            if (binder != null) {
                if (!binder.isBinderAlive()) {
                    clearBinder();
                }
            }
            if ((binder != null) && (userIPC != null)) {
                return userIPC;
            }
        }
        return null;
    }


    public T getIPC(int timeout_ms) {
        if (isDisconnectScheduled()) return null;
        if (isInEvent()) {
            return userIPC;
        }

        if (timeout_ms <= 0) return getIPC();

        synchronized (binderSync) {
            if (binder == null) {
                try {
                    binderSync.wait(timeout_ms);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return getIPC();
    }
}
