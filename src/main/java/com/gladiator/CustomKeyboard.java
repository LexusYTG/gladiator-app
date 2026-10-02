package com.gladiator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import com.termux.x11.LorieView;

import java.util.ArrayList;
import java.util.List;

public class CustomKeyboard extends View {

    private static final int L_MAIN = 0, L_SYM = 1, L_FN = 2;
    private static final int A_NONE = 0, A_SHIFT = 1, A_CTRL = 2, A_ALT = 3,
            A_L_MAIN = 4, A_L_SYM = 5, A_L_FN = 6, A_BACKSPACE = 7;

    private final LorieView target;
    private int layer = L_MAIN;
    private boolean shift = false, ctrl = false, alt = false;
    private final List<Key> allKeys = new ArrayList<>();
    private Key pressed = null;
    private int pressedPointer = -1;
    private final Handler repeat = new Handler(Looper.getMainLooper());
    private Runnable repeatTask;

    private final Paint bgP = new Paint();
    private final Paint keyP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokeP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    private static final int C_BG = 0xF0060A16;
    private static final int C_KEY = 0xFF0E1830;
    private static final int C_KEY_PRESSED = 0xCC00E5FF;
    private static final int C_KEY_MOD = 0xFF7C4DFF;
    private static final int C_STROKE = 0xFF24365E;
    private static final int C_STROKE_ACT = 0xFF00E5FF;
    private static final int C_TEXT = 0xFFE8F4FF;
    private static final int C_TEXT_PRESSED = 0xFF04060D;
    private static final int C_MAGENTA = 0xFFFF2BD6;

    public CustomKeyboard(Context c, LorieView target) {
        super(c);
        this.target = target;
        setClickable(true);
        setFocusable(false);
        bgP.setColor(C_BG);
        keyP.setColor(C_KEY);
        strokeP.setStyle(Paint.Style.STROKE);
        strokeP.setStrokeWidth(2f);
        strokeP.setColor(C_STROKE);
        textP.setColor(C_TEXT);
        textP.setTextAlign(Paint.Align.CENTER);
        textP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        rebuild();
    }

    private static class Key {
        String label;
        String text;
        String textShift;
        int keyCode;
        int action = A_NONE;
        float width = 1f;
        boolean accent = false;
        float px, py, pw, ph;
    }

    private static class Row { final List<Key> keys = new ArrayList<>(); }

    private static class K {
        final Key k = new Key();
        K t(String s) { k.text = s; k.label = s; return this; }
        K t2(String s) { k.textShift = s; return this; }
        K lbl(String l) { k.label = l; return this; }
        K code(int c) { k.keyCode = c; return this; }
        K act(int a) { k.action = a; return this; }
        K w(float w) { k.width = w; return this; }
        K acc() { k.accent = true; return this; }
    }

    private List<Row> rows = new ArrayList<>();

    private Row row(K... ks) {
        Row rw = new Row();
        for (K k : ks) rw.keys.add(k.k);
        return rw;
    }

    private void rebuild() {
        rows = new ArrayList<>();
        switch (layer) {
            case L_MAIN: buildMain(); break;
            case L_SYM:  buildSym();  break;
            case L_FN:   buildFn();   break;
        }
        post(() -> {
            if (getWidth() > 0) layoutKeys(getWidth(), getHeight());
            invalidate();
        });
    }

    private void buildMain() {
        rows.add(row(
                new K().t("1").t2("!"), new K().t("2").t2("@"), new K().t("3").t2("#"),
                new K().t("4").t2("$"), new K().t("5").t2("%"), new K().t("6").t2("^"),
                new K().t("7").t2("&"), new K().t("8").t2("*"), new K().t("9").t2("("),
                new K().t("0").t2(")"), new K().lbl("BKSP").act(A_BACKSPACE).w(1.5f)));
        rows.add(row(
                new K().t("q"), new K().t("w"), new K().t("e"), new K().t("r"),
                new K().t("t"), new K().t("y"), new K().t("u"), new K().t("i"),
                new K().t("o"), new K().t("p")));
        rows.add(row(
                new K().t("a"), new K().t("s"), new K().t("d"), new K().t("f"),
                new K().t("g"), new K().t("h"), new K().t("j"), new K().t("k"),
                new K().t("l"),
                new K().lbl("ENTER").code(android.view.KeyEvent.KEYCODE_ENTER).w(1.5f).acc()));
        rows.add(row(
                new K().lbl("SHIFT").act(A_SHIFT).w(1.5f),
                new K().t("z"), new K().t("x"), new K().t("c"), new K().t("v"),
                new K().t("b"), new K().t("n"), new K().t("m"),
                new K().t(",").t2("<"), new K().t(".").t2(">"), new K().t("/").t2("?")));
        rows.add(row(
                new K().lbl("?123").act(A_L_SYM).w(1.3f),
                new K().lbl("CTRL").act(A_CTRL).w(1.3f),
                new K().lbl("ALT").act(A_ALT).w(1.3f),
                new K().lbl("SPACE").t(" ").w(3.5f),
                new K().lbl("LEFT").code(android.view.KeyEvent.KEYCODE_DPAD_LEFT),
                new K().lbl("UP").code(android.view.KeyEvent.KEYCODE_DPAD_UP),
                new K().lbl("DOWN").code(android.view.KeyEvent.KEYCODE_DPAD_DOWN),
                new K().lbl("RIGHT").code(android.view.KeyEvent.KEYCODE_DPAD_RIGHT)));
    }

