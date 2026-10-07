package com.gladiator;

import android.content.Context;
import android.content.SharedPreferences;

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
}
