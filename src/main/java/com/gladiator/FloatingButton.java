package com.gladiator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class FloatingButton extends View {

    public interface OnClickListener { void onClick(); }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
    private OnClickListener listener;

    private float fracX = 0.5f, fracY = 0.08f;
    private float downRawX, downRawY;
    private boolean dragging = false;

    public FloatingButton(Context c) {
        super(c);
        fill.setColor(0xCC00E5FF);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3f);
        stroke.setColor(0xFF00E5FF);
        glyph.setColor(0xFF04060D);
        glyph.setTextAlign(Paint.Align.CENTER);
        glyph.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        setClickable(true);
    }

    public FloatingButton setListener(OnClickListener l) { this.listener = l; return this; }
    public void setInitialFraction(float fx, float fy) { this.fracX = fx; this.fracY = fy; }

    @Override
    protected void onMeasure(int ws, int hs) {
        int size = (int)(44 * getResources().getDisplayMetrics().density + 0.5f);
        setMeasuredDimension(size, size);
    }

    public void applyPosition() {
        if (!(getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) getParent();
        int pw = parent.getWidth(), ph = parent.getHeight();
        if (pw <= 0 || ph <= 0) return;

        int size = getMeasuredWidth();
        int left = (int)(fracX * pw - size / 2f);
        int top = (int)(fracY * ph - size / 2f);
        left = Math.max(0, Math.min(pw - size, left));
        top = Math.max(0, Math.min(ph - size, top));

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp == null) lp = new FrameLayout.LayoutParams(size, size);
        lp.width = size;
        lp.height = size;
        lp.leftMargin = left;
        lp.topMargin = top;
        lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        setLayoutParams(lp);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float r = Math.min(w, h) / 2f - 4f;
        cv.drawCircle(cx, cy, r, fill);
        cv.drawCircle(cx, cy, r, stroke);
        float dotR = r * 0.13f;
        float spacing = r * 0.35f;
        cv.drawCircle(cx, cy - spacing, dotR, glyph);
        cv.drawCircle(cx, cy, dotR, glyph);
        cv.drawCircle(cx, cy + spacing, dotR, glyph);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = e.getRawX();
                downRawY = e.getRawY();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getRawX() - downRawX;
                float dy = e.getRawY() - downRawY;
                if (!dragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) dragging = true;
                if (dragging && getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) getParent();
                    int pw = parent.getWidth(), ph = parent.getHeight();
                    if (pw <= 0 || ph <= 0) return true;
                    int[] loc = new int[2];
                    parent.getLocationOnScreen(loc);
                    float nfx = (e.getRawX() - loc[0]) / pw;
                    float nfy = (e.getRawY() - loc[1]) / ph;
                    fracX = Math.max(0.02f, Math.min(0.98f, nfx));
                    fracY = Math.max(0.02f, Math.min(0.98f, nfy));
                    applyPosition();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!dragging && listener != null) listener.onClick();
                return true;
        }
        return super.onTouchEvent(e);
    }
}