    private void buildSym() {
        rows.add(row(
                new K().t("!").t2("1"), new K().t("@").t2("2"), new K().t("#").t2("3"),
                new K().t("$").t2("4"), new K().t("%").t2("5"), new K().t("^").t2("6"),
                new K().t("&").t2("7"), new K().t("*").t2("8"), new K().t("(").t2("9"),
                new K().t(")").t2("0"),
                new K().lbl("BKSP").act(A_BACKSPACE).w(1.5f)));
        rows.add(row(
                new K().t("-").t2("_"), new K().t("=").t2("+"),
                new K().t("[").t2("{"), new K().t("]").t2("}"),
                new K().t("\\").t2("|"),
                new K().t(";").t2(":"), new K().t("'").t2("\""),
                new K().t("`").t2("~"),
                new K().t("§"), new K().t("°")));
        rows.add(row(
                new K().lbl("ABC").act(A_L_MAIN).w(1.5f),
                new K().t("€"), new K().t("£"), new K().t("¥"),
                new K().lbl("SPACE").t(" ").w(3.5f),
                new K().lbl("TAB").code(android.view.KeyEvent.KEYCODE_TAB).w(1.3f),
                new K().lbl("ESC").code(android.view.KeyEvent.KEYCODE_ESCAPE).w(1.3f),
                new K().lbl("ENTER").code(android.view.KeyEvent.KEYCODE_ENTER).w(1.5f).acc()));
        rows.add(row(
                new K().lbl("FN").act(A_L_FN).w(1.5f),
                new K().lbl("LEFT").code(android.view.KeyEvent.KEYCODE_DPAD_LEFT),
                new K().lbl("UP").code(android.view.KeyEvent.KEYCODE_DPAD_UP),
                new K().lbl("DOWN").code(android.view.KeyEvent.KEYCODE_DPAD_DOWN),
                new K().lbl("RIGHT").code(android.view.KeyEvent.KEYCODE_DPAD_RIGHT),
                new K().lbl("HOME").code(android.view.KeyEvent.KEYCODE_MOVE_HOME).w(1.3f),
                new K().lbl("END").code(android.view.KeyEvent.KEYCODE_MOVE_END).w(1.3f),
                new K().lbl("PGUP").code(android.view.KeyEvent.KEYCODE_PAGE_UP).w(1.3f),
                new K().lbl("PGDN").code(android.view.KeyEvent.KEYCODE_PAGE_DOWN).w(1.3f)));
    }

