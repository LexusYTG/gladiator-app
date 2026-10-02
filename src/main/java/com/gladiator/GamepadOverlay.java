package com.gladiator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.termux.x11.LorieView;

import java.util.List;

public class GamepadOverlay extends FrameLayout {
    private final LorieView target;

    public GamepadOverlay(Context c, LorieView target) {
        super(c);
        this.target = target;
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(false);
        setFocusable(false);
    }

    public void load(List<ControlConfig> controls) {
        removeAllViews();
        for (ControlConfig c : controls) {
            ControlView v = new ControlView(getContext(), c, target);
            addView(v, new LayoutParams(0, 0));
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        int h = MeasureSpec.getSize(hs);
        setMeasuredDimension(w, h);
        int minDim = Math.min(w, h);
        for (int i = 0; i < getChildCount(); i++) {
            ControlView v = (ControlView) getChildAt(i);
            int size = (int)(v.cfg.sizeFrac * minDim);
            v.measure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l, h = b - t;
        for (int i = 0; i < getChildCount(); i++) {
            ControlView c = (ControlView) getChildAt(i);
            int size = c.getMeasuredWidth();
            int cx = (int)(c.cfg.cx * w);
            int cy = (int)(c.cfg.cy * h);
            c.layout(cx - size/2, cy - size/2, cx + size/2, cy + size/2);
        }
    }

    private static class ControlView extends View {
        final ControlConfig cfg;
        final LorieView target;
        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint accent = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF rect = new RectF();
        final Path path = new Path();

        boolean held = false;
        int dpadActive = 0;
        int dpadPointerId = -1;
        int stickPointerId = -1;
        float stickX = 0f, stickY = 0f;
        int stickActiveKeys = 0;

        ControlView(Context c, ControlConfig cfg, LorieView target) {
            super(c);
            this.cfg = cfg;
            this.target = target;
            setClickable(true);
            setFocusable(false);
            fill.setColor(0x55203A60);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2f);
            stroke.setColor(0xFF00E5FF);
            text.setColor(0xFFE8F4FF);
            text.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            text.setTextAlign(Paint.Align.CENTER);
            accent.setColor(0xCC00E5FF);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            float pad = 3f;
            rect.set(pad, pad, w - pad, h - pad);
            switch (cfg.type) {
                case ControlConfig.TYPE_BUTTON: drawButton(cv, w, h, pad); break;
                case ControlConfig.TYPE_DPAD:   drawDpad(cv, w, h, pad); break;
                case ControlConfig.TYPE_JOYSTICK: drawJoystick(cv, w, h, pad); break;
            }
        }

        private void drawButton(Canvas cv, float w, float h, float pad) {
            drawShape(cv, w, h, pad, cfg.shape);
            String label = labelOrDefault();
            text.setTextSize(h * 0.36f);
            float ty = h / 2f - (text.ascent() + text.descent()) / 2f;
            cv.drawText(label, w / 2f, ty, text);
        }

        private void drawDpad(Canvas cv, float w, float h, float pad) {
            float cw = w / 3f, ch = h / 3f;
            fill.setColor(0x33203A60);
            cv.drawRoundRect(rect, h * 0.12f, h * 0.12f, fill);
            cv.drawRoundRect(rect, h * 0.12f, h * 0.12f, stroke);
            drawDpadCell(cv, 1, cw, 0, 0);
            drawDpadCell(cv, 2, cw, ch, ch);
            drawDpadCell(cv, 4, cw, 0, ch);
            drawDpadCell(cv, 8, cw, ch * 2, ch);
        }

        private void drawDpadCell(Canvas cv, int bit, float cw, float x, float y) {
            boolean on = (dpadActive & bit) != 0;
            Paint p = on ? accent : stroke;
            float pad = 4f;
            RectF cell = new RectF(x + pad, y + pad, x + cw - pad, y + cw - pad);
            cv.drawRoundRect(cell, cw * 0.15f, cw * 0.15f, p);
        }

        private void drawJoystick(Canvas cv, float w, float h, float pad) {
            float cx = w / 2f, cy = h / 2f;
            float outerR = Math.min(w, h) / 2f - pad;
            float innerR = outerR * 0.45f;
            fill.setColor(0x33203A60);
            cv.drawCircle(cx, cy, outerR, fill);
            cv.drawCircle(cx, cy, outerR, stroke);
            stroke.setAlpha(80);
            cv.drawLine(cx - outerR * 0.8f, cy, cx + outerR * 0.8f, cy, stroke);
            cv.drawLine(cx, cy - outerR * 0.8f, cx, cy + outerR * 0.8f, stroke);
            stroke.setAlpha(255);
            float sx = cx + stickX * (outerR - innerR);
            float sy = cy + stickY * (outerR - innerR);
            Paint stickFill = new Paint(Paint.ANTI_ALIAS_FLAG);
            stickFill.setColor(stickX != 0 || stickY != 0 ? 0xCC00E5FF : 0x55203A60);
            cv.drawCircle(sx, sy, innerR, stickFill);
            cv.drawCircle(sx, sy, innerR, stroke);
        }

        private void drawShape(Canvas cv, float w, float h, float pad, int shape) {
            switch (shape) {
                case ControlConfig.SHAPE_SQUARE: {
                    float r = h * 0.15f;
                    cv.drawRoundRect(rect, r, r, fill);
                    cv.drawRoundRect(rect, r, r, stroke);
                    break;
                }
                case ControlConfig.SHAPE_CIRCLE: {
                    float cx = w / 2f, cy = h / 2f, rad = Math.min(w, h) / 2f - pad;
                    cv.drawCircle(cx, cy, rad, fill);
                    cv.drawCircle(cx, cy, rad, stroke);
                    break;
                }
                case ControlConfig.SHAPE_TRIANGLE: {
                    path.reset();
                    path.moveTo(w / 2f, pad);
                    path.lineTo(w - pad, h - pad);
                    path.lineTo(pad, h - pad);
                    path.close();
                    cv.drawPath(path, fill);
                    cv.drawPath(path, stroke);
                    break;
                }
            }
        }

        private String labelOrDefault() {
            if (cfg.label != null && !cfg.label.isEmpty()) return cfg.label;
            return KeyMap.labelFor(cfg.keyCode);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (cfg.type) {
                case ControlConfig.TYPE_BUTTON: return handleButton(e);
                case ControlConfig.TYPE_DPAD:   return handleDpad(e);
                case ControlConfig.TYPE_JOYSTICK: return handleJoystick(e);
            }
            return false;
        }

        private boolean handleButton(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (!held) {
                        held = true;
                        if (target != null) target.sendKeyEvent(0, cfg.keyCode, true);
                        fill.setColor(0xCC00E5FF);
                        text.setColor(0xFF04060D);
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (held) {
                        held = false;
                        if (target != null) target.sendKeyEvent(0, cfg.keyCode, false);
                        fill.setColor(0x55203A60);
                        text.setColor(0xFFE8F4FF);
                        invalidate();
                    }
                    return true;
            }
            return false;
        }

        private boolean handleDpad(MotionEvent e) {
            int action = e.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    int idx = e.getActionIndex();
                    int bit = dpadBitAt(e.getX(idx), e.getY(idx), getWidth(), getHeight());
                    if (bit != 0) {
                        dpadActive |= bit;
                        pressDpadBit(bit, true);
                        dpadPointerId = e.getPointerId(idx);
                        invalidate();
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (dpadPointerId < 0) return true;
                    int idx = e.findPointerIndex(dpadPointerId);
                    if (idx < 0) return true;
                    int bit = dpadBitAt(e.getX(idx), e.getY(idx), getWidth(), getHeight());
                    if (bit != dpadActive) {
                        releaseAllDpadBits();
                        if (bit != 0) {
                            dpadActive = bit;
                            pressDpadBit(bit, true);
                        }
                        invalidate();
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL: {
                    releaseAllDpadBits();
                    dpadPointerId = -1;
                    invalidate();
                    return true;
                }
            }
            return true;
        }

        private int dpadBitAt(float x, float y, float w, float h) {
            float cw = w / 3f, ch = h / 3f;
            int col = (int)(x / cw), row = (int)(y / ch);
            if (row == 0 && col == 1) return 1;
            if (row == 2 && col == 1) return 2;
            if (col == 0 && row == 1) return 4;
            if (col == 2 && row == 1) return 8;
            return 0;
        }

        private void pressDpadBit(int bit, boolean down) {
            if (target == null) return;
            if (bit == 1 && cfg.keyUp != 0) target.sendKeyEvent(0, cfg.keyUp, down);
            if (bit == 2 && cfg.keyDown != 0) target.sendKeyEvent(0, cfg.keyDown, down);
            if (bit == 4 && cfg.keyLeft != 0) target.sendKeyEvent(0, cfg.keyLeft, down);
            if (bit == 8 && cfg.keyRight != 0) target.sendKeyEvent(0, cfg.keyRight, down);
        }

        private void releaseAllDpadBits() {
            if (target == null) { dpadActive = 0; return; }
            if ((dpadActive & 1) != 0 && cfg.keyUp != 0) target.sendKeyEvent(0, cfg.keyUp, false);
            if ((dpadActive & 2) != 0 && cfg.keyDown != 0) target.sendKeyEvent(0, cfg.keyDown, false);
            if ((dpadActive & 4) != 0 && cfg.keyLeft != 0) target.sendKeyEvent(0, cfg.keyLeft, false);
            if ((dpadActive & 8) != 0 && cfg.keyRight != 0) target.sendKeyEvent(0, cfg.keyRight, false);
            dpadActive = 0;
        }

        private boolean handleJoystick(MotionEvent e) {
            int action = e.getActionMasked();
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    int idx = e.getActionIndex();
                    stickPointerId = e.getPointerId(idx);
                    updateStick(e.getX(idx), e.getY(idx), cx, cy);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (stickPointerId < 0) return true;
                    int idx = e.findPointerIndex(stickPointerId);
                    if (idx < 0) return true;
                    updateStick(e.getX(idx), e.getY(idx), cx, cy);
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL: {
                    stickPointerId = -1;
                    stickX = 0; stickY = 0;
                    releaseStickKeys();
                    invalidate();
                    return true;
                }
            }
            return true;
        }

        private void updateStick(float px, float py, float cx, float cy) {
            float dx = px - cx, dy = py - cy;
            float mag = (float) Math.hypot(dx, dy);
            float radius = getWidth() / 2f;
            if (mag > radius) { dx = dx / mag * radius; dy = dy / mag * radius; mag = radius; }
            float dead = radius * 0.18f;
            if (mag < dead) {
                stickX = 0; stickY = 0;
                releaseStickKeys();
                invalidate();
                return;
            }
            stickX = dx / radius;
            stickY = dy / radius;
            float axisDead = dead;
            int keys = 0;
            if (dy < -axisDead) keys |= 1;
            if (dy >  axisDead) keys |= 2;
            if (dx < -axisDead) keys |= 4;
            if (dx >  axisDead) keys |= 8;
            if (keys != stickActiveKeys) {
                releaseStickKeys();
                stickActiveKeys = keys;
                pressStickKeys(keys);
            }
            invalidate();
        }

        private void pressStickKeys(int keys) {
            if (target == null) return;
            if ((keys & 1) != 0 && cfg.keyUp != 0) target.sendKeyEvent(0, cfg.keyUp, true);
            if ((keys & 2) != 0 && cfg.keyDown != 0) target.sendKeyEvent(0, cfg.keyDown, true);
            if ((keys & 4) != 0 && cfg.keyLeft != 0) target.sendKeyEvent(0, cfg.keyLeft, true);
            if ((keys & 8) != 0 && cfg.keyRight != 0) target.sendKeyEvent(0, cfg.keyRight, true);
        }

        private void releaseStickKeys() {
            if (target == null) { stickActiveKeys = 0; return; }
            if ((stickActiveKeys & 1) != 0 && cfg.keyUp != 0) target.sendKeyEvent(0, cfg.keyUp, false);
            if ((stickActiveKeys & 2) != 0 && cfg.keyDown != 0) target.sendKeyEvent(0, cfg.keyDown, false);
            if ((stickActiveKeys & 4) != 0 && cfg.keyLeft != 0) target.sendKeyEvent(0, cfg.keyLeft, false);
            if ((stickActiveKeys & 8) != 0 && cfg.keyRight != 0) target.sendKeyEvent(0, cfg.keyRight, false);
            stickActiveKeys = 0;
        }
    }
}
