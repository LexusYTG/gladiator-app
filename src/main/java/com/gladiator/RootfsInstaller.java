package com.gladiator;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class RootfsInstaller {
    private static final String TAG = "GladiatorRootfs";

    public interface Progress { void onProgress(String message, int percent); }

    public static File containerDir(Context ctx) {
        return RootfsImporter.containersRoot(ctx);
    }

    public static File binDir(Context ctx) {
        return new File(ctx.getApplicationInfo().nativeLibraryDir);
    }

    public static File libDir(Context ctx) {
        File d = new File(ctx.getFilesDir(), "lib");
        d.mkdirs();
        return d;
    }

    public static void extractLibsIfNeeded(Context ctx) throws Exception {
        File libDir = libDir(ctx);
        AssetManager am = ctx.getAssets();
        String[] assets = am.list("lib");
        if (assets == null) return;
        for (String name : assets) {
            File out = new File(libDir, name);
            if (out.exists() && out.length() > 0) continue;
            copyAsset(ctx, "lib/" + name, out);
        }
    }

    public static void extractTarball(Context ctx, File tarball, Container container,
                                      Progress cb) throws Exception {
        File bin = binDir(ctx);
        File zstd = new File(bin, "libzstdcli.so");
        String ldPath = libDir(ctx).getAbsolutePath() + ":" + bin.getAbsolutePath();

        File tarFile = new File(ctx.getCacheDir(), "rootfs.tar");

        cb.onProgress("Descomprimiendo…", 35);
        try (OutputStream os = new FileOutputStream(tarFile)) {
            ProcessBuilder pb1 = new ProcessBuilder(
                    zstd.getAbsolutePath(), "-d", "-c", tarball.getAbsolutePath());
            pb1.environment().put("LD_LIBRARY_PATH", ldPath);
            pb1.redirectErrorStream(false);
            Process p1 = pb1.start();
            long total = tarball.length() * 5L;
            long done = 0;
            int last = -1;
            try (InputStream is = p1.getInputStream()) {
                byte[] buf = new byte[262144];
                int n;
                while ((n = is.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    done += n;
                    int p = (int) Math.min(35 + done * 30 / total, 65);
                    if (p != last) { last = p; cb.onProgress("Descomprimiendo…", p); }
                }
            }
            int r1 = p1.waitFor();
            if (r1 != 0) throw new RuntimeException("zstd falló: " + readAll(p1.getErrorStream()));
        }

        cb.onProgress("Extrayendo archivos…", 68);
        container.dir.mkdirs();
        ProcessBuilder pb2 = new ProcessBuilder(
                new File(bin, "libtarcli.so").getAbsolutePath(),
                "-xf", tarFile.getAbsolutePath(),
                "-C", container.dir.getAbsolutePath());
        pb2.environment().put("LD_LIBRARY_PATH", ldPath);
        pb2.redirectErrorStream(false);
        Process p2 = pb2.start();

        final ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        Thread tOut = new Thread(() -> copyStream(p2.getInputStream(), new ByteArrayOutputStream()));
        Thread tErr = new Thread(() -> copyStream(p2.getErrorStream(), errBuf));
        tOut.start(); tErr.start();
        int r2 = p2.waitFor();
        tOut.join(); tErr.join();

        tarFile.delete();

        if (r2 != 0) throw new RuntimeException("tar falló: " + errBuf.toString("UTF-8"));

        new File(container.rootfs, "usr/local/bin/gladiator-session.sh").setExecutable(true, false);
        cb.onProgress("Rootfs listo", 95);
    }

    private static void copyAsset(Context ctx, String path, File out) throws Exception {
        AssetManager am = ctx.getAssets();
        try (InputStream in = am.open(path, AssetManager.ACCESS_STREAMING);
             OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        }
    }

    private static String readAll(InputStream in) {
        if (in == null) return "";
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return b.toString("UTF-8");
        } catch (Exception e) { return "(error: " + e.getMessage() + ")"; }
    }

    private static void copyStream(InputStream in, OutputStream out) {
        if (in == null) return;
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception ignored) {}
    }
}