    private void buildFn() {
        rows.add(row(
                new K().lbl("F1").code(android.view.KeyEvent.KEYCODE_F1),
                new K().lbl("F2").code(android.view.KeyEvent.KEYCODE_F2),
                new K().lbl("F3").code(android.view.KeyEvent.KEYCODE_F3),
                new K().lbl("F4").code(android.view.KeyEvent.KEYCODE_F4),
                new K().lbl("F5").code(android.view.KeyEvent.KEYCODE_F5),
                new K().lbl("F6").code(android.view.KeyEvent.KEYCODE_F6),
                new K().lbl("F7").code(android.view.KeyEvent.KEYCODE_F7),
                new K().lbl("F8").code(android.view.KeyEvent.KEYCODE_F8),
                new K().lbl("F9").code(android.view.KeyEvent.KEYCODE_F9),
                new K().lbl("F10").code(android.view.KeyEvent.KEYCODE_F10)));
        rows.add(row(
                new K().lbl("F11").code(android.view.KeyEvent.KEYCODE_F11),
                new K().lbl("F12").code(android.view.KeyEvent.KEYCODE_F12),
                new K().lbl("ESC").code(android.view.KeyEvent.KEYCODE_ESCAPE),
                new K().lbl("TAB").code(android.view.KeyEvent.KEYCODE_TAB),
                new K().lbl("INS").code(android.view.KeyEvent.KEYCODE_INSERT),
                new K().lbl("DEL").code(android.view.KeyEvent.KEYCODE_FORWARD_DEL),
                new K().lbl("HOME").code(android.view.KeyEvent.KEYCODE_MOVE_HOME),
                new K().lbl("END").code(android.view.KeyEvent.KEYCODE_MOVE_END),
                new K().lbl("PGUP").code(android.view.KeyEvent.KEYCODE_PAGE_UP),
                new K().lbl("PGDN").code(android.view.KeyEvent.KEYCODE_PAGE_DOWN)));
        rows.add(row(
                new K().lbl("CTRL").act(A_CTRL).w(1.5f),
                new K().lbl("ALT").act(A_ALT).w(1.5f),
                new K().lbl("SHIFT").act(A_SHIFT).w(1.5f),
                new K().lbl("SPACE").t(" ").w(3f),
                new K().lbl("BKSP").act(A_BACKSPACE).w(1.5f),
                new K().lbl("ENTER").code(android.view.KeyEvent.KEYCODE_ENTER).w(1.5f).acc()));
        rows.add(row(
                new K().lbl("ABC").act(A_L_MAIN).w(1.5f),
                new K().lbl("SYM").act(A_L_SYM).w(1.5f),
                new K().lbl("LEFT").code(android.view.KeyEvent.KEYCODE_DPAD_LEFT),
                new K().lbl("UP").code(android.view.KeyEvent.KEYCODE_DPAD_UP),
                new K().lbl("DOWN").code(android.view.KeyEvent.KEYCODE_DPAD_DOWN),
                new K().lbl("RIGHT").code(android.view.KeyEvent.KEYCODE_DPAD_RIGHT),
                new K().lbl("CAPS").code(android.view.KeyEvent.KEYCODE_CAPS_LOCK).w(1.3f)));
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        int h = MeasureSpec.getSize(hs);
        float d = getResources().getDisplayMetrics().density;
        int rowsN = Math.max(1, rows.size());
        int desiredH = (int)(rowsN * 54 * d + 10 * d);
        setMeasuredDimension(w, Math.min(desiredH, h / 2));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        layoutKeys(w, h);
    }

