package com.gladiator;

import android.content.Context;
import android.system.Os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extrae el bootstrap de Termux (assets/bootstrap-aarch64.zip) a filesDir/usr/.
 * Con targetSdk 28 se puede ejecutar cualquier binario desde filesDir — no hace
 * falta ni jniLibs ni nativeLibraryDir.
 */
public class BootstrapInstaller {
    private static final String TAG = "BootstrapInstaller";
    private static final String ASSET = "bootstrap-aarch64.zip";

    public interface Progress { void onProgress(String msg, int pct); }

    public static File prefixDir(Context ctx) { return new File(ctx.getFilesDir(), "usr"); }
    public static File homeDir(Context ctx)   { return new File(ctx.getFilesDir(), "home"); }
    public static File tmpDir(Context ctx)    { File f = new File(ctx.getFilesDir(), "tmp"); f.mkdirs(); return f; }
    public static File containersDir(Context ctx) {
        File f = new File(prefixDir(ctx), "var/lib/proot-distro/containers");
        f.mkdirs();
        return f;
    }

    public static boolean isInstalled(Context ctx) {
        return new File(prefixDir(ctx), "bin/bash").exists()
                && new File(prefixDir(ctx), "bin/proot").exists()
                && new File(prefixDir(ctx), "bin/proot-distro").exists();
    }

    public static void installIfNeeded(Context ctx, Progress cb) throws Exception {
        if (isInstalled(ctx)) {
            // Idempotente: repara permisos (+x) en instalaciones existentes.
            chmodExecutables(prefixDir(ctx));
            cb.onProgress("Bootstrap ya instalado", 100);
            return;
        }
        File prefix = prefixDir(ctx);
        prefix.mkdirs();

        cb.onProgress("Extrayendo bootstrap…", 0);
        extractZip(ctx, ASSET, prefix, cb);

        cb.onProgress("Creando symlinks…", 85);
        createSymlinks(prefix);

        cb.onProgress("Regenerando symlinks de libs…", 90);
        regenerateLibSymlinks(prefix);

        cb.onProgress("Ajustando permisos…", 92);
        chmodExecutables(prefix);

        cb.onProgress("Bootstrap listo", 100);
        GladiatorLog.log(TAG, "bootstrap instalado en " + prefix.getAbsolutePath());
    }

    private static void extractZip(Context ctx, String assetName, File dest, Progress cb) throws Exception {
        try (InputStream is = ctx.getAssets().open(assetName, android.content.res.AssetManager.ACCESS_STREAMING);
             ZipInputStream zis = new ZipInputStream(is)) {

            long total = is.available();
            long done = 0;
            int last = -1;
            byte[] buf = new byte[262144];
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                File out = new File(dest, e.getName());
                if (e.isDirectory()) {
                    out.mkdirs();
                } else {
                    out.getParentFile().mkdirs();
                    // Saltar SYMLINKS.txt — se maneja aparte
                    if (e.getName().equals("SYMLINKS.txt")) {
                        // guardarlo temporalmente en dest/.symlinks.txt
                        try (OutputStream os = new FileOutputStream(new File(dest, ".symlinks.txt"))) {
                            int n;
                            while ((n = zis.read(buf)) > 0) os.write(buf, 0, n);
                        }
                        continue;
                    }
                    try (OutputStream os = new FileOutputStream(out)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) os.write(buf, 0, n);
                    }
                    // ZipInputStream no expone permisos Unix; chmodExecutables() repone +x despues de extraer.
                }
                done++;
                if (cb != null) {
                    // Progreso por archivos no es fiable; usamos nada más el contador
                    int pct = (int)Math.min(80, done / 40);
                    if (pct != last) {
                        last = pct;
                        cb.onProgress("Extrayendo bootstrap…", pct);
                    }
                }
            }
        }
    }

    private static void createSymlinks(File prefix) {
        File symlinksTxt = new File(prefix, ".symlinks.txt");
        if (!symlinksTxt.exists()) return;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(symlinksTxt), StandardCharsets.UTF_8))) {
            String line;
            int created = 0;
            while ((line = r.readLine()) != null) {
                int arrow = line.indexOf('\u2190');
                if (arrow <= 0) continue;
                String target = line.substring(0, arrow);
                String linkPath = line.substring(arrow + 1);
                File linkFile = new File(prefix, linkPath);
                linkFile.getParentFile().mkdirs();
                try {
                    // si ya existe, borrar antes
                    if (linkFile.exists()) linkFile.delete();
                    Os.symlink(target, linkFile.getAbsolutePath());
                    created++;
                } catch (Throwable t) {
                    GladiatorLog.log(TAG, "symlink falló: " + linkPath + " -> " + target + " (" + t.getMessage() + ")");
                }
            }
            GladiatorLog.log(TAG, "symlinks creados: " + created);
        } catch (Exception e) {
            GladiatorLog.err(TAG, "no pude leer symlinks", e);
        }
        symlinksTxt.delete();
    }

    private static void regenerateLibSymlinks(File prefix) {
        File libDir = new File(prefix, "lib");
        File[] files = libDir.listFiles();
        if (files == null) return;
        int created = 0;
        for (File f : files) {
            if (!f.isFile()) continue;
            String name = f.getName();
            if (!name.startsWith("lib") || !name.contains(".so.")) continue;
            int soIdx = name.indexOf(".so.");
            if (soIdx < 0) continue;
            String after = name.substring(soIdx + 4);
            if (after.isEmpty() || !Character.isDigit(after.charAt(0))) continue;
            int dot = after.indexOf('.');
            if (dot < 0) continue;
            String soname = name.substring(0, soIdx + 4) + after.substring(0, dot);
            String base = name.substring(0, soIdx) + ".so";
            created += ensureSymlink(f, new File(libDir, soname));
            created += ensureSymlink(f, new File(libDir, base));
        }
        GladiatorLog.log(TAG, "regenerateLibSymlinks: " + created + " creados");
    }

    private static int ensureSymlink(File target, File link) {
        if (link.exists()) return 0;
        try {
            Os.symlink(target.getName(), link.getAbsolutePath());
            return 1;
        } catch (Throwable t) {
            GladiatorLog.log(TAG, "symlink fallo: " + link.getName() + " -> " + target.getName()
                    + " (" + t.getMessage() + ")");
            return 0;
        }
    }

    private static void chmodExecutables(File prefix) {
        chmodRecursive(new File(prefix, "bin"), false);
        // libexec/proot/loader y loader32 viven en un subdirectorio: recursivo.
        chmodRecursive(new File(prefix, "libexec"), true);
        chmodRecursive(new File(prefix, "lib/apt/methods"), true);
        File lib = new File(prefix, "lib");
        File[] libs = lib.listFiles();
        if (libs != null) for (File f : libs) {
            if (f.isFile() && f.getName().contains(".so")) f.setExecutable(true, false);
        }
    }

    private static void chmodRecursive(File dir, boolean recurse) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (recurse) chmodRecursive(f, true);
            } else if (f.isFile()) {
                f.setExecutable(true, false);
            }
        }
    }
}
