package com.gladiator;

import com.termux.x11.LorieApp;

public class GladiatorApp extends LorieApp {
    @Override
    public void onCreate() {
        PrefixFetcher.fetchAsync(this);
        GladiatorLog.init(this);
        GladiatorLog.log("App", "onCreate start (proc=" + getPackageName() + ")");
        CrashReporter.reportPastDeaths(this);
        try {
            super.onCreate();
            GladiatorLog.log("App", "super.onCreate OK");
        } catch (Throwable t) {
            GladiatorLog.err("App", "super.onCreate falló", t);
            throw t;
        }
    }
}
