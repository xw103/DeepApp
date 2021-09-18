package io.app.hide.utils;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class RandomInfo {
    public static int getVersionCode(){
        return new Random().nextInt(99999);
    }

    public static String getVersionName(){
        StringBuffer str = new StringBuffer();
        str.append(new Random().nextInt(99) + 1);
        str.append(".");
        str.append(new Random().nextInt(10 - 4 + 1) + 4);
        if(new Random().nextInt(2) == 1){
            str.append('.');
            str.append(new Random().nextInt(10 - 4 + 1) + 4);
        }
        return str.toString();
    }

    public static String getRandomPackageName(){
        StringBuffer str = new StringBuffer("com.");
        str.append(getRandStr(new Random().nextInt(10 - 4 + 1) + 4));
        if(new Random().nextInt(2) == 1){
            str.append('.');
            str.append(getRandStr(new Random().nextInt(10 - 4 + 1) + 4));
        }
        return str.toString();
    }

    private static String getRandStr(int num){
        String strs = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuffer buff = new StringBuffer();
        for(int i=1;i<=num;i++){
            char str = strs.charAt((int)(Math.random() * strs.length()));
            buff.append(str);
        }
        return buff.toString();
    }

    public static String getAppName(){
        StringBuffer buff = new StringBuffer();
        for(int i = 1; i<=(new Random().nextInt(6 - 1 + 1) + 1); i++){
            buff.append(getRandomChar());
        }
        return buff.toString();
    }


    private static char getRandomChar() {
        String str = "";
        int hightPos;
        int lowPos;
        Random random = new Random();

        hightPos = (176 + Math.abs(random.nextInt(39)));
        lowPos = (161 + Math.abs(random.nextInt(93)));

        byte[] b = new byte[2];
        b[0] = (Integer.valueOf(hightPos)).byteValue();
        b[1] = (Integer.valueOf(lowPos)).byteValue();

        try {
            str = new String(b, "GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return str.charAt(0);
    }

}