    private void layoutKeys(int w, int h) {
        float pad = 4f * getResources().getDisplayMetrics().density;
        float innerW = w - pad * 2;
        int nRows = rows.size();
        if (nRows == 0) return;
        float rowH = (h - pad * 2) / nRows;

        allKeys.clear();
        for (int ri = 0; ri < nRows; ri++) {
            Row row = rows.get(ri);
            float totalW = 0;
            for (Key k : row.keys) totalW += k.width;
            float unitW = innerW / totalW;
            float x = pad;
            float y = pad + ri * rowH;
            for (Key k : row.keys) {
                k.px = x;
                k.py = y;
                k.pw = k.width * unitW;
                k.ph = rowH - pad;
                x += k.pw;
                allKeys.add(k);
            }
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        cv.drawRect(0, 0, getWidth(), getHeight(), bgP);
        for (Key k : allKeys) {
            boolean modActive = (k.action == A_SHIFT && shift)
                    || (k.action == A_CTRL && ctrl)
                    || (k.action == A_ALT && alt);
            boolean isPressed = (k == pressed);

            if (isPressed) keyP.setColor(C_KEY_PRESSED);
            else if (modActive) keyP.setColor(C_KEY_MOD);
            else keyP.setColor(C_KEY);

            strokeP.setColor(k.accent ? C_MAGENTA
                    : (isPressed || modActive ? C_STROKE_ACT : C_STROKE));

            r.set(k.px + 2, k.py + 2, k.px + k.pw - 2, k.py + k.ph - 2);
            float rad = k.ph * 0.15f;
            cv.drawRoundRect(r, rad, rad, keyP);
            cv.drawRoundRect(r, rad, rad, strokeP);

            textP.setColor(isPressed ? C_TEXT_PRESSED : C_TEXT);
            textP.setTextSize(Math.min(k.ph * 0.42f, k.pw * 0.5f));
            String lbl = displayLabel(k);
            float ty = r.centerY() - (textP.ascent() + textP.descent()) / 2f;
            cv.drawText(lbl, r.centerX(), ty, textP);
        }
    }

    private String displayLabel(Key k) {
        if (k.action != A_NONE) return k.label;
        if (k.text == null || k.text.isEmpty()) return k.label;
        if (shift) {
            if (k.textShift != null && !k.textShift.isEmpty()) return k.textShift;
            if (k.text.length() == 1 && Character.isLetter(k.text.charAt(0)))
                return k.text.toUpperCase();
        }
        return k.text.length() == 1 ? k.text : k.label;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int idx = e.getActionIndex();
        float x = e.getX(idx), y = e.getY(idx);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                Key k = hit(x, y);
                if (k != null) {
                    pressed = k;
                    pressedPointer = e.getPointerId(idx);
                    press(k);
                    if (k.action == A_BACKSPACE) startRepeat();
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (pressed == null) return true;
                int pi = e.findPointerIndex(pressedPointer);
                if (pi < 0) return true;
                Key k = hit(e.getX(pi), e.getY(pi));
                if (k != pressed) {
                    stopRepeat();
                    pressed = null;
                    if (k != null) {
                        pressed = k;
                        press(k);
                        if (k.action == A_BACKSPACE) startRepeat();
                    }
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                stopRepeat();
                pressed = null;
                pressedPointer = -1;
                invalidate();
                return true;
            }
        }
        return true;
    }

    private Key hit(float x, float y) {
        for (Key k : allKeys)
            if (x >= k.px && x <= k.px + k.pw && y >= k.py && y <= k.py + k.ph) return k;
        return null;
    }

    private void press(Key k) {
        switch (k.action) {
            case A_SHIFT: shift = !shift; break;
            case A_CTRL:  ctrl = !ctrl; break;
            case A_ALT:   alt = !alt; break;
            case A_L_MAIN: layer = L_MAIN; rebuild(); break;
            case A_L_SYM:  layer = L_SYM;  rebuild(); break;
            case A_L_FN:   layer = L_FN;   rebuild(); break;
            case A_BACKSPACE: sendKey(android.view.KeyEvent.KEYCODE_DEL); break;
            case A_NONE: sendActual(k); break;
        }
        invalidate();
    }

    private void sendActual(Key k) {
        if (target == null) return;
        boolean isChar = k.text != null && !k.text.isEmpty();
        boolean useKeyCode = !isChar || ctrl || alt;
        if (useKeyCode) {
            int code = k.keyCode;
            if (code == 0 && isChar) code = KeyMap.codeFor(k.text.toUpperCase());
            if (code == 0) return;
            if (ctrl) target.sendKeyEvent(0, android.view.KeyEvent.KEYCODE_CTRL_LEFT, true);
            if (alt)  target.sendKeyEvent(0, android.view.KeyEvent.KEYCODE_ALT_LEFT, true);
            target.sendKeyEvent(0, code, true);
            target.sendKeyEvent(0, code, false);
            if (alt)  target.sendKeyEvent(0, android.view.KeyEvent.KEYCODE_ALT_LEFT, false);
            if (ctrl) target.sendKeyEvent(0, android.view.KeyEvent.KEYCODE_CTRL_LEFT, false);
            ctrl = false; alt = false;
        } else {
            String toSend = k.text;
            if (shift) {
                if (k.textShift != null && !k.textShift.isEmpty()) toSend = k.textShift;
                else if (k.text.length() == 1 && Character.isLetter(k.text.charAt(0)))
                    toSend = k.text.toUpperCase();
            }
            try { target.sendTextEvent(toSend.getBytes("UTF-8")); } catch (Exception ignored) {}
            if (shift) shift = false;
        }
    }

    private void sendKey(int keyCode) {
        if (target == null) return;
        target.sendKeyEvent(0, keyCode, true);
        target.sendKeyEvent(0, keyCode, false);
    }

    private void startRepeat() {
        stopRepeat();
        repeatTask = new Runnable() {
            @Override public void run() {
                sendKey(android.view.KeyEvent.KEYCODE_DEL);
                repeat.postDelayed(this, 60);
            }
        };
        repeat.postDelayed(repeatTask, 500);
    }

    private void stopRepeat() {
        if (repeatTask != null) repeat.removeCallbacks(repeatTask);
        repeatTask = null;
    }
}
