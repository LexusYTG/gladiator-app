package com.gladiator;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String NAME = "gladiator_settings";

    public static final String[] RESOLUTIONS = {
            "800x600", "1024x768", "1280x720", "1280x800", "1366x768",
            "1600x900", "1920x1080", "2560x1440"
    };
    public static final String[] DEPTHS = {"16", "24", "32"};
    public static final String[] QUALITY = {"Rendimiento", "Balance", "Calidad"};
    public static final String[] INPUT_MODES = {"Botones volumen", "Gestos"};

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public String resolution() { return sp.getString("resolution", "1280x720"); }
    public void setResolution(String v) { sp.edit().putString("resolution", v).apply(); }

    public int depth() { return sp.getInt("depth", 24); }
    public void setDepth(int v) { sp.edit().putInt("depth", v).apply(); }

    public int quality() { return sp.getInt("quality", 1); }
    public void setQuality(int v) { sp.edit().putInt("quality", v).apply(); }

    public int inputMode() { return sp.getInt("input_mode", 0); }
    public void setInputMode(int v) { sp.edit().putInt("input_mode", v).apply(); }

    public String screenSpec() { return resolution() + "x" + depth(); }
}
