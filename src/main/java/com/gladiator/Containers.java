package com.gladiator;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Containers {
    public static List<Container> list(Context ctx) {
        File base = BootstrapInstaller.containersDir(ctx);
        File[] dirs = base.listFiles(File::isDirectory);
        List<Container> out = new ArrayList<>();
        if (dirs == null) return out;
        for (File d : dirs) out.add(new Container(d.getName(), d));
        Collections.sort(out, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    public static boolean nameExists(Context ctx, String name) {
        for (Container c : list(ctx)) if (c.name.equals(name)) return true;
        return false;
    }

    public static String sanitize(String name) {
        if (name == null) return null;
        String s = name.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (s.isEmpty()) return null;
        if (s.startsWith(".")) s = "_" + s.substring(1);
        return s;
    }
}
