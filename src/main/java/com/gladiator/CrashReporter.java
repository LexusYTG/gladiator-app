package com.gladiator;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

public class CrashReporter {
    private static final String TAG = "CrashReporter";

    public static void reportPastDeaths(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;
            List<ApplicationExitInfo> list = am.getHistoricalProcessExitReasons(
                    ctx.getPackageName(), 0, 10);
            if (list == null || list.isEmpty()) {
                GladiatorLog.log(TAG, "sin decesos previos registrados");
                return;
            }
            int n = 0;
            for (ApplicationExitInfo info : list) {
                if (n++ >= 3) break;
                GladiatorLog.log(TAG, describe(info));
            }
        } catch (Throwable t) {
            GladiatorLog.err(TAG, "no pude leer exit reasons", t);
        }
    }

    private static String describe(ApplicationExitInfo i) {
        return "deceso: reason=" + i.getReason() + " pid=" + i.getPid()
                + " ts=" + i.getTimestamp() + " desc=" + i.getDescription();
    }
}
