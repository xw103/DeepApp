package io.sapp.core.internal;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.sapp.core.utils.Shell;

public abstract class IPCServer<T> extends RootIPCReceiver<T> {

    private Context context;
    private static int code = 0;
    private String[] param = null;
    private Class<?> mainClass = null;
    private String apkPath = null;
    private String packageName = null;
    private String processName = null;

    public abstract void onLine(String line);

    public IPCServer(Context context) {
        super(context, code);
        this.context = context;
    }

    public IPCServer(Context context, Class clazz) {
        super(context, code, clazz);
        this.context = context;
    }

    public IPCServer setParam(String[] param) {
        this.param = param;
        return this;
    }

    public IPCServer setMainClass(Class<?> mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public IPCServer setApkPath(String apkPath) {
        this.apkPath = apkPath;
        return this;
    }

    public IPCServer setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public void createProcess(String packageCodePath, String packageName, Class<?> clazz, String[] param, String processName) {
        List<String> script = getLaunchScript(packageCodePath, packageName, clazz, param, processName);
        Shell.Interactive shell = (new Shell.Builder())
                .useSU()
                .open(new Shell.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        if (exitCode != SHELL_RUNNING) {
                            //执行中
                        }
                    }
                });

        // 异步运行Script
        shell.addCommand(script, code, new Shell.OnCommandLineListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode) {
                //执行完成
            }

            @Override
            public void onLine(String line) {
                // 接收输出
                IPCServer.this.onLine(line);
            }
        });
    }

    public void createProcess(String packageCodePath, String packageName, Class<?> clazz, String[] param) {
        createProcess(packageCodePath,packageName,clazz,param,"root");
    }

    public void createProcess(Class<?> clazz, String[] param) {
        createProcess(context.getPackageCodePath(), context.getPackageName(),clazz,param,"root");
    }

    public List<String> getLaunchScript(Class<?> mainClass, String[] params) {
        return getLaunchScript(context.getPackageCodePath(), context.getPackageName(), mainClass, params, "root");
    }

    public List<String> getLaunchScript(Class<?> mainClass, String[] params, String processName) {
        return getLaunchScript(context.getPackageCodePath(), context.getPackageName(), mainClass, params, processName);
    }

    public List<String> getLaunchScript(String packageCodePath, String packageName, Class<?> mainClass, String[] params, String processName) {
        List<String> paramList = new ArrayList<String>();
        paramList.add(packageCodePath);
        paramList.add(packageName);
        paramList.add(context.getApplicationInfo().nativeLibraryDir);
        paramList.add(mainClass.getName());
        paramList.add(String.valueOf(code));
        if (params != null) {
            Collections.addAll(paramList, params);
        }
        return RootServer.getLaunchScript(context, packageCodePath, mainClass, null, null, paramList.toArray(new String[0]), packageName + ":" + processName);
    }

    public void start() {
        if(packageName == null){
            packageName = context.getPackageName();
        }
        if(apkPath == null){
            apkPath = context.getPackageCodePath();
        }
        if(processName == null){
            processName = "root";
        }
        createProcess(apkPath,packageName,mainClass,param,processName);
        try {
            Method m = IPCManager.class.getDeclaredMethod("addIPCReceiver",Class.class,RootIPCReceiver.class);
            m.setAccessible(true);
            Type superClass = getClass().getGenericSuperclass();
            Type tType = ((ParameterizedType)superClass).getActualTypeArguments()[0];
            m.invoke(null,(Class<T>)tType,this);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        code++;
    }
}
