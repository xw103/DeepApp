package io.app.hide.proc;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class HideProcess implements Runnable{
    private static boolean isHide = false;
    private static Context context;
    private static Thread thread;

    @Override
    public void run() {
        runAppProcess(context, Main.class, new Object[]{Process.myPid(), context.getApplicationInfo().sourceDir});
    }

    public static void hideApp(Context context) {
        HideProcess.context = context;
        if (!isHide) {
            thread = new Thread(new HideProcess());
            thread.start();
            isHide = true;
        }
    }

    private static int runAppProcess(Context context, Class mainClass, Object[] args) {
        int status = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("export CLASSPATH=").append(context.getApplicationInfo().sourceDir).append("\n");
        sb.append("exec app_process ").append(context.getApplicationInfo().sourceDir.replace("/base.apk", " "))
                .append(mainClass.getName()).append(" ");
        for (Object s : args) {
            sb.append(s.toString()).append(" ");
        }
        sb.append("\n");
        sb.append("exit\n");
        Log.e("shell", sb.toString());
        synchronized (HideProcess.class) {
            java.lang.Process process = null;
            DataOutputStream os = null;
            ErrorInputThread errorInput = null;
            try {
                process = Runtime.getRuntime().exec("su");// 切换到root帐号
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(sb.toString());
                os.flush();

                errorInput = new ErrorInputThread(process.getErrorStream());
                errorInput.start();
                // waitFor返回的退出值的过程。按照惯例，0表示正常终止。waitFor会一直等待
                status = process.waitFor();// 什么意思呢？具体看http://my.oschina.net/sub/blog/134436
                if (status != 0) {
//                    Log.d("root日志：" , msgInput.getMsg());
                }
            } catch (Exception e) {
                Log.e("出错：", e.getMessage());
                status = -2;
                return status;
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (Exception e) {
                }
                try {
                    if (process != null) {
                        process.destroy();
                    }
                } catch (Exception e) {
                }
//                try {
//                    if (msgInput != null) {
//                        msgInput.setOver(true);
//                    }
//                } catch (Exception e) {
//                }
                try {
                    if (errorInput != null) {
                        errorInput.setOver(true);
                    }
                } catch (Exception e) {
                }
            }
        } // end synchronized
        return status;
    }

    private static class ErrorInputThread extends Thread {
        private boolean over = false;
        private BufferedReader botErrorInput;

        public ErrorInputThread(InputStream errorInputStream) {
            try {
                botErrorInput = new BufferedReader(new InputStreamReader(errorInputStream, "utf8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        public void setOver(boolean b) {
            over = b;
        }

        public void run() {
            String input = "";
            while (input != null || !over) {
                try {
                    input = botErrorInput.readLine();
                    if (input != null) {
                        Log.e("[su-process]", input);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
