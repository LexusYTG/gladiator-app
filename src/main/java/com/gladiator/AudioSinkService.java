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
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.IBinder;

import java.io.File;
import java.io.InputStream;

/**
 * AudioSinkService - lee PCM de PulseAudio y lo reproduce en Android.
 *
 * Java puro. Sin binario intermedio. Se conecta al socket AF_UNIX que
 * expone module-simple-protocol-unix de PulseAudio (vive en host-tmp/,
 * bindeado al container). Lee PCM crudo s16le 2ch 44100Hz y lo escribe
 * a un AudioTrack en STREAM mode.
 *
 * Sin proceso intermedio: sin huérfanos, sin doble oratord, sin pipe.
 */
public class AudioSinkService extends Service {

    private static final String TAG = "AudioSinkService";
    private static final String CHAN = "gladiator_audio";
    private static final int NOTIF_ID = 2;
    private static final int SAMPLE_RATE = 44100;

    private Thread reader;
    private volatile boolean running;
    private LocalSocket sock;

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
        android.util.Log.i(TAG, "onStartCommand entro, running=" + running);
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
        File paSock = new File(prefix, "tmp/host-tmp/orator-pa.sock");

        reader = new Thread(() -> loop(paSock), "audio-sink");
        reader.setDaemon(true);
        reader.start();
        return START_STICKY;
    }

    private void loop(File paSock) {
        while (running) {
            InputStream in = null;
            AudioTrack track = null;

            try {
                android.util.Log.i(TAG, "conectando a " + paSock.getAbsolutePath());
                LocalSocket s = new LocalSocket();
                s.connect(new LocalSocketAddress(
                        paSock.getAbsolutePath(),
                        LocalSocketAddress.Namespace.FILESYSTEM));
                sock = s;
                in = s.getInputStream();
                android.util.Log.i(TAG, "conectado, getInputStream OK");

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
                android.util.Log.i(TAG, "AudioTrack.play OK, bufSz=" + bufSz);

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
                android.util.Log.e(TAG, "loop exception", t);
                GladiatorLog.log(TAG, "loop: " + t.getMessage());
            } finally {
                if (track != null) {
                    try { track.stop(); } catch (Throwable ignored) {}
                    try { track.release(); } catch (Throwable ignored) {}
                }
                if (in != null) try { in.close(); } catch (Throwable ignored) {}
                if (sock != null) {
                    try { sock.close(); } catch (Throwable ignored) {}
                    sock = null;
                }
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
        if (sock != null) { try { sock.close(); } catch (Throwable ignored) {} }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
