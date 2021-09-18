package io.app.hide.proc;

import java.io.File;
import java.io.IOException;

public class Main {
    private static String path;

    public static void main(String[] args) {
        path = args[1].replace("/data/app/", "");
        path = "/data/app/" + path.substring(0, path.indexOf('/'));
        try {
            Runtime.getRuntime().exec("chmod -R 000 " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File f = new File("/proc/" + args[0] + "/maps");
        while (true) {
            if (!f.exists()) {
                try {
                    Runtime.getRuntime().exec("chmod -R 775 " + path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
