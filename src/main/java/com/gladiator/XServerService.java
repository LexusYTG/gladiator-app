package com.gladiator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.system.Os;

import com.termux.x11.CmdEntryPoint;

import java.io.File;
import java.io.FileOutputStream;

public class XServerService extends Service {
    private static final String TAG = "XServerService";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            File stderrLog = new File(getFilesDir(), "xserver-stderr.log");
            FileOutputStream fos = new FileOutputStream(stderrLog, false);
            Os.dup2(fos.getFD(), 1);
            Os.dup2(fos.getFD(), 2);
        } catch (Throwable t) {
            android.util.Log.e(TAG, "no pude redirigir stderr", t);
        }
        GladiatorLog.init(getApplicationContext());
        GladiatorLog.log(TAG, "onCreate");
    }

    private static void setEnv(String key, String value) {
        try {
            Os.setenv(key, value, true);
            GladiatorLog.log(TAG, "Os.setenv " + key + "=" + value);
        } catch (Throwable t) {
            GladiatorLog.err(TAG, "Os.setenv falló para " + key, t);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Foreground service: Android mata services normales en background a los pocos minutos.
        // El apt install puede tardar 10+ min, así que necesitamos notificación persistente.
        try {
            android.app.NotificationChannel ch = new android.app.NotificationChannel(
                    "gladiator_xserver", "Sesion Gladiator",
                    android.app.NotificationManager.IMPORTANCE_LOW);
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
            android.app.Notification n = new android.app.Notification.Builder(this, "gladiator_xserver")
                    .setContentTitle("Gladiator")
                    .setContentText("Sesion en ejecucion")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setOngoing(true)
                    .build();
            startForeground(1, n);
        } catch (Throwable t) {
            GladiatorLog.log(TAG, "startForeground fallo: " + t.getMessage());
        }
        File prefix = BootstrapInstaller.prefixDir(this);

        // XKB viene del bootstrap, no del contenedor
        File xkb = new File(prefix, "share/X11/xkb");
        if (!xkb.exists() || !xkb.isDirectory()) {
            GladiatorLog.log(TAG, "xkb no existe en " + xkb.getAbsolutePath() + ", abortando");
            return START_NOT_STICKY;
        }

        // TMPDIR también en el prefix para que cmdentrypoint.cpp encuentre
        // /share/fonts, /etc/X11/fonts, etc relativos a dirname(TMPDIR)
        File tmp = new File(prefix, "tmp");
        new File(tmp, ".X11-unix").mkdirs();

        setEnv("TMPDIR", tmp.getAbsolutePath());
        setEnv("XKB_CONFIG_ROOT", xkb.getAbsolutePath());
        setEnv("DISPLAY", ":0");

        File fonts = new File(prefix, "share/fonts");
        if (fonts.exists()) {
            setEnv("X11_FONT_PATH", fonts.getAbsolutePath());
        }

        final String[] args = {":0"};

        new Thread(() -> {
            Looper.prepare();
            try {
                CmdEntryPoint.main(args);
            } catch (Throwable t) {
                GladiatorLog.err(TAG, "Fatal", t);
            }
        }, "xserver-main").start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
