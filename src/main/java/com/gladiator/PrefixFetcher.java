package com.gladiator;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * PrefixFetcher - descarga prefixes.json del repo gladiator-init-setup y lo
 * escribe en files/usr/tmp/host-tmp/prefixes.json. Ese directorio está
 * bindeado dentro del container proot como /host-tmp, así que sesar-shell
 * lo lee sin necesitar curl ni red dentro del container.
 */
public class PrefixFetcher {

    private static final String TAG = "PrefixFetcher";
    private static final String URL =
            "https://raw.githubusercontent.com/LexusYTG/gladiator-init-setup/main/prefixes.json";
    private static final int TIMEOUT_MS = 10000;
    private static final int MAX_BYTES = 1024 * 1024;

    public static void fetchAsync(Context ctx) {
        final Context app = ctx.getApplicationContext();
        Thread t = new Thread(() -> fetch(app), "prefix-fetcher");
        t.setDaemon(true);
        t.start();
    }

    public static boolean fetch(Context ctx) {
        File prefix = BootstrapInstaller.prefixDir(ctx);
        File hostTmp = new File(prefix, "tmp/host-tmp");
        if (!hostTmp.exists() && !hostTmp.mkdirs()) {
            GladiatorLog.log(TAG, "no puedo crear " + hostTmp);
            return false;
        }
        File dst = new File(hostTmp, "prefixes.json");
        File tmp = new File(hostTmp, "prefixes.json.tmp");

        HttpURLConnection conn = null;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URL u = new URL(URL);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Gladiator/1.0");
            conn.setUseCaches(false);
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            conn.connect();
            int code = conn.getResponseCode();
            if (code != 200) {
                GladiatorLog.log(TAG, "HTTP " + code);
                return false;
            }
            in = conn.getInputStream();
            out = new FileOutputStream(tmp);
            byte[] buf = new byte[8192];
            int r;
            long total = 0;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
                total += r;
                if (total > MAX_BYTES) {
                    GladiatorLog.log(TAG, "archivo demasiado grande, abortando");
                    return false;
                }
            }
            out.flush();
            out.close();
            out = null;

            if (total < 32) {
                GladiatorLog.log(TAG, "archivo vacío o inválido");
                tmp.delete();
                return false;
            }
            if (dst.exists() && !dst.delete()) {
                GladiatorLog.log(TAG, "no puedo borrar destino viejo");
                tmp.delete();
                return false;
            }
            if (!tmp.renameTo(dst)) {
                GladiatorLog.log(TAG, "rename falló");
                tmp.delete();
                return false;
            }
            // legible para el container (proot corre como el mismo uid,
            // pero por las dudas forzamos permisos)
            dst.setReadable(true, false);
            GladiatorLog.log(TAG, "prefixes.json actualizado (" + total + " bytes)");
            return true;
        } catch (Throwable t) {
            GladiatorLog.log(TAG, "fetch: " + t.getMessage());
            return false;
        } finally {
            try { if (in  != null) in.close();  } catch (Throwable ignored) {}
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
            if (conn != null) conn.disconnect();
        }
    }
}
