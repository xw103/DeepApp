package io.sapp.core.internal;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class IPCMain {

    private static String nativeLibraryDir;

    public abstract void main(String sourcePath,String[] args);

    public abstract IBinder onBind();

    public String getNativeLibraryDir(){
        return nativeLibraryDir;
    }

    public static void main(String[] args){
        String source = args[0];
        String packageName = args[1];
        String nativeLibraryDir = args[2];
        String className = args[3];
        int code = Integer.parseInt(args[4]);
        IPCMain.nativeLibraryDir = nativeLibraryDir;
        List<String> paramList = new ArrayList<String>();
        for(int i = 5;i<args.length;i++){
            paramList.add(args[i]);
        }
        try {
            Class<IPCMain> clz = (Class<IPCMain>) Class.forName(className);
            IPCMain main = clz.newInstance();
            main.main(source,paramList.toArray(new String[0]));
            new RootIPC(packageName,main.onBind() ,code,30*1000,true);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (RootIPC.TimeoutException e) {
            e.printStackTrace();
        }
    }
}
