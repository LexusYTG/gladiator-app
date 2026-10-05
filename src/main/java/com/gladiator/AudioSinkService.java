package com.gladiator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.IBinder;

import java.io.File;
import java.io.InputStream;

/**
 * AudioSinkService - lado Java de Orator.
 *
 * Arranca el binario bionic oratord como proceso hijo. oratord conecta al
 * socket AF_UNIX que expone PulseAudio en el container (via host-tmp) y
 * escribe PCM crudo s16le 2ch 44100Hz a su stdout. Este servicio lee ese
 * stdout como InputStream y lo escribe a un AudioTrack en STREAM mode.
 *
 * Sin sockets Java, sin API 33, sin binder. Solo ProcessBuilder + pipe.
 */
public class AudioSinkService extends Service {

    private static final String TAG = "AudioSinkService";
    private static final String CHAN = "gladiator_audio";
    private static final int NOTIF_ID = 2;
    private static final int SAMPLE_RATE = 44100;

    private Thread reader;
    private volatile boolean running;
    private Process child;

    public static void start(Context ctx) {
        Intent i = new Intent(ctx, AudioSinkService.class);
        if (android.os.Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, AudioSinkService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            NotificationChannel ch = new NotificationChannel(
                    CHAN, "Gladiator Audio", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
            Notification n = new Notification.Builder(this, CHAN)
                    .setContentTitle("Gladiator")
                    .setContentText("Audio activo")
                    .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                    .setOngoing(true).build();
            startForeground(NOTIF_ID, n);
        } catch (Throwable t) {
            GladiatorLog.log(TAG, "startForeground fallo: " + t.getMessage());
        }

        if (running) return START_STICKY;
        running = true;

        File prefix = BootstrapInstaller.prefixDir(this);
        File bin      = new File(prefix, "bin/oratord");
        File paSock   = new File(prefix, "tmp/host-tmp/orator-pa.sock");
        File logFile  = new File(prefix, "var/log/oratord.log");
        logFile.getParentFile().mkdirs();

        reader = new Thread(() -> loop(bin, paSock, logFile), "orator-reader");
        reader.setDaemon(true);
        reader.start();
        return START_STICKY;
    }

    private void loop(File bin, File paSock, File logFile) {
        while (running) {
            Process p = null;
            InputStream in = null;
            AudioTrack track = null;

            try {
                if (!bin.exists()) {
                    GladiatorLog.err(TAG, "no existe " + bin.getAbsolutePath(), null);
                    sleep(2000);
                    continue;
                }
                ProcessBuilder pb = new ProcessBuilder(
                        bin.getAbsolutePath(), paSock.getAbsolutePath());
                pb.environment().put("ORATOR_VERBOSE", "1");
                pb.redirectErrorStream(false);
                pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile));
                p = pb.start();
                child = p;
                in = p.getInputStream();

                int bufSz = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                if (bufSz <= 0) bufSz = 16384;
                bufSz *= 2;

                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                .build())
                        .setBufferSizeInBytes(bufSz)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();
                track.play();

                byte[] buf = new byte[8192];
                while (running) {
                    int r = in.read(buf);
                    if (r < 0) break;
                    if (r == 0) continue;
                    int w = track.write(buf, 0, r);
                    if (w < 0) {
                        GladiatorLog.log(TAG, "AudioTrack.write=" + w);
                        break;
                    }
                }
            } catch (Throwable t) {
                GladiatorLog.log(TAG, "loop: " + t.getMessage());
            } finally {
                if (track != null) {
                    try { track.stop(); } catch (Throwable ignored) {}
                    try { track.release(); } catch (Throwable ignored) {}
                }
                if (in != null) try { in.close(); } catch (Throwable ignored) {}
                if (p != null) {
                    try { p.destroy(); } catch (Throwable ignored) {}
                }
                child = null;
            }

            if (running) sleep(1000);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    @Override
    public void onDestroy() {
        running = false;
        if (reader != null) {
            reader.interrupt();
            try { reader.join(1500); } catch (InterruptedException ignored) {}
        }
        if (child != null) { try { child.destroy(); } catch (Throwable ignored) {} }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
