package com.gladiator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class LauncherActivity extends Activity {
    private static final String PREFS = "gladiator";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final int MAX_W_DP = 720;

    private FrameLayout root;
    private View content;
    private SharedPreferences prefs;
    private Ui.BootPanel boot;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.styleWindow(this);
        GladiatorLog.init(this);
        GladiatorLog.log("Launcher", "onCreate");
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        root.addView(new Ui.GridBackground(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        showBoot();
    }

    private void setContent(View v) {
        if (content != null) root.removeView(content);
        content = v;
        root.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        v.setAlpha(0f);
        v.animate().alpha(1f).setStartDelay(0).setDuration(240).start();
    }

    private void showBoot() {
        boot = new Ui.BootPanel(this, null);
        boot.set("Inicializando…", 0);
        setContent(boot);
        new Thread(this::bootTask, "boot").start();
    }

    private void bootTask() {
        try {
            GladiatorLog.log("Boot", "inicio");
            setBoot("Verificando binarios…", 10);
            // Con bootstrap de Termux no hay binarios en jniLibs.

            setBoot("Verificando bootstrap…", 20);
            if (!BootstrapInstaller.isInstalled(this)) {
                BootstrapInstaller.installIfNeeded(this, (msg, pct) ->
                        setBoot(msg, 20 + pct * 60 / 100));
            }

            // Debug diagnostics
            new Thread(() -> DebugRunner.runDiagnostics(LauncherActivity.this), "debug-runner").start();

            setBoot("Listo", 100);
            try { Thread.sleep(350); } catch (InterruptedException ignored) {}

            runOnUiThread(() -> {
                if (prefs.getBoolean(KEY_FIRST_RUN, true)) showWarning();
                else showMainMenu();
            });
        } catch (Throwable t) {
            GladiatorLog.err("Boot", "falló", t);
            runOnUiThread(() -> boot.fail("Error: " + t.getMessage(), "Salir", this::finish));
        }
    }

    private void setBoot(String msg, int pct) {
        GladiatorLog.log("Boot", pct + "% " + msg);
        runOnUiThread(() -> boot.set(msg, pct));
    }

    private void showWarning() {
        showWizardPage(0);
    }

    private void showWizardPage(int step) {
        switch (step) {
            case 0: wizardWarnings(); break;
            case 1: wizardCompat();   break;
            case 2: wizardGpuTest();  break;
            case 3: wizardData();     break;
            case 4: wizardThanks();   break;
        }
    }

    private android.widget.ScrollView scrollable(String txt) {
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setVerticalScrollBarEnabled(true);
        int maxH = (int)(getResources().getDisplayMetrics().heightPixels * 0.6);
        sv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, maxH));
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.addView(Ui.bodyText(this, txt, 13, Ui.TEXT_DIM));
        sv.addView(inner);
        return sv;
    }

    private Ui.GameDialog wizardDialog(String title, int accent) {
        return new Ui.GameDialog(this, title, accent).cancelable(false);
    }

    private void wizardWarnings() {
        String txt =
                "GLADIATOR ES EXPERIMENTAL.\n\n"
              + "ALMACENAMIENTO\n"
              + "El entorno completo ocupa entre 2 y 3 GB. Necesitas ese espacio "
              + "libre en el dispositivo.\n\n"
              + "ESTABILIDAD\n"
              + "El proyecto esta en desarrollo activo. Pueden aparecer cuelgues, "
              + "reinicios inesperados de la sesion o fallos al arrancar el entorno. "
              + "Si la sesion no arranca, forzar cierre de la app y volver a entrar.\n\n"
              + "COMPATIBILIDAD\n"
              + "Solo probado sobre GPU Mali (Mali-G52 MC2). No preparado para Adreno, "
              + "PowerVR ni otras familias. En otro hardware puede no arrancar.";
        wizardDialog("Aviso // Importante", Ui.AMBER)
                .view(scrollable(txt))
                .button("Siguiente", Ui.NeonButton.PRIMARY, d -> {
                    d.dismiss();
                    showWizardPage(1);
                })
                .show();
    }

    private void wizardCompat() {
        String txt =
                "COMPATIBILIDAD OPENGL\n\n"
              + "Gladiator traduce OpenGL de escritorio sobre OpenGL ES del device.\n\n"
              + "Soporta hasta OpenGL 4.3 core. La traduccion puede ser inestable "
              + "en aplicaciones que usen features muy modernas (bindless, sparse, "
              + "clip control). El rendimiento puede no ser optimo comparado con un "
              + "driver nativo.\n\n"
              + "NO se hace emulacion por CPU. Si la GPU del device es compatible "
              + "con la version de OpenGL que la aplicacion pide, la aplicacion se "
              + "ejecuta sobre la GPU. La traduccion es solo de API, no de trabajo.\n\n"
              + "Si la aplicacion pide una version mayor a la que el backend puede "
              + "expresar, Gladiator la rechaza en lugar de dar un resultado incorrecto.";
        wizardDialog("Compatibilidad", Ui.CYAN)
                .view(scrollable(txt))
                .button("Siguiente", Ui.NeonButton.PRIMARY, d -> {
                    d.dismiss();
                    showWizardPage(2);
                })
                .show();
    }

    private void wizardGpuTest() {
        // Queries reales del device: GLES via EGL14, Vulkan via API Java.
        String glesInfo = probeGles();
        String vkInfo   = probeVulkan();

        String txt =
                "TEST DE GPU\n\n"
              + "OpenGL ES del device:\n"
              + glesInfo + "\n\n"
              + "Vulkan del device:\n"
              + vkInfo + "\n\n"
              + "Dentro del entorno, Gladiator reportara:\n"
              + "  OpenGL hasta 4.3 core (via Lorica)\n"
              + "  Vulkan 1.0 / 1.1 / 1.2 (via Spatha)\n\n"
              + "El test completo se corre al arrancar el entorno.";
        wizardDialog("Test de GPU", Ui.GREEN)
                .view(scrollable(txt))
                .button("Siguiente", Ui.NeonButton.PRIMARY, d -> {
                    d.dismiss();
                    showWizardPage(3);
                })
                .show();
    }

    private String probeGles() {
        EGLDisplay dpy = null;
        EGLContext ctx = null;
        EGLSurface surf = null;
        try {
            dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] ver = new int[2];
            EGL14.eglInitialize(dpy, ver, 0, ver, 1);
            int[] attrs = {
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
            };
            EGLConfig[] cfg = new EGLConfig[1];
            int[] n = new int[1];
            EGL14.eglChooseConfig(dpy, attrs, 0, cfg, 0, 1, n, 0);
            int[] cattr = { EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE };
            ctx = EGL14.eglCreateContext(dpy, cfg[0], EGL14.EGL_NO_CONTEXT, cattr, 0);
            int[] sattr = { EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE };
            surf = EGL14.eglCreatePbufferSurface(dpy, cfg[0], sattr, 0);
            EGL14.eglMakeCurrent(dpy, surf, surf, ctx);
            String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            String version = GLES20.glGetString(GLES20.GL_VERSION);
            String exts = GLES20.glGetString(GLES20.GL_EXTENSIONS);
            int nExt = (exts == null) ? 0 : exts.split(" ").length;
            return "  Vendor:   " + (vendor == null ? "-" : vendor)
                 + "\n  Renderer: " + (renderer == null ? "-" : renderer)
                 + "\n  Version:  " + (version == null ? "-" : version)
                 + "\n  Extensions: " + nExt;
        } catch (Throwable t) {
            return "  no disponible (" + t.getMessage() + ")";
        } finally {
            try { if (dpy != null && surf != null) EGL14.eglDestroySurface(dpy, surf); } catch (Throwable ignored) {}
            try { if (dpy != null && ctx != null)  EGL14.eglDestroyContext(dpy, ctx); }  catch (Throwable ignored) {}
            try { if (dpy != null) EGL14.eglTerminate(dpy); } catch (Throwable ignored) {}
        }
    }

    private String probeVulkan() {
        if (android.os.Build.VERSION.SDK_INT < 24) return "  requiere Android 7+";
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            boolean hasVk = pm.hasSystemFeature(
                    android.content.pm.PackageManager.FEATURE_VULKAN_HARDWARE_VERSION);
            if (!hasVk) return "  no soportado por el device";
            int vkVer = 0;
            try {
                java.lang.reflect.Method m = android.os.Build.class
                        .getMethod("getVulkanVersion");
                Object r = m.invoke(null);
                if (r instanceof Integer) vkVer = (Integer) r;
            } catch (Throwable ignored) {}
            if (vkVer == 0) return "  Vulkan soportado (version no consultable)";
            return "  Vulkan " + ((vkVer >> 22) & 0x3FF) + "."
                             + ((vkVer >> 12) & 0x3FF) + "."
                             + (vkVer & 0xFFF);
        } catch (Throwable t) {
            return "  no disponible";
        }
    }

    private void wizardData() {
        String txt =
                "DATOS Y BATERIA\n\n"
              + "DATOS\n"
              + "La primera vez que arranca el container, la app descarga paquetes "
              + "desde internet (Ubuntu base, JWM, XTerm, fuentes y dependencias). "
              + "Eso consume datos moviles si no estas en WiFi.\n\n"
              + "Una vez armado el container, la app funciona sin red.\n\n"
              + "BATERIA\n"
              + "El entorno ejecuta un servidor X, un container y daemons de "
              + "traduccion grafica. El consumo es mayor que una app normal.\n\n"
              + "El primer arranque de cada entorno puede tardar hasta 10 minutos "
              + "y calentar el dispositivo. Se recomienda tenerlo enchufado.\n\n"
              + "La app no tiene publicidad, ni analiticas, ni telemetria.";
        wizardDialog("Datos y bateria", Ui.MAGENTA)
                .view(scrollable(txt))
                .button("Siguiente", Ui.NeonButton.PRIMARY, d -> {
                    d.dismiss();
                    showWizardPage(4);
                })
                .show();
    }

    private void wizardThanks() {
        String txt =
                "Gracias por usar Gladiator.\n\n"
              + "El proyecto es de codigo abierto (GPL-3.0). Todo su codigo, "
              + "incluyendo Scutum, Spatha, Sesar y Lorica, esta disponible en "
              + "GitHub.\n\n"
              + "Cualquier componente puede usarse por separado dentro de Termux "
              + "standalone. No hace falta la app para correr los binarios.\n\n"
              + "github.com/LexusYTG";
        wizardDialog("Bienvenido", Ui.CYAN)
                .view(scrollable(txt))
                .button("Empezar", Ui.NeonButton.PRIMARY, d -> {
                    prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
                    d.dismiss();
                    showMainMenu();
                })
                .show();
    }

    private void showMainMenu() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            try {
                if (!android.os.Environment.isExternalStorageManager()) {
                    Intent i = new Intent(
                            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    i.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            } catch (Throwable ignored) {}
        }

        List<Container> containers = Containers.list(this);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(16), dp(20), dp(14));

        Ui.GlyphView logo = new Ui.GlyphView(this, Ui.G_HEX, Ui.CYAN).setLabel("G").setGlow(true);
        header.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.label(this, "GLADIATOR", 20, Ui.TEXT, true, 0.22f);
        Ui.glow(title, Ui.CYAN, 10);
        titles.addView(title);
        titles.addView(Ui.vspace(this, 5));
        titles.addView(Ui.label(this, "LINUX // X11 // CONTENEDORES", 10, Ui.MUTED, false, 0.25f));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(14);
        header.addView(titles, tlp);
        screen.addView(header);

        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dlp.leftMargin = dp(20);
        dlp.rightMargin = dp(20);
        screen.addView(Ui.divider(this), dlp);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(20), dp(18), dp(20), dp(20));

        LinearLayout sec = new LinearLayout(this);
        sec.setOrientation(LinearLayout.HORIZONTAL);
        sec.setGravity(Gravity.CENTER_VERTICAL);
        sec.addView(Ui.sectionTitle(this, "Contenedores", Ui.CYAN),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sec.addView(Ui.badge(this, String.format(Locale.US, "%02d", containers.size()), Ui.CYAN));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = dp(14);
        list.addView(sec, slp);

        if (containers.isEmpty()) {
            list.addView(emptyState());
        } else {
            int i = 0;
            for (Container c : containers) {
                View card = containerCard(c);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                clp.bottomMargin = dp(10);
                list.addView(card, clp);
                card.setAlpha(0f);
                card.setTranslationY(dp(16));
                card.animate().alpha(1f).translationY(0f)
                        .setStartDelay(Math.min(i, 8) * 50L).setDuration(280)
                        .setInterpolator(new DecelerateInterpolator()).start();
                i++;
            }
        }
        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        screen.addView(Ui.divider(this), new LinearLayout.LayoutParams(dlp));
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(dp(16), dp(12), dp(16), dp(12));
        dock.setBackgroundColor(0xE6060A16);

        View settings = Ui.iconButton(this, Ui.G_GEAR, Ui.CYAN, 48);
        settings.setOnClickListener(x ->
                startActivity(new Intent(LauncherActivity.this, SettingsActivity.class)));
        dock.addView(settings);

        Ui.NeonButton create = new Ui.NeonButton(this, "+  Nuevo contenedor", Ui.NeonButton.PRIMARY);
        create.setOnClickListener(x -> showNewContainerDialog());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        blp.leftMargin = dp(12);
        blp.rightMargin = dp(12);
        dock.addView(create, blp);

        View exit = Ui.iconButton(this, Ui.G_POWER, Ui.MAGENTA, 48);
        exit.setOnClickListener(x -> finish());
        dock.addView(exit);
        screen.addView(dock);

        int screenW = getResources().getDisplayMetrics().widthPixels;
        FrameLayout holder = new FrameLayout(this);
        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                Math.min(screenW, dp(MAX_W_DP)), ViewGroup.LayoutParams.MATCH_PARENT);
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        holder.addView(screen, hlp);
        setContent(holder);
    }

    private View emptyState() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(16), dp(56), dp(16), dp(16));

        Ui.GlyphView hex = new Ui.GlyphView(this, Ui.G_HEX, Ui.MUTED);
        hex.setAlpha(0.7f);
        box.addView(hex, new LinearLayout.LayoutParams(dp(76), dp(76)));
        box.addView(Ui.vspace(this, 18));

        TextView t = Ui.label(this, "SIN CONTENEDORES", 14, Ui.TEXT_DIM, true, 0.2f);
        t.setGravity(Gravity.CENTER);
        box.addView(t);
        box.addView(Ui.vspace(this, 8));

        TextView s = Ui.bodyText(this, "Usá NUEVO CONTENEDOR para crear el primero.", 13, Ui.MUTED);
        s.setGravity(Gravity.CENTER);
        box.addView(s);
        return box;
    }

    private View containerCard(final Container c) {
        final boolean ok = c.isInstalled();
        int accent = ok ? Ui.GREEN : Ui.AMBER;

        Ui.NeonCard row = new Ui.NeonCard(this, accent);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));

        Ui.GlyphView icon = new Ui.GlyphView(this, Ui.G_HEX, accent).setLabel(c.name);
        row.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView n = Ui.label(this, c.name, 16, Ui.TEXT, true, 0.03f);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(n);
        texts.addView(Ui.vspace(this, 7));
        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.addView(Ui.badge(this, ok ? "Listo" : "Pendiente", accent));
        texts.addView(meta);
        LinearLayout.LayoutParams txlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        txlp.leftMargin = dp(14);
        row.addView(texts, txlp);

        View trash = Ui.iconButton(this, Ui.G_TRASH, Ui.RED, 40);
        trash.setOnClickListener(x -> confirmDelete(c));
        LinearLayout.LayoutParams trlp = new LinearLayout.LayoutParams(dp(40), dp(40));
        trlp.leftMargin = dp(8);
        row.addView(trash, trlp);

        View play = Ui.iconButton(this, Ui.G_PLAY, accent, 40);
        play.setOnClickListener(x -> launch(c));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(40), dp(40));
        plp.leftMargin = dp(8);
        row.addView(play, plp);

        row.setOnClickListener(x -> launch(c));
        row.setOnLongClickListener(x -> { confirmDelete(c); return true; });
        return row;
    }

    private void launch(Container c) {
        Intent i = new Intent(LauncherActivity.this, SessionActivity.class);
        i.putExtra("container", c.name);
        startActivity(i);
    }

    private void confirmDelete(final Container c) {
        new Ui.GameDialog(this, "Borrar contenedor", Ui.RED)
                .message("¿Borrar \"" + c.name + "\"? Esto elimina todos sus datos.")
                .button("Cancelar", Ui.NeonButton.SECONDARY, null)
                .button("Borrar", Ui.NeonButton.DANGER, d -> {
                    d.dismiss();
                    deleteContainer(c);
                })
                .show();
    }

    private void deleteContainer(final Container c) {
        final Ui.GameDialog busy = new Ui.GameDialog(this, "Borrando", Ui.RED)
                .message("Eliminando \"" + c.name + "\"…")
                .cancelable(false);
        busy.show();
        new Thread(() -> {
            deleteRecursive(c.dir);
            runOnUiThread(() -> { busy.dismiss(); showMainMenu(); });
        }, "delete-container").start();
    }

    private void showNewContainerDialog() {
        final EditText input = Ui.input(this, "nombre (ej: ubuntu-focal)");
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        final TextView err = Ui.label(this, "", 11, Ui.RED, false, 0.03f);
        err.setVisibility(View.GONE);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(Ui.label(this, "NOMBRE", 10, Ui.MUTED, true, 0.2f));
        body.addView(Ui.vspace(this, 8));
        body.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.topMargin = dp(8);
        body.addView(err, elp);

        final Ui.GameDialog dlg = new Ui.GameDialog(this, "Nuevo contenedor", Ui.CYAN);
        final Ui.Action create = d -> {
            String s = Containers.sanitize(input.getText().toString());
            if (s == null) {
                err.setText("Nombre inválido.");
                err.setVisibility(View.VISIBLE);
                return;
            }
            if (Containers.nameExists(this, s)) {
                err.setText("Ya existe un contenedor llamado \"" + s + "\".");
                err.setVisibility(View.VISIBLE);
                return;
            }
            d.dismiss();
            Intent i = new Intent(this, SessionActivity.class);
            i.putExtra("container", s);
            startActivity(i);
        };
        input.setOnEditorActionListener((v, actionId, e) -> { create.run(dlg); return true; });

        dlg.view(body).keyboard()
                .button("Cancelar", Ui.NeonButton.SECONDARY, null)
                .button("Crear", Ui.NeonButton.PRIMARY, create)
                .show();
        input.requestFocus();
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        f.delete();
    }

    private int dp(float v) { return Ui.dp(this, v); }
}
