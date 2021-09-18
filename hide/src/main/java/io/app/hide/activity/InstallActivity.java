package io.app.hide.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.wind.meditor.core.FileProcesser;
import com.wind.meditor.property.AttributeItem;
import com.wind.meditor.property.ModificationProperty;
import com.wind.meditor.utils.NodeValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.app.hide.R;
import io.app.hide.apksigner.KeyHelper;
import io.app.hide.apksigner.SignApk;
import io.app.hide.utils.RandomInfo;

public class InstallActivity extends Activity implements Runnable {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install);
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            File baseDir = new File(getFilesDir().getAbsolutePath());
            if (baseDir.exists()) {
                delete(baseDir);
            }
            baseDir.mkdirs();
            //拷贝apk到工作目录
            File inFile = //new File("/storage/emulated/0/base.apk");
                    new File(getApplicationInfo().sourceDir);
            File outFile = new File(baseDir.getAbsolutePath() + "/base.apk");
            outFile.createNewFile();
            FileInputStream fis = new FileInputStream(inFile);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buf = new byte[10240];
            int len = 0;
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fis.close();
            fos.flush();
            fos.close();
            ModificationProperty property = new ModificationProperty();
            property.addManifestAttribute(new AttributeItem(NodeValue.Manifest.VERSION_CODE, RandomInfo.getVersionCode()))
                    .addManifestAttribute(new AttributeItem(NodeValue.Manifest.VERSION_NAME, RandomInfo.getVersionName()))
                    .addApplicationAttribute(new AttributeItem(NodeValue.Application.LABEL, RandomInfo.getAppName()))
                    .addManifestAttribute(new AttributeItem(NodeValue.Manifest.PACKAGE, RandomInfo.getRandomPackageName()).setNamespace(null));
            // 处理得到的未签名的apk
            FileProcesser.processApkFile(outFile.getAbsolutePath(), baseDir.getAbsolutePath() + "/app-unsigned.apk", property);
            delete(outFile);
            if (signApk(baseDir.getAbsolutePath() + "/app-unsigned.apk", baseDir.getAbsolutePath() + "/app-release.apk")) {
                installApk();
            } else {
                throw new Exception("签名失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(InstallActivity.this);
                builder.setTitle("随机安装失败");
                builder.setMessage(Log.getStackTraceString(e));
                builder.setPositiveButton("确定", (dialog, which) -> {
                    dialog.dismiss();
                });
                AlertDialog d = builder.create();
                d.show();
            });
        }
    }

    public void installApk() {
        runShell("pm install " + getFilesDir().getAbsolutePath() + "/app-release.apk", true);
        PackageManager pm = getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(getFilesDir().getAbsolutePath() + "/app-release.apk", 0);
        if (pi == null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(InstallActivity.this);
                builder.setTitle("安装失败");
                builder.setMessage("apk打包失败");
                builder.setPositiveButton("确定", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                });
                AlertDialog d = builder.create();
                d.show();
            });
        }else {
            new Handler(Looper.getMainLooper()).post(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(InstallActivity.this);
                builder.setTitle("提示");
                builder.setMessage("安装成功:" +
                        "\n软件名：" + pi.applicationInfo.nonLocalizedLabel +
                        "\n包名：" + pi.packageName);
                builder.setPositiveButton("确定", (dialog, which) -> {
                    runShell("pm uninstall " + getPackageName(), true);
                });
                AlertDialog d = builder.create();
                d.show();
            });
        }
    }


    public byte[] runShell(String command, boolean isRoot) {
//        System.out.println("cmd:"+command);
        try {
            Process process = Runtime.getRuntime().exec(isRoot ? "su" : "sh");
            InputStream ins = process.getInputStream();
            InputStream es = process.getErrorStream();
            OutputStream ous = process.getOutputStream();
            ous.write("\n".getBytes());
            ous.flush();
            ous.write(command.getBytes());
            ous.flush();
            ous.write("\n".getBytes());
            ous.flush();
            ous.write("exit".getBytes());
            ous.flush();
            ous.write("\n".getBytes());
            ous.flush();
            byte[] result = readInputStream(ins, false);
            byte[] error = readInputStream(es, false);
            process.waitFor();
            ins.close();
            es.close();
            ous.close();
            if (new String(error).trim().isEmpty()) {
                return result;
            } else {
                return null;
            }
        } catch (Throwable th) {
            return null;
        }
    }

    public static byte[] readInputStream(InputStream ins, boolean close) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int i = -1;
            byte[] buf = new byte[1024];
            while ((i = ins.read(buf)) != -1) {
                bos.write(buf, 0, i);
            }
            if (close) {
                ins.close();
                bos.close();
            }
            return bos.toByteArray();
        } catch (Throwable th) {
            return Log.getStackTraceString(th).getBytes();
        }
    }

    public static boolean signApk(String src, String dest) throws Exception {
        SignApk signApk = new SignApk(KeyHelper.privateKey, KeyHelper.sigPrefix);

        boolean signed = signApk.sign(src, dest);
        if (signed) {
            //verify signed apk
            return SignApk.verifyJar(dest);
        }
        return false;
    }

    public static void delete(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }

        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                delete(childFiles[i]);
            }
            file.delete();
        }
    }
}
