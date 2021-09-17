package io.sapp.core.internal;

// This is the wrapper used internally by RootIPC(Receiver)

interface IRootIPC {
    void addBinder(IBinder self);
    IBinder getIPC();
    void removeBinder(IBinder self);
}
