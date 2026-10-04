package com.gladiator;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.termux.x11.CmdEntryPoint;
import com.termux.x11.ICmdEntryInterface;
import com.termux.x11.LorieView;

import java.util.List;

public class SessionActivity extends Activity {
    private Ui.BootPanel bootPanel;
    private Ui.GridBackground bgView;
    private LorieView lorieView;
    private GamepadOverlay overlay;
    private FloatingButton fab;
    private CustomKeyboard keyboard;
    private Container container;
    private int xw = 1280, xh = 720;
    private volatile boolean xserverConnected = false;

    private volatile boolean volUpHeld = false;
    private volatile boolean volDownHeld = false;
    private android.widget.TextView netIndicator;
    private Handler netHandler;
    private Runnable netRunnable;

    private final BroadcastReceiver startReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!CmdEntryPoint.ACTION_START.equals(intent.getAction())) return;
            if (xserverConnected) return;
            Bundle bundle = intent.getBundleExtra(null);
            IBinder binder = bundle == null ? null : bundle.getBinder(null);
            if (binder == null) return;
            GladiatorLog.log("Session", "binder recibido");
            try {
                ICmdEntryInterface svc = ICmdEntryInterface.Stub.asInterface(binder);
                ParcelFileDescriptor fd = svc.getXConnection();
                if (fd != null) {
                    lorieView.connect(fd.detachFd());
                    xserverConnected = true;
                    GladiatorLog.log("Session", "LorieView conectado");
                    runOnUiThread(() -> {
                        // bootPanel/bgView siguen visibles hasta DONE
                        lorieView.setVisibility(View.VISIBLE);
                        lorieView.requestFocus();
                    });
                }
            } catch (Throwable t) {
                GladiatorLog.err("Session", "conexión falló", t);
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String cname = getIntent().getStringExtra("container");
        if (cname == null) cname = "ubuntu";
        container = Container.forName(this, cname);
        GladiatorLog.log("Session", "onCreate container=" + cname);
        getSharedPreferences("gladiator", MODE_PRIVATE).edit()
                .putString("last_container", cname).apply();

        Prefs p = new Prefs(this);
        String[] res = p.resolution().split("x");
        if (res.length == 2) {
            try {
                xw = Integer.parseInt(res[0]);
                xh = Integer.parseInt(res[1]);
            } catch (Exception ignored) {}
        }

        Ui.styleWindow(this);

        FrameLayout rootView = new FrameLayout(this);
        rootView.setBackgroundColor(Ui.BG);

        bgView = new Ui.GridBackground(this);
        rootView.addView(bgView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bootPanel = new Ui.BootPanel(this, "CONTENEDOR // " + cname);
        bootPanel.set("Preparando…", 0);
        rootView.addView(bootPanel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        lorieView = new LorieView(this, null);
        lorieView.setVisibility(View.GONE);
        lorieView.setFocusable(true);
        lorieView.setFocusableInTouchMode(true);
        lorieView.setZOrderMediaOverlay(true);
        installTouchMouseHandler(lorieView);
        rootView.addView(lorieView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlay = new GamepadOverlay(this, lorieView);
        List<ControlConfig> controls = ControlConfigStore.load(this, cname);
        overlay.load(controls);
        overlay.setVisibility(View.GONE);
        rootView.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        keyboard = new CustomKeyboard(this, lorieView);
        keyboard.setVisibility(View.GONE);
        FrameLayout.LayoutParams klp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        klp.gravity = android.view.Gravity.BOTTOM;
        rootView.addView(keyboard, klp);

        netIndicator = new android.widget.TextView(this);
        netIndicator.setTextSize(9f);
        netIndicator.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        netIndicator.setLetterSpacing(0.15f);
        netIndicator.setPadding(20, 8, 20, 8);
        netIndicator.setBackgroundColor(0xCC04060D);
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        nlp.topMargin = 8;
        nlp.rightMargin = 8;
        netIndicator.setLayoutParams(nlp);
        rootView.addView(netIndicator);

        fab = new FloatingButton(this);
        fab.setInitialFraction(0.5f, 0.06f);
        fab.setListener(this::showFloatingMenu);
        fab.setVisibility(View.GONE);
        rootView.addView(fab, new FrameLayout.LayoutParams(0, 0));
        rootView.addOnLayoutChangeListener((vv, ll, tt, rr, bb, o1, o2, o3, o4) -> fab.applyPosition());

        bootPanel.bringToFront();

        setContentView(rootView);

        IntentFilter filter = new IntentFilter(CmdEntryPoint.ACTION_START);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(startReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(startReceiver, filter);
        }

        startNetMonitor();

        new Thread(this::boot, "session-boot").start();
    }

    private void startNetMonitor() {
        netHandler = new Handler(Looper.getMainLooper());
        netRunnable = new Runnable() {
            @Override public void run() {
                updateNetIndicator();
                if (netHandler != null && netRunnable != null)
                    netHandler.postDelayed(netRunnable, 3000);
            }
        };
        netHandler.post(netRunnable);
    }

    private void updateNetIndicator() {
        if (netIndicator == null) return;
        boolean online = false;
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                android.net.Network n = cm.getActiveNetwork();
                if (n != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(n);
                    if (caps != null) {
                        online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                              && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    }
                }
            }
        } catch (Throwable ignored) {}
        netIndicator.setText(online ? "INTERNET // ON" : "INTERNET // OFF");
        netIndicator.setTextColor(online ? 0xFF00E5FF : 0xFFFF3B5C);
    }

    private void stopNetMonitor() {
        if (netHandler != null && netRunnable != null)
            netHandler.removeCallbacks(netRunnable);
    }

    @Override
    public void onBackPressed() {
        // No cerrar la actividad: abrir el menu flotante (controles, teclado, salir).
        if (fab != null) {
            showFloatingMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Ui.styleWindow(this);
    }

    @Override
    protected void onDestroy() {
        stopNetMonitor();
        try { unregisterReceiver(startReceiver); } catch (Exception ignored) {}
        try { stopService(new Intent(this, XServerService.class)); } catch (Throwable ignored) {}
        super.onDestroy();
    }

    private void installTouchMouseHandler(final LorieView v) {
        final float SENS = 1.5f;
        final float TAP_THRESHOLD = 12f;
        final float SCROLL_STEP = 20f;

        v.setOnTouchListener(new View.OnTouchListener() {
            float lastX, lastY, downX, downY;
            float scrollAcc = 0;
            boolean moved = false;
            boolean multiTouch = false;
            boolean cursorInited = false;

            @Override public boolean onTouch(View view, MotionEvent e) {
                int n = e.getPointerCount();
                int action = e.getActionMasked();

                if (!cursorInited && xw > 0 && xh > 0) {
                    v.sendMouseEvent(xw / 2f, xh / 2f, 0, false, false);
                    cursorInited = true;
                }

                if (n >= 2 || multiTouch) {
                    if (action == MotionEvent.ACTION_POINTER_DOWN) {
                        multiTouch = true;
                        lastY = e.getY(0);
                        scrollAcc = 0;
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        float dy = e.getY(0) - lastY;
                        lastY = e.getY(0);
                        scrollAcc += dy;
                        while (Math.abs(scrollAcc) >= SCROLL_STEP) {
                            int sign = scrollAcc > 0 ? -120 : 120;
                            v.sendMouseWheelEvent(0, sign);
                            scrollAcc += (scrollAcc > 0 ? -SCROLL_STEP : SCROLL_STEP);
                        }
                    } else if (action == MotionEvent.ACTION_POINTER_UP
                            || action == MotionEvent.ACTION_UP
                            || action == MotionEvent.ACTION_CANCEL) {
                        multiTouch = false;
                        scrollAcc = 0;
                    }
                    return true;
                }

                float x = e.getX(0);
                float y = e.getY(0);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = x; lastY = y;
                        downX = x; downY = y;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float dx = x - lastX;
                        float dy = y - lastY;
                        lastX = x; lastY = y;
                        if (Math.abs(x - downX) > TAP_THRESHOLD
                                || Math.abs(y - downY) > TAP_THRESHOLD) moved = true;
                        v.sendMouseEvent(dx * SENS, dy * SENS, 0, false, true);
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!moved && !volUpHeld && !volDownHeld) {
                            v.sendMouseEvent(0f, 0f, 1, true, true);
                            v.sendMouseEvent(0f, 0f, 1, false, true);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (event.getRepeatCount() > 0) return true;
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            if (!volUpHeld) { volUpHeld = true; sendMouseButton(1, true); }
            return true;
        }
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!volDownHeld) { volDownHeld = true; sendMouseButton(3, true); }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            if (volUpHeld) { volUpHeld = false; sendMouseButton(1, false); }
            return true;
        }
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volDownHeld) { volDownHeld = false; sendMouseButton(3, false); }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void sendMouseButton(int button, boolean down) {
        if (lorieView == null) return;
        lorieView.sendMouseEvent(0f, 0f, button, down, true);
    }

    private void showFloatingMenu() {
        final boolean controlsVisible = overlay != null && overlay.getVisibility() == View.VISIBLE;
        final boolean keyboardVisible = keyboard != null && keyboard.getVisibility() == View.VISIBLE;
        new Ui.GameDialog(this, "Opciones", Ui.CYAN)
                .button(controlsVisible ? "Ocultar controles" : "Mostrar controles",
                        Ui.NeonButton.PRIMARY,
                        d -> { d.dismiss(); if (overlay != null)
                                overlay.setVisibility(controlsVisible ? View.GONE : View.VISIBLE); })
                .button(keyboardVisible ? "Ocultar teclado" : "Mostrar teclado",
                        Ui.NeonButton.SECONDARY,
                        d -> { d.dismiss(); if (keyboard != null)
                                keyboard.setVisibility(keyboardVisible ? View.GONE : View.VISIBLE); })
                .button("Salir del contenedor", Ui.NeonButton.DANGER,
                        d -> { d.dismiss(); finish(); })
                .show();
    }

    private void boot() {
        try {
            // 1. Bootstrap
            BootstrapInstaller.installIfNeeded(this, (msg, pct) -> setBoot(msg, pct * 25 / 100));

            // 2. X server
            setBoot("Arrancando servidor X…", 30);
            try { stopService(new Intent(this, XServerService.class)); } catch (Throwable ignored) {}
            Thread.sleep(250);
            Intent svc = new Intent(this, XServerService.class);
            svc.putExtra("container", container.name);
            startService(svc);
            Thread.sleep(2500);

            // 3. LorieView
            setBoot("Conectando display…", 35);
            runOnUiThread(() -> lorieView.requestConnection());
            Thread.sleep(2000);

            // 4. session-bootstrap.sh — prepara + lanza
            setBoot("Iniciando contenedor " + container.name + "…", 40);
            SessionLauncher.start(this, container, new SessionLauncher.Progress() {
                @Override public void onProgress(String msg, int pct) {
                    setBoot(msg, 40 + pct * 60 / 100);
                }
                @Override public void onDone() {
                    onSessionDone();
                }
                @Override public void onError(String msg) {
                    setBoot("Error: " + msg, 100);
                }
            });
        } catch (Throwable t) {
            GladiatorLog.err("Session", "Fatal", t);
            setBoot("Error: " + t.getMessage(), 100);
        }
    }

    private void onSessionDone() {
        GladiatorLog.log("Session", "DONE -> ocultando pantalla de carga");
        if (bootPanel != null) bootPanel.stopElapsedTimer();
        runOnUiThread(() -> {
            bootPanel.animate().alpha(0f).setDuration(350).withEndAction(() -> {
                bootPanel.setVisibility(View.GONE);
                bgView.setVisibility(View.GONE);
                overlay.setVisibility(View.VISIBLE);
                fab.setVisibility(View.VISIBLE);
                lorieView.requestFocus();
            }).start();
        });
    }

    private void setBoot(String msg, int pct) {
        GladiatorLog.log("Session", pct + "% " + msg);
        runOnUiThread(() -> {
            if (msg.startsWith("Error")) bootPanel.fail(msg, "Volver", this::finish);
            else bootPanel.set(msg, pct);
        });
    }
}
