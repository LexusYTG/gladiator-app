package com.gladiator;

import android.content.Context;

import java.io.File;

public class Container {
    public final String name;
    public final File dir;
    public final File rootfs;

    public Container(String name, File dir) {
        this.name = name;
        this.dir = dir;
        this.rootfs = new File(dir, "rootfs");
    }

    public boolean isInstalled() {
        // ubuntu-init.sh deja el marker en $HOME/.sesar-ready (=$ROOTFS/root/.sesar-ready)
        return new File(rootfs, "root/.sesar-ready").exists();
    }

    public static Container forName(Context ctx, String name) {
        File dir = new File(BootstrapInstaller.containersDir(ctx), name);
        return new Container(name, dir);
    }
}
