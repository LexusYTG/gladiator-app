package com.gladiator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ControlEditorActivity extends Activity {
    private String containerName;
    private List<ControlConfig> controls;
    private EditorCanvas canvas;
    private int inputMode = 0;   // 0=botones volumen, 1=gestos

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.styleWindow(this);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        containerName = getSharedPreferences("gladiator", MODE_PRIVATE)
                .getString("last_container", "default");
        controls = ControlConfigStore.load(this, containerName);
        inputMode = new Prefs(this).inputMode();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0A0E1A);

        canvas = new EditorCanvas(this);
        root.addView(canvas, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(10), dp(12), dp(10));
        header.setBackgroundColor(0xCC04060D);

        View back = Ui.iconButton(this, Ui.G_BACK, Ui.CYAN, 40);
        back.setOnClickListener(v -> confirmExit());
        header.addView(back);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.label(this, "EDITOR DE CONTROLES", 15, Ui.TEXT, true, 0.2f);
        Ui.glow(t, Ui.CYAN, 8);
        titles.addView(t);
        titles.addView(Ui.vspace(this, 4));
        titles.addView(Ui.label(this, "TAP edita · DRAG mueve · HANDLE naranja redimensiona", 9, Ui.MUTED, false, 0.1f));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(10);
        header.addView(titles, tlp);

        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.gravity = Gravity.TOP;
        root.addView(header, hlp);

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setPadding(dp(12), dp(10), dp(12), dp(10));
        dock.setBackgroundColor(0xCC04060D);

        Ui.NeonButton addBtn = new Ui.NeonButton(this, "+ Botón", Ui.NeonButton.PRIMARY);
        addBtn.setOnClickListener(v -> addControl(ControlConfig.TYPE_BUTTON));
        dock.addView(addBtn, weightLp());

        Ui.NeonButton addDpad = new Ui.NeonButton(this, "+ Cruceta", Ui.NeonButton.SECONDARY);
        addDpad.setOnClickListener(v -> addControl(ControlConfig.TYPE_DPAD));
        dock.addView(addDpad, weightLp());

        Ui.NeonButton addStick = new Ui.NeonButton(this, "+ Joystick", Ui.NeonButton.SECONDARY);
        addStick.setOnClickListener(v -> addControl(ControlConfig.TYPE_JOYSTICK));
        dock.addView(addStick, weightLp());

        Ui.NeonButton addFire = new Ui.NeonButton(this, "+ Disparo", Ui.NeonButton.SECONDARY);
        addFire.setOnClickListener(v -> addControl(ControlConfig.TYPE_FIRE));
        dock.addView(addFire, weightLp());

        // La rueda de scroll solo existe en modo "Botones volumen".
        if (inputMode == 0) {
            Ui.NeonButton addScroll = new Ui.NeonButton(this, "+ Scroll", Ui.NeonButton.SECONDARY);
            addScroll.setOnClickListener(v -> addControl(ControlConfig.TYPE_SCROLL));
            dock.addView(addScroll, weightLp());
        }

        Ui.NeonButton saveBtn = new Ui.NeonButton(this, "Guardar", Ui.NeonButton.PRIMARY);
        saveBtn.setOnClickListener(v -> saveAndExit());
        dock.addView(saveBtn, weightLp());

        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.gravity = Gravity.BOTTOM;
        root.addView(dock, dlp);

        setContentView(root);
        canvas.invalidate();
    }

    /** Un control de scroll guardado no se muestra ni se edita en modo gestos (se conserva en disco). */
    private boolean editable(ControlConfig c) {
        return c.type != ControlConfig.TYPE_SCROLL || inputMode == 0;
    }

    private String nameOf(ControlConfig c) {
        if (c.label != null && !c.label.isEmpty()) return c.label;
        if (c.type == ControlConfig.TYPE_FIRE) return "Disparo";
        if (c.type == ControlConfig.TYPE_SCROLL) return "Scroll";
        return "control";
    }

    private LinearLayout.LayoutParams weightLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        return lp;
    }

    @Override
    public void onBackPressed() { confirmExit(); }

    private void confirmExit() {
        new Ui.GameDialog(this, "Salir", Ui.AMBER)
                .message("¿Guardar los cambios?")
                .button("Descartar", Ui.NeonButton.SECONDARY, d -> { d.dismiss(); finish(); })
                .button("Guardar", Ui.NeonButton.PRIMARY, d -> { d.dismiss(); saveAndExit(); })
                .show();
    }

    private void saveAndExit() {
        ControlConfigStore.save(this, containerName, controls);
        finish();
    }

    private void addControl(int type) {
        ControlConfig c = new ControlConfig();
        c.id = UUID.randomUUID().toString();
        c.type = type;
        c.shape = ControlConfig.SHAPE_CIRCLE;
        c.cx = 0.3f + (float)Math.random() * 0.4f;
        c.cy = 0.4f + (float)Math.random() * 0.3f;
        c.sizeFrac = type == ControlConfig.TYPE_BUTTON ? 0.14f : 0.28f;
        if (type == ControlConfig.TYPE_BUTTON) {
            c.label = "A";
            c.keyCode = android.view.KeyEvent.KEYCODE_A;
        } else if (type == ControlConfig.TYPE_FIRE) {
            c.label = "";
            c.sizeFrac = 0.22f;
            c.sens = ControlConfig.DEFAULT_SENS;
        } else if (type == ControlConfig.TYPE_SCROLL) {
            c.label = "";
            c.sizeFrac = 0.24f;
        } else {
            c.label = "STICK";
            c.keyUp = android.view.KeyEvent.KEYCODE_W;
            c.keyDown = android.view.KeyEvent.KEYCODE_S;
            c.keyLeft = android.view.KeyEvent.KEYCODE_A;
            c.keyRight = android.view.KeyEvent.KEYCODE_D;
        }
        controls.add(c);
        canvas.invalidate();
        new Handler(Looper.getMainLooper()).postDelayed(() -> showEditDialog(c), 100);
    }

    private void showEditDialog(final ControlConfig c) {
        final EditText labelInput = Ui.input(this, "etiqueta");
        labelInput.setText(c.label);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        final boolean keyed = c.type == ControlConfig.TYPE_BUTTON
                || c.type == ControlConfig.TYPE_DPAD
                || c.type == ControlConfig.TYPE_JOYSTICK;
        if (keyed) {
            body.addView(Ui.label(this, "ETIQUETA", 10, Ui.MUTED, true, 0.2f));
            body.addView(Ui.vspace(this, 6));
            body.addView(labelInput);
            body.addView(Ui.vspace(this, 14));
        }

        Spinner shapeSpin = null;
        if (c.type == ControlConfig.TYPE_BUTTON) {
            final String[] SHAPES = {"Cuadrado", "Círculo", "Triángulo"};
            body.addView(Ui.label(this, "FORMA", 10, Ui.MUTED, true, 0.2f));
            body.addView(Ui.vspace(this, 6));
            shapeSpin = makeSpinner(SHAPES);
            shapeSpin.setSelection(c.shape);
            body.addView(shapeSpin);
            body.addView(Ui.vspace(this, 14));
        }

        final String[] SIZES = {"Pequeño", "Mediano", "Grande", "Enorme"};
        final float[] SIZES_FRAC = {0.10f, 0.15f, 0.22f, 0.30f};
        int sizeIdx = 1;
        float best = 999f;
        for (int i = 0; i < SIZES_FRAC.length; i++) {
            float d = Math.abs(SIZES_FRAC[i] - c.sizeFrac);
            if (d < best) { best = d; sizeIdx = i; }
        }
        body.addView(Ui.label(this, "TAMAÑO", 10, Ui.MUTED, true, 0.2f));
        body.addView(Ui.vspace(this, 6));
        final Spinner sizeSpin = makeSpinner(SIZES);
        sizeSpin.setSelection(sizeIdx);
        body.addView(sizeSpin);
        body.addView(Ui.vspace(this, 14));

        final float[] SENS_VALS = {0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f};
        final String[] SENS_LABELS = {"0.5×", "0.75×", "1×", "1.5× (cámara)", "2×", "2.5×", "3×", "4×"};
        Spinner sensTmp = null;
        if (c.type == ControlConfig.TYPE_FIRE) {
            int sensIdx = 3;
            float bestS = 999f;
            for (int i = 0; i < SENS_VALS.length; i++) {
                float d = Math.abs(SENS_VALS[i] - c.sens);
                if (d < bestS) { bestS = d; sensIdx = i; }
            }
            body.addView(Ui.label(this, "SENSIBILIDAD", 10, Ui.MUTED, true, 0.2f));
            body.addView(Ui.vspace(this, 6));
            sensTmp = makeSpinner(SENS_LABELS);
            sensTmp.setSelection(sensIdx);
            body.addView(sensTmp);
            body.addView(Ui.vspace(this, 14));
        }
        final Spinner sensSpin = sensTmp;

        final List<Spinner> keySpinners = new ArrayList<>();
        final List<String> keyLabels = KeyMap.labels();
        String[] keyArr = keyLabels.toArray(new String[0]);

        if (c.type == ControlConfig.TYPE_BUTTON) {
            body.addView(Ui.label(this, "TECLA", 10, Ui.MUTED, true, 0.2f));
            body.addView(Ui.vspace(this, 6));
            Spinner ks = makeSpinner(keyArr);
            int idx = keyLabels.indexOf(KeyMap.labelFor(c.keyCode));
            if (idx >= 0) ks.setSelection(idx);
            body.addView(ks);
            keySpinners.add(ks);
        } else if (c.type == ControlConfig.TYPE_DPAD || c.type == ControlConfig.TYPE_JOYSTICK) {
            String[][] dirs = {
                    {"ARRIBA", KeyMap.labelFor(c.keyUp)},
                    {"ABAJO", KeyMap.labelFor(c.keyDown)},
                    {"IZQUIERDA", KeyMap.labelFor(c.keyLeft)},
                    {"DERECHA", KeyMap.labelFor(c.keyRight)}
            };
            for (String[] d : dirs) {
                body.addView(Ui.label(this, d[0], 10, Ui.MUTED, true, 0.2f));
                body.addView(Ui.vspace(this, 6));
                Spinner ks = makeSpinner(keyArr);
                int idx = keyLabels.indexOf(d[1]);
                if (idx >= 0) ks.setSelection(idx);
                body.addView(ks);
                body.addView(Ui.vspace(this, 10));
                keySpinners.add(ks);
            }
        }

        final Spinner finalShapeSpin = shapeSpin;
        new Ui.GameDialog(this, "Configurar control", Ui.CYAN)
                .view(body)
                .button("Borrar", Ui.NeonButton.DANGER, d -> {
                    controls.remove(c);
                    d.dismiss();
                    canvas.invalidate();
                })
                .button("Guardar", Ui.NeonButton.PRIMARY, d -> {
                    if (keyed) c.label = labelInput.getText().toString();
                    if (finalShapeSpin != null) c.shape = finalShapeSpin.getSelectedItemPosition();
                    c.sizeFrac = SIZES_FRAC[sizeSpin.getSelectedItemPosition()];
                    if (sensSpin != null) c.sens = SENS_VALS[sensSpin.getSelectedItemPosition()];
                    if (c.type == ControlConfig.TYPE_BUTTON) {
                        c.keyCode = KeyMap.codeFor((String) keySpinners.get(0).getSelectedItem());
                    } else if (keyed) {
                        c.keyUp    = KeyMap.codeFor((String) keySpinners.get(0).getSelectedItem());
                        c.keyDown  = KeyMap.codeFor((String) keySpinners.get(1).getSelectedItem());
                        c.keyLeft  = KeyMap.codeFor((String) keySpinners.get(2).getSelectedItem());
                        c.keyRight = KeyMap.codeFor((String) keySpinners.get(3).getSelectedItem());
                    }
                    d.dismiss();
                    canvas.invalidate();
                })
                .show();
    }

    private Spinner makeSpinner(String[] items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        return s;
    }

    private int dp(float v) { return Ui.dp(this, v); }

    private class EditorCanvas extends View {
        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint strokeThin = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint handleFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint handleStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint accent = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final RectF rect = new RectF();

        ControlConfig dragging = null;
        int dragPointerId = -1;
        float dragStartX, dragStartY;
        float dragOrigCx, dragOrigCy, dragOrigSize;
        boolean isResizing = false;

        ControlConfig longPressTarget = null;
        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable longPressRunnable;

        EditorCanvas(Context c) {
            super(c);
            setFocusable(true);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(3f);
            stroke.setColor(0xFF00E5FF);
            strokeThin.setStyle(Paint.Style.STROKE);
            strokeThin.setStrokeWidth(2f);
            strokeThin.setColor(0xFF00E5FF);
            fill.setColor(0x55203A60);
            accent.setColor(0xCC00E5FF);
            text.setColor(0xFFE8F4FF);
            text.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            text.setTextAlign(Paint.Align.CENTER);
            handleFill.setColor(0xFFFFB020);
            handleStroke.setStyle(Paint.Style.STROKE);
            handleStroke.setStrokeWidth(2f);
            handleStroke.setColor(0xFF04060D);
        }

        private float minDim() { return Math.min(getWidth(), getHeight()); }

        private RectF rectOf(ControlConfig c) {
            float size = c.sizeFrac * minDim();
            float wdt = size * c.aspect();
            float cx = c.cx * getWidth();
            float cy = c.cy * getHeight();
            return new RectF(cx - wdt/2, cy - size/2, cx + wdt/2, cy + size/2);
        }

        private RectF resizeHandleOf(ControlConfig c) {
            RectF r = rectOf(c);
            float hs = Math.max(28f, minDim() * 0.06f);
            return new RectF(r.right - hs/2, r.bottom - hs/2, r.right + hs/2, r.bottom + hs/2);
        }

        @Override
        protected void onDraw(Canvas cv) {
            super.onDraw(cv);
            for (ControlConfig c : controls) if (editable(c)) drawControl(cv, c);
        }

        private void drawControl(Canvas cv, ControlConfig c) {
            RectF r = rectOf(c);
            boolean selected = (c == dragging);

            fill.setColor(selected ? 0x8800E5FF : 0x55203A60);
            stroke.setColor(selected ? 0xFFFFB020 : 0xFF00E5FF);

            switch (c.type) {
                case ControlConfig.TYPE_BUTTON:
                    drawShapeFor(cv, r, c.shape);
                    text.setTextSize(r.height() * 0.36f);
                    text.setColor(selected ? 0xFF04060D : 0xFFE8F4FF);
                    float ty = r.centerY() - (text.ascent() + text.descent()) / 2f;
                    cv.drawText(c.label != null ? c.label : "?", r.centerX(), ty, text);
                    break;
                case ControlConfig.TYPE_DPAD:
                    drawDpadEditor(cv, r, c);
                    break;
                case ControlConfig.TYPE_JOYSTICK:
                    drawStickEditor(cv, r);
                    break;
                case ControlConfig.TYPE_FIRE:
                    ControlShapes.drawBullet(cv, r, path, fill, stroke, accent, selected);
                    break;
                case ControlConfig.TYPE_SCROLL:
                    ControlShapes.drawScroll(cv, r, path, fill, stroke, accent, 0f, 0f, selected);
                    break;
            }

            text.setTextSize(Math.max(10f, r.height() * 0.12f));
            text.setColor(0xFFB4C4E0);
            String info;
            if (c.type == ControlConfig.TYPE_BUTTON) {
                info = KeyMap.labelFor(c.keyCode);
            } else if (c.type == ControlConfig.TYPE_FIRE) {
                info = "DISPARO · " + c.sens + "×";
            } else if (c.type == ControlConfig.TYPE_SCROLL) {
                info = "SCROLL";
            } else {
                info = KeyMap.labelFor(c.keyUp) + "/" + KeyMap.labelFor(c.keyDown)
                        + "/" + KeyMap.labelFor(c.keyLeft) + "/" + KeyMap.labelFor(c.keyRight);
            }
            cv.drawText(info, r.centerX(), r.top - 8f, text);

            RectF h = resizeHandleOf(c);
            cv.drawRoundRect(h, 4, 4, handleFill);
            cv.drawRoundRect(h, 4, 4, handleStroke);
        }

        private void drawShapeFor(Canvas cv, RectF r, int shape) {
            switch (shape) {
                case ControlConfig.SHAPE_SQUARE: {
                    float rad = r.height() * 0.15f;
                    cv.drawRoundRect(r, rad, rad, fill);
                    cv.drawRoundRect(r, rad, rad, stroke);
                    break;
                }
                case ControlConfig.SHAPE_CIRCLE:
                    cv.drawCircle(r.centerX(), r.centerY(), r.width() / 2f - 1, fill);
                    cv.drawCircle(r.centerX(), r.centerY(), r.width() / 2f - 1, stroke);
                    break;
                case ControlConfig.SHAPE_TRIANGLE:
                    path.reset();
                    path.moveTo(r.centerX(), r.top + 1);
                    path.lineTo(r.right - 1, r.bottom - 1);
                    path.lineTo(r.left + 1, r.bottom - 1);
                    path.close();
                    cv.drawPath(path, fill);
                    cv.drawPath(path, stroke);
                    break;
            }
        }

        private void drawDpadEditor(Canvas cv, RectF r, ControlConfig c) {
            float cw = r.width() / 3f;
            float ch = r.height() / 3f;
            cv.drawRoundRect(r, r.height() * 0.12f, r.height() * 0.12f, fill);
            cv.drawRoundRect(r, r.height() * 0.12f, r.height() * 0.12f, stroke);

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            p.setColor(0xFF00E5FF);

            cv.drawRect(r.left + cw, r.top, r.left + cw * 2, r.top + ch, p);
            cv.drawRect(r.left + cw, r.top + ch * 2, r.left + cw * 2, r.bottom, p);
            cv.drawRect(r.left, r.top + ch, r.left + cw, r.top + ch * 2, p);
            cv.drawRect(r.left + cw * 2, r.top + ch, r.right, r.top + ch * 2, p);

            text.setTextSize(ch * 0.22f);
            text.setColor(0xFFE8F4FF);
            cv.drawText(KeyMap.labelFor(c.keyUp), r.left + cw * 1.5f, r.top + ch * 0.65f, text);
            cv.drawText(KeyMap.labelFor(c.keyDown), r.left + cw * 1.5f, r.top + ch * 2.65f, text);
            cv.drawText(KeyMap.labelFor(c.keyLeft), r.left + cw * 0.5f, r.top + ch * 1.65f, text);
            cv.drawText(KeyMap.labelFor(c.keyRight), r.left + cw * 2.5f, r.top + ch * 1.65f, text);
        }

        private void drawStickEditor(Canvas cv, RectF r) {
            cv.drawCircle(r.centerX(), r.centerY(), r.width()/2f - 1, fill);
            cv.drawCircle(r.centerX(), r.centerY(), r.width()/2f - 1, stroke);

            strokeThin.setAlpha(80);
            cv.drawLine(r.left + r.width() * 0.1f, r.centerY(), r.right - r.width() * 0.1f, r.centerY(), strokeThin);
            cv.drawLine(r.centerX(), r.top + r.height() * 0.1f, r.centerX(), r.bottom - r.height() * 0.1f, strokeThin);
            strokeThin.setAlpha(255);

            Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
            inner.setColor(0xCC00E5FF);
            cv.drawCircle(r.centerX(), r.centerY(), r.width() * 0.25f, inner);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int action = e.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    for (int i = controls.size() - 1; i >= 0; i--) {
                        ControlConfig c = controls.get(i);
                        if (!editable(c)) continue;
                        if (resizeHandleOf(c).contains(e.getX(), e.getY())) {
                            dragging = c;
                            isResizing = true;
                            dragPointerId = e.getPointerId(0);
                            dragStartX = e.getX();
                            dragStartY = e.getY();
                            dragOrigSize = c.sizeFrac;
                            invalidate();
                            return true;
                        }
                    }
                    for (int i = controls.size() - 1; i >= 0; i--) {
                        ControlConfig c = controls.get(i);
                        if (!editable(c)) continue;
                        if (rectOf(c).contains(e.getX(), e.getY())) {
                            dragging = c;
                            isResizing = false;
                            dragPointerId = e.getPointerId(0);
                            dragStartX = e.getX();
                            dragStartY = e.getY();
                            dragOrigCx = c.cx;
                            dragOrigCy = c.cy;

                            longPressTarget = c;
                            longPressRunnable = () -> {
                                if (longPressTarget == c && dragging == c) {
                                    new Ui.GameDialog(ControlEditorActivity.this, "Borrar control", Ui.RED)
                                            .message("¿Borrar \"" + nameOf(c) + "\"?")
                                            .button("Cancelar", Ui.NeonButton.SECONDARY, null)
                                            .button("Borrar", Ui.NeonButton.DANGER, d -> {
                                                d.dismiss();
                                                controls.remove(c);
                                                dragging = null;
                                                longPressTarget = null;
                                                invalidate();
                                            })
                                            .show();
                                }
                            };
                            handler.postDelayed(longPressRunnable, 550);
                            invalidate();
                            return true;
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (dragging == null) return true;
                    float dx = e.getX() - dragStartX;
                    float dy = e.getY() - dragStartY;
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        if (longPressRunnable != null) {
                            handler.removeCallbacks(longPressRunnable);
                            longPressTarget = null;
                        }
                    }
                    if (isResizing) {
                        float md = minDim();
                        float delta = Math.max(dx, dy) / md;
                        dragging.sizeFrac = Math.max(0.06f, Math.min(0.6f, dragOrigSize + delta));
                    } else {
                        dragging.cx = Math.max(0.02f, Math.min(0.98f, dragOrigCx + dx / getWidth()));
                        dragging.cy = Math.max(0.02f, Math.min(0.98f, dragOrigCy + dy / getHeight()));
                    }
                    invalidate();
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (longPressRunnable != null) {
                        handler.removeCallbacks(longPressRunnable);
                        longPressRunnable = null;
                    }
                    boolean wasTap = dragging != null && longPressTarget != null
                            && Math.abs(e.getX() - dragStartX) < 12
                            && Math.abs(e.getY() - dragStartY) < 12;
                    ControlConfig tapped = dragging;
                    dragging = null;
                    longPressTarget = null;
                    dragPointerId = -1;
                    invalidate();
                    if (wasTap && tapped != null) showEditDialog(tapped);
                    return true;
                }
            }
            return super.onTouchEvent(e);
        }
    }
}
