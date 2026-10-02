package com.gladiator;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class RootfsImporter {

    public static File containersRoot(Context ctx) {
        File f = new File(ctx.getFilesDir(), "container");
        f.mkdirs();
        return f;
    }

    public static void importFromUri(Context ctx, Uri uri, Container container,
                                     RootfsInstaller.Progress cb) throws Exception {
        File cache = ctx.getCacheDir();
        File tmp = new File(cache, "rootfs-import.tar.zst");

        cb.onProgress("Copiando archivo…", 2);
        long total = -1;
        try {
            android.database.Cursor c = ctx.getContentResolver()
                    .query(uri, null, null, null, null);
            if (c != null) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (idx >= 0 && c.moveToFirst()) total = c.getLong(idx);
                c.close();
            }
        } catch (Throwable ignored) {}

        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(tmp)) {
            if (in == null) throw new RuntimeException("No se pudo abrir el archivo");
            byte[] buf = new byte[262144];
            long done = 0;
            int n, last = -1;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
                done += n;
                if (total > 0) {
                    int p = (int)(done * 30 / total);
                    if (p != last) { last = p; cb.onProgress("Copiando archivo…", 2 + p); }
                }
            }
        }

        RootfsInstaller.extractTarball(ctx, tmp, container, cb);
        tmp.delete();
    }
}
