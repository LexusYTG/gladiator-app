package com.gladiator;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GladiatorLog {
    private static final String TAG = "GladiatorLog";
    private static BufferedWriter writer;
    private static final Object lock = new Object();
    private static File logFile;
    private static boolean initDone = false;

    public static void init(Context ctx) {
        if (initDone) return;
        initDone = true;
        try {
            File base = null;
            File[] dirs = ctx.getExternalMediaDirs();
            if (dirs != null && dirs.length > 0) base = dirs[0];
            if (base == null) base = ctx.getExternalFilesDir(null);
            File logDir = new File(base, "logs");
            logDir.mkdirs();

            String proc = getProcessName();
            String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
            logFile = new File(logDir, "gladiator-" + proc + "-" + ts + ".log");
            writer = new BufferedWriter(new FileWriter(logFile, true));

            log("=== Gladiator start ===");
            log("proc: " + proc + " pid: " + Process.myPid());
            log("log file: " + logFile.getAbsolutePath());
            log("device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
            log("sdk: " + android.os.Build.VERSION.SDK_INT);

            System.setOut(new PrintStream(new LineWriter("OUT"), true));
            System.setErr(new PrintStream(new LineWriter("ERR"), true));

            final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                log("!!! UNCAUGHT en hilo " + t.getName() + " !!!");
                log(stack(e));
                try { writer.flush(); } catch (Exception ignored) {}
                if (prev != null) prev.uncaughtException(t, e);
            });

        } catch (Exception e) {
            Log.e(TAG, "init falló", e);
        }
    }

    private static String getProcessName() {
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.FileReader("/proc/self/cmdline"));
            String line = r.readLine();
            r.close();
            if (line != null) {
                int n = line.indexOf('\0');
                String name = n > 0 ? line.substring(0, n) : line;
                if (name.contains(":")) name = name.substring(name.indexOf(':') + 1);
                return name;
            }
        } catch (Exception ignored) {}
        return "main";
    }

    public static void log(String msg) {
        synchronized (lock) {
            try {
                String line = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
                        .format(new Date()) + " " + msg;
                Log.i(TAG, msg);
                if (writer != null) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            } catch (Exception ignored) {}
        }
    }

    public static void log(String tag, String msg) { log("[" + tag + "] " + msg); }

    public static void err(String tag, String msg, Throwable t) {
        log("[" + tag + "] ERROR: " + msg);
        if (t != null) log(stack(t));
    }

    public static String stack(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static File getLogFile() { return logFile; }

    private static class LineWriter extends OutputStream {
        private final String tag;
        private final StringBuilder buf = new StringBuilder();
        LineWriter(String tag) { this.tag = tag; }
        @Override public void write(int b) {
            if (b == '\n') { log("[" + tag + "] " + buf); buf.setLength(0); }
            else buf.append((char) b);
        }
    }
}
