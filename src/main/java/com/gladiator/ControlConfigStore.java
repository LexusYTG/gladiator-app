package com.gladiator;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ControlConfigStore {
    private static final String PREFS = "gladiator_controls";
    private static final String KEY = "global_controls";

    public static List<ControlConfig> load(Context ctx, String ignored) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY, null);
        List<ControlConfig> out = new ArrayList<>();
        if (json == null) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ControlConfig c = new ControlConfig();
                c.id = o.optString("id");
                c.label = o.optString("label");
                c.type = o.optInt("type", ControlConfig.TYPE_BUTTON);
                c.shape = o.optInt("shape", ControlConfig.SHAPE_CIRCLE);
                c.keyCode = o.optInt("keyCode");
                c.cx = (float) o.optDouble("cx", 0.5);
                c.cy = (float) o.optDouble("cy", 0.5);
                c.sizeFrac = (float) o.optDouble("size", 0.15);
                c.keyUp = o.optInt("keyUp");
                c.keyDown = o.optInt("keyDown");
                c.keyLeft = o.optInt("keyLeft");
                c.keyRight = o.optInt("keyRight");
                c.sens = (float) o.optDouble("sens", ControlConfig.DEFAULT_SENS);
                out.add(c);
            }
        } catch (Exception ignored2) {}
        return out;
    }

    public static void save(Context ctx, String ignored, List<ControlConfig> list) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray();
            for (ControlConfig c : list) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("label", c.label);
                o.put("type", c.type);
                o.put("shape", c.shape);
                o.put("keyCode", c.keyCode);
                o.put("cx", c.cx);
                o.put("cy", c.cy);
                o.put("size", c.sizeFrac);
                o.put("keyUp", c.keyUp);
                o.put("keyDown", c.keyDown);
                o.put("keyLeft", c.keyLeft);
                o.put("keyRight", c.keyRight);
                o.put("sens", c.sens);
                arr.put(o);
            }
            sp.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored2) {}
    }

    // ============================================================ multi-perfil

    /** Directorio de perfiles exportados/importados, bindeado al container. */
    public static File profilesDir(Context ctx) {
        File d = new File(BootstrapInstaller.prefixDir(ctx), "tmp/host-tmp/controls");
        d.mkdirs();
        return d;
    }

    /** Lista los .json en el directorio de perfiles, ordenados por nombre. */
    public static java.util.List<File> listLocal(Context ctx) {
        java.util.List<File> out = new java.util.ArrayList<>();
        File dir = profilesDir(ctx);
        File[] fs = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (fs == null) return out;
        java.util.Arrays.sort(fs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File f : fs) out.add(f);
        return out;
    }

    /** Nombre del perfil sin extension. */
    public static String nameOf(File f) {
        String n = f.getName();
        return n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
    }

    /** Serializa la lista al mismo formato que save() (JSON array). */
    public static String toJson(java.util.List<ControlConfig> list) {
        try {
            JSONArray arr = new JSONArray();
            for (ControlConfig c : list) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("label", c.label);
                o.put("type", c.type);
                o.put("shape", c.shape);
                o.put("keyCode", c.keyCode);
                o.put("cx", c.cx);
                o.put("cy", c.cy);
                o.put("size", c.sizeFrac);
                o.put("keyUp", c.keyUp);
                o.put("keyDown", c.keyDown);
                o.put("keyLeft", c.keyLeft);
                o.put("keyRight", c.keyRight);
                o.put("sens", c.sens);
                arr.put(o);
            }
            return arr.toString(2);
        } catch (Exception e) { return "[]"; }
    }

    /** Parsea un JSON array al mismo formato que load(). */
    public static java.util.List<ControlConfig> fromJson(String json) {
        java.util.List<ControlConfig> out = new java.util.ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ControlConfig c = new ControlConfig();
                c.id = o.optString("id");
                c.label = o.optString("label");
                c.type = o.optInt("type", ControlConfig.TYPE_BUTTON);
                c.shape = o.optInt("shape", ControlConfig.SHAPE_CIRCLE);
                c.keyCode = o.optInt("keyCode");
                c.cx = (float) o.optDouble("cx", 0.5);
                c.cy = (float) o.optDouble("cy", 0.5);
                c.sizeFrac = (float) o.optDouble("size", 0.15);
                c.keyUp = o.optInt("keyUp");
                c.keyDown = o.optInt("keyDown");
                c.keyLeft = o.optInt("keyLeft");
                c.keyRight = o.optInt("keyRight");
                c.sens = (float) o.optDouble("sens", ControlConfig.DEFAULT_SENS);
                out.add(c);
            }
        } catch (Exception ignored2) {}
        return out;
    }

    /** Escribe el perfil a un archivo .json en el directorio de perfiles. */
    public static boolean export(Context ctx, String name, java.util.List<ControlConfig> list) {
        try {
            String safe = name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
            if (safe.isEmpty()) return false;
            File out = new File(profilesDir(ctx), safe + ".json");
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(toJson(list).getBytes(StandardCharsets.UTF_8));
            fos.close();
            out.setReadable(true, false);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Lee un perfil de un archivo. */
    public static java.util.List<ControlConfig> importFrom(File f) {
        try {
            FileInputStream in = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int r = in.read(buf);
            in.close();
            if (r <= 0) return new java.util.ArrayList<>();
            return fromJson(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) { return new java.util.ArrayList<>(); }
    }

}
