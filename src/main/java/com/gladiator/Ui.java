package com.gladiator;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public final class Ui {
    private Ui() {}

    public static final int BG        = 0xFF04060D;
    public static final int CYAN      = 0xFF00E5FF;
    public static final int MAGENTA   = 0xFFFF2BD6;
    public static final int VIOLET    = 0xFF7C4DFF;
    public static final int GREEN     = 0xFF39FF88;
    public static final int AMBER     = 0xFFFFB020;
    public static final int RED       = 0xFFFF3B5C;
    public static final int TEXT      = 0xFFE8F4FF;
    public static final int TEXT_DIM  = 0xFFB4C4E0;
    public static final int MUTED     = 0xFF6F82A8;
    public static final int LINE      = 0xFF24365E;

    private static final int PANEL_TOP = 0xF00E1730;
    private static final int PANEL_BOT = 0xF0090F22;

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int withAlpha(int color, int a) {
        return (color & 0x00FFFFFF) | ((a & 0xFF) << 24);
    }

    public static Typeface mono(boolean bold) {
        return Typeface.create(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    public static TextView label(Context c, String s, float sp, int color, boolean bold, float spacing) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setTypeface(mono(bold));
        t.setLetterSpacing(spacing);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView bodyText(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        t.setLineSpacing(dp(c, 3), 1f);
        return t;
    }

    public static void glow(TextView t, int color, float radiusDp) {
        t.setShadowLayer(dp(t.getContext(), radiusDp), 0, 0, color);
    }

    public static View vspace(Context c, float dpH) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpH)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{withAlpha(CYAN, 200), withAlpha(MAGENTA, 140), withAlpha(MAGENTA, 0)}));
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1))));
        return v;
    }

    public static void styleWindow(Activity a) {
        Window w = a.getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        View decor = w.getDecorView();
        decor.setBackgroundColor(BG);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            int f = decor.getSystemUiVisibility();
            if (Build.VERSION.SDK_INT >= 23) f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) f &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            f |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
               | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
               | View.SYSTEM_UI_FLAG_FULLSCREEN
               | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
               | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
               | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decor.setSystemUiVisibility(f);
        }
    }

    private static void pressScale(View v, boolean pressed, float scale) {
        v.animate().scaleX(pressed ? scale : 1f).scaleY(pressed ? scale : 1f)
                .setStartDelay(0).setDuration(90).start();
    }

    public static class Chamfer extends Drawable {
        private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokeP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float cut;
        private final float strokeW;
        private int top, bottom, stroke, glow, accent;
        private float inset;
        private float l, t, r, b, c;

        public Chamfer(float cutPx, float strokePx) {
            this.cut = cutPx;
            this.strokeW = strokePx;
            strokeP.setStyle(Paint.Style.STROKE);
            strokeP.setStrokeJoin(Paint.Join.MITER);
            inset = strokePx / 2f;
        }

        public Chamfer fill(int topColor, int bottomColor) {
            top = topColor;
            bottom = bottomColor;
            rebuild();
            invalidateSelf();
            return this;
        }

        public Chamfer solid(int color) { return fill(color, color); }

        public Chamfer stroke(int color) {
            stroke = color;
            invalidateSelf();
            return this;
        }

        public Chamfer glow(int color) {
            glow = color;
            inset = glow != 0 ? strokeW * 2.5f : strokeW / 2f;
            rebuild();
            invalidateSelf();
            return this;
        }

        public Chamfer accentCuts(int color) {
            accent = color;
            invalidateSelf();
            return this;
        }

        @Override protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            rebuild();
        }

        private void rebuild() {
            Rect bd = getBounds();
            l = bd.left + inset;
            t = bd.top + inset;
            r = bd.right - inset;
            b = bd.bottom - inset;
            path.reset();
            if (r - l <= 2 || b - t <= 2) return;
            c = Math.min(cut, Math.min(r - l, b - t) * 0.4f);
            path.moveTo(l + c, t);
            path.lineTo(r, t);
            path.lineTo(r, b - c);
            path.lineTo(r - c, b);
            path.lineTo(l, b);
            path.lineTo(l, t + c);
            path.close();
            if (top != bottom) {
                fillP.setShader(new LinearGradient(0, t, 0, b, top, bottom, Shader.TileMode.CLAMP));
                fillP.setColor(Color.WHITE);
            } else {
                fillP.setShader(null);
                fillP.setColor(top);
            }
        }

        @Override public void draw(Canvas cv) {
            if (path.isEmpty()) return;
            fillP.setStyle(Paint.Style.FILL);
            cv.drawPath(path, fillP);
            if (glow != 0) {
                float[] m = {5f, 3.5f, 2f};
                int[] a = {14, 26, 46};
                for (int i = 0; i < m.length; i++) {
                    strokeP.setStrokeWidth(strokeW * m[i]);
                    strokeP.setColor(withAlpha(glow, a[i]));
                    cv.drawPath(path, strokeP);
                }
            }
            if (stroke != 0 && strokeW > 0) {
                strokeP.setStrokeWidth(strokeW);
                strokeP.setColor(stroke);
                cv.drawPath(path, strokeP);
            }
            if (accent != 0) {
                strokeP.setStrokeWidth(strokeW * 2f);
                strokeP.setColor(accent);
                cv.drawLine(l, t + c, l + c, t, strokeP);
                cv.drawLine(r, b - c, r - c, b, strokeP);
            }
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(android.graphics.ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    public static final int G_PLUS = 0, G_GEAR = 1, G_POWER = 2, G_BACK = 3,
            G_TRASH = 4, G_PLAY = 5, G_HEX = 6;

    public static class GlyphView extends View {
        private final int type;
        private int color;
        private boolean glow;
        private String label;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();

        public GlyphView(Context ctx, int type, int color) {
            super(ctx);
            this.type = type;
            this.color = color;
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            tp.setTypeface(mono(true));
            tp.setTextAlign(Paint.Align.CENTER);
        }

        public GlyphView setLabel(String s) {
            label = (s == null || s.isEmpty()) ? null : s.substring(0, 1).toUpperCase(Locale.getDefault());
            invalidate();
            return this;
        }

        public GlyphView setGlow(boolean g) { glow = g; invalidate(); return this; }
        public void setColor(int c) { color = c; invalidate(); }

        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            float s = Math.min(w, h), cx = w / 2f, cy = h / 2f, r = s / 2f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.09f);
            p.setColor(color);
            switch (type) {
                case G_PLUS:
                    cv.drawLine(cx - r * 0.6f, cy, cx + r * 0.6f, cy, p);
                    cv.drawLine(cx, cy - r * 0.6f, cx, cy + r * 0.6f, p);
                    break;
                case G_BACK:
                    path.reset();
                    path.moveTo(cx + r * 0.2f, cy - r * 0.6f);
                    path.lineTo(cx - r * 0.4f, cy);
                    path.lineTo(cx + r * 0.2f, cy + r * 0.6f);
                    cv.drawPath(path, p);
                    break;
                case G_GEAR:
                    cv.drawCircle(cx, cy, r * 0.55f, p);
                    cv.drawCircle(cx, cy, r * 0.22f, p);
                    break;
                case G_POWER:
                    rect.set(cx - r * 0.55f, cy - r * 0.47f, cx + r * 0.55f, cy + r * 0.63f);
                    cv.drawArc(rect, -55, 290, false, p);
                    cv.drawLine(cx, cy - r * 0.68f, cx, cy - r * 0.05f, p);
                    break;
                case G_TRASH:
                    cv.drawLine(cx - r * 0.62f, cy - r * 0.4f, cx + r * 0.62f, cy - r * 0.4f, p);
                    path.reset();
                    path.moveTo(cx - r * 0.45f, cy - r * 0.4f);
                    path.lineTo(cx - r * 0.38f, cy + r * 0.66f);
                    path.lineTo(cx + r * 0.38f, cy + r * 0.66f);
                    path.lineTo(cx + r * 0.45f, cy - r * 0.4f);
                    cv.drawPath(path, p);
                    break;
                case G_PLAY:
                    path.reset();
                    path.moveTo(cx - r * 0.25f, cy - r * 0.52f);
                    path.lineTo(cx + r * 0.52f, cy);
                    path.lineTo(cx - r * 0.25f, cy + r * 0.52f);
                    path.close();
                    p.setStyle(Paint.Style.FILL_AND_STROKE);
                    cv.drawPath(path, p);
                    break;
                case G_HEX:
                default:
                    drawHex(cv, cx, cy, r, s);
                    break;
            }
        }

        private void drawHex(Canvas cv, float cx, float cy, float r, float s) {
            path.reset();
            float rr = r * 0.78f;
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(-90 + 60 * i);
                float x = cx + (float) (Math.cos(a) * rr);
                float y = cy + (float) (Math.sin(a) * rr);
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(withAlpha(color, 30));
            cv.drawPath(path, p);
            p.setStyle(Paint.Style.STROKE);
            if (glow) {
                float[] wd = {0.20f, 0.13f, 0.08f};
                int[] al = {20, 36, 60};
                for (int i = 0; i < wd.length; i++) {
                    p.setStrokeWidth(s * wd[i]);
                    p.setColor(withAlpha(color, al[i]));
                    cv.drawPath(path, p);
                }
            }
            p.setStrokeWidth(s * 0.055f);
            p.setColor(color);
            cv.drawPath(path, p);
            if (label != null) {
                tp.setColor(color);
                tp.setTextSize(s * 0.36f);
                float y = cy - (tp.ascent() + tp.descent()) / 2f;
                cv.drawText(label, cx, y, tp);
            }
        }
    }

    public static class NeonButton extends TextView {
        public static final int PRIMARY = 0, SECONDARY = 1, DANGER = 2;
        private final Chamfer bg;
        private final int fillA, fillB, pressA, pressB;

        public NeonButton(Context c, String label, int style) {
            super(c);
            int accent = style == DANGER ? RED : CYAN;
            setText(label.toUpperCase(Locale.getDefault()));
            setTypeface(mono(true));
            setTextSize(13);
            setLetterSpacing(0.1f);
            setGravity(Gravity.CENTER);
            setSingleLine(true);
            setEllipsize(TextUtils.TruncateAt.END);
            setClickable(true);
            setFocusable(true);
            setMinHeight(dp(c, 48));
            setPadding(dp(c, 18), 0, dp(c, 18), 0);

            bg = new Chamfer(dp(c, 10), dp(c, 1.5f));
            if (style == PRIMARY) {
                fillA = 0xFF00E5FF; fillB = 0xFF009BC7;
                pressA = 0xFF8AF4FF; pressB = 0xFF35C8E8;
                setTextColor(BG);
                bg.fill(fillA, fillB).stroke(0xFFB8F7FF).glow(CYAN);
            } else {
                fillA = 0xFF0E1A33; fillB = 0xFF0A1226;
                pressA = withAlpha(accent, 70); pressB = withAlpha(accent, 40);
                setTextColor(accent);
                bg.fill(fillA, fillB).stroke(withAlpha(accent, 210));
            }
            setBackground(bg);
        }

        @Override public void setPressed(boolean p) {
            super.setPressed(p);
            if (bg != null) bg.fill(p ? pressA : fillA, p ? pressB : fillB);
            pressScale(this, p, 0.96f);
        }
    }

    public static class IconButton extends FrameLayout {
        private final Chamfer bg;

        public IconButton(Context c, int glyph, int accent) {
            super(c);
            setClickable(true);
            setFocusable(true);
            bg = new Chamfer(dp(c, 8), dp(c, 1.2f))
                    .fill(0xFF0E1A33, 0xFF0A1226).stroke(withAlpha(accent, 170));
            setBackground(bg);
            GlyphView g = new GlyphView(c, glyph, accent);
            addView(g, new FrameLayout.LayoutParams(dp(c, 22), dp(c, 22), Gravity.CENTER));
        }

        @Override public void setPressed(boolean p) {
            super.setPressed(p);
            if (bg != null) bg.fill(p ? 0xFF1C3050 : 0xFF0E1A33, p ? 0xFF14243E : 0xFF0A1226);
            pressScale(this, p, 0.92f);
        }
    }

    public static IconButton iconButton(Context c, int glyph, int accent, float sizeDp) {
        IconButton b = new IconButton(c, glyph, accent);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return b;
    }

    public static class NeonCard extends LinearLayout {
        private final Chamfer bg;
        private final int accent;

        public NeonCard(Context c, int accent) {
            super(c);
            this.accent = accent;
            bg = new Chamfer(dp(c, 12), dp(c, 1.2f))
                    .fill(PANEL_TOP, PANEL_BOT).stroke(withAlpha(accent, 110)).accentCuts(accent);
            setBackground(bg);
            setClickable(true);
            setFocusable(true);
        }

        @Override public void setPressed(boolean p) {
            super.setPressed(p);
            if (bg == null) return;
            bg.fill(p ? 0xF01A2A52 : PANEL_TOP, p ? 0xF0111C3C : PANEL_BOT)
                    .stroke(withAlpha(accent, p ? 230 : 110));
            pressScale(this, p, 0.985f);
        }
    }

    public static LinearLayout card(Context c, int accent) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(new Chamfer(dp(c, 14), dp(c, 1.2f))
                .fill(PANEL_TOP, PANEL_BOT).stroke(withAlpha(accent, 100)).accentCuts(accent));
        l.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        return l;
    }

    public static View sectionTitle(Context c, String text, int accent) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View bar = new View(c);
        bar.setBackgroundColor(accent);
        row.addView(bar, new LinearLayout.LayoutParams(dp(c, 4), dp(c, 14)));
        TextView t = label(c, text.toUpperCase(Locale.getDefault()), 12, TEXT, true, 0.2f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(c, 10);
        row.addView(t, lp);
        return row;
    }

    public static TextView badge(Context c, String text, int color) {
        TextView t = label(c, text.toUpperCase(Locale.getDefault()), 10, color, true, 0.12f);
        t.setPadding(dp(c, 8), dp(c, 3), dp(c, 8), dp(c, 3));
        t.setBackground(new Chamfer(dp(c, 4), dp(c, 1))
                .fill(withAlpha(color, 34), withAlpha(color, 34)).stroke(withAlpha(color, 170)));
        return t;
    }

    public static EditText input(Context c, String hint) {
        final EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(CYAN);
        e.setTypeface(mono(true));
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setHighlightColor(withAlpha(CYAN, 90));
        e.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        final Chamfer bg = new Chamfer(dp(c, 8), dp(c, 1.2f))
                .fill(0xFF070C1A, 0xFF070C1A).stroke(LINE);
        e.setBackground(bg);
        e.setOnFocusChangeListener((v, f) -> bg.stroke(f ? CYAN : LINE));
        return e;
    }

    public static class GridBackground extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Shader sky, orbA, orbB, haze, grid;
        private ValueAnimator anim;
        private float phase;
        private boolean attached;

        public GridBackground(Context c) {
            super(c);
            linePaint.setStrokeWidth(Math.max(1f, dp(c, 1)));
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            float hy = h * 0.66f;
            float big = Math.max(w, h);
            sky = new LinearGradient(0, 0, 0, h,
                    new int[]{0xFF03050B, 0xFF0A1030, 0xFF170A36},
                    new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP);
            orbA = new RadialGradient(w * 0.12f, h * 0.06f, big * 0.6f,
                    withAlpha(MAGENTA, 64), withAlpha(MAGENTA, 0), Shader.TileMode.CLAMP);
            orbB = new RadialGradient(w * 0.95f, h * 0.22f, big * 0.55f,
                    withAlpha(CYAN, 50), withAlpha(CYAN, 0), Shader.TileMode.CLAMP);
            haze = new LinearGradient(0, hy - dp(getContext(), 70), 0, hy,
                    withAlpha(MAGENTA, 0), withAlpha(MAGENTA, 60), Shader.TileMode.CLAMP);
            grid = new LinearGradient(0, hy, 0, h,
                    withAlpha(CYAN, 0), withAlpha(CYAN, 120), Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            if (sky == null) return;
            float hy = h * 0.66f;

            p.setStyle(Paint.Style.FILL);
            p.setShader(sky);
            cv.drawRect(0, 0, w, h, p);
            p.setShader(orbA);
            cv.drawRect(0, 0, w, h, p);
            p.setShader(orbB);
            cv.drawRect(0, 0, w, h, p);
            p.setShader(haze);
            cv.drawRect(0, hy - dp(getContext(), 70), w, hy, p);
            p.setShader(null);

            p.setColor(withAlpha(MAGENTA, 130));
            cv.drawRect(0, hy - 1, w, hy + Math.max(1f, dp(getContext(), 1)), p);

            linePaint.setShader(grid);
            float cx = w / 2f;
            float step = w / 7f;
            for (int i = -14; i <= 14; i++) {
                cv.drawLine(cx, hy, cx + i * step, h, linePaint);
            }
            int rows = 11;
            for (int k = 0; k < rows; k++) {
                float z = (k + phase) / rows;
                float y = hy + (h - hy) * z * z;
                cv.drawLine(0, y, w, y, linePaint);
            }
            linePaint.setShader(null);

            float scan = (SystemClock.uptimeMillis() % 7000L) / 7000f;
            p.setColor(withAlpha(CYAN, 14));
            cv.drawRect(0, scan * h, w, scan * h + dp(getContext(), 2), p);
        }

        private void updateRunning() {
            boolean run = attached && getVisibility() == VISIBLE && getWindowVisibility() == VISIBLE;
            if (run && anim == null) {
                anim = ValueAnimator.ofFloat(0f, 1f);
                anim.setDuration(2800);
                anim.setRepeatCount(ValueAnimator.INFINITE);
                anim.setInterpolator(new LinearInterpolator());
                anim.addUpdateListener(a -> {
                    phase = (Float) a.getAnimatedValue();
                    invalidate();
                });
                anim.start();
            } else if (!run && anim != null) {
                anim.cancel();
                anim = null;
            }
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attached = true;
            updateRunning();
        }

        @Override protected void onDetachedFromWindow() {
            attached = false;
            updateRunning();
            super.onDetachedFromWindow();
        }

        @Override protected void onWindowVisibilityChanged(int v) {
            super.onWindowVisibilityChanged(v);
            updateRunning();
        }

        @Override protected void onVisibilityChanged(View changed, int v) {
            super.onVisibilityChanged(changed, v);
            if (changed == this) updateRunning();
        }
    }

    public static class HudBar extends View {
        private final Paint trackP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shineP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sepP = new Paint();
        private final Paint borderP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clip = new Path();
        private final Matrix mx = new Matrix();
        private Shader fillShader, shineShader;
        private float shown, target, sh;
        private boolean error;
        private ValueAnimator smooth, shimmer;

        public HudBar(Context c) {
            super(c);
            trackP.setColor(0xFF0B1224);
            sepP.setColor(0xE604060D);
            borderP.setStyle(Paint.Style.STROKE);
            borderP.setStrokeWidth(Math.max(1f, dp(c, 1)));
            borderP.setColor(withAlpha(CYAN, 120));
            fillP.setColor(RED);
        }

        public void setProgress(int pct) {
            target = Math.max(0, Math.min(100, pct)) / 100f;
            if (smooth != null) smooth.cancel();
            smooth = ValueAnimator.ofFloat(shown, target);
            smooth.setDuration(380);
            smooth.setInterpolator(new DecelerateInterpolator());
            smooth.addUpdateListener(a -> {
                shown = (Float) a.getAnimatedValue();
                invalidate();
            });
            smooth.start();
        }

        public void setError(boolean e) { error = e; invalidate(); }

        @Override protected void onMeasure(int ws, int hs) {
            setMeasuredDimension(resolveSize(dp(getContext(), 200), ws),
                    resolveSize(dp(getContext(), 14), hs));
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            float c = h * 0.42f;
            clip.reset();
            clip.moveTo(c, 0);
            clip.lineTo(w, 0);
            clip.lineTo(w, h - c);
            clip.lineTo(w - c, h);
            clip.lineTo(0, h);
            clip.lineTo(0, c);
            clip.close();
            fillShader = new LinearGradient(0, 0, w, 0, CYAN, MAGENTA, Shader.TileMode.CLAMP);
            shineShader = new LinearGradient(0, 0, dp(getContext(), 60), 0,
                    new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
            shineP.setShader(shineShader);
        }

        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            if (fillShader == null) return;
            cv.drawPath(clip, trackP);
            cv.save();
            cv.clipPath(clip);
            float fw = w * shown;
            if (fw > 0) {
                fillP.setShader(error ? null : fillShader);
                cv.drawRect(0, 0, fw, h, fillP);
                if (!error) {
                    float bw = dp(getContext(), 60);
                    float x = -bw + (fw + bw) * sh;
                    mx.setTranslate(x, 0);
                    shineShader.setLocalMatrix(mx);
                    cv.drawRect(Math.max(0, x), 0, Math.min(fw, x + bw), h, shineP);
                }
            }
            float seg = dp(getContext(), 12), sw = dp(getContext(), 2);
            for (float x = seg; x < w; x += seg) cv.drawRect(x, 0, x + sw, h, sepP);
            cv.restore();
            cv.drawPath(clip, borderP);
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            shimmer = ValueAnimator.ofFloat(0f, 1f);
            shimmer.setDuration(1600);
            shimmer.setRepeatCount(ValueAnimator.INFINITE);
            shimmer.setInterpolator(new LinearInterpolator());
            shimmer.addUpdateListener(a -> { sh = (Float) a.getAnimatedValue(); invalidate(); });
            shimmer.start();
        }

        @Override protected void onDetachedFromWindow() {
            if (shimmer != null) shimmer.cancel();
            if (smooth != null) smooth.cancel();
            super.onDetachedFromWindow();
        }
    }

    public interface Action { void run(GameDialog d); }

    public static class BootPanel extends FrameLayout {
        private final TextView status, percent;
        private final HudBar bar;
        private final TextView[] history = new TextView[3];
        private final NeonButton action;
        private final GlyphView logo;
        private String last;
        private ValueAnimator pulse;
        private boolean failed;

        public BootPanel(Context c, String chipText) {
            super(c);
            ScrollView sv = new ScrollView(c);
            sv.setFillViewport(true);
            sv.setVerticalScrollBarEnabled(false);
            FrameLayout holder = new FrameLayout(c);
            sv.addView(holder, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            addView(sv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setPadding(dp(c, 24), dp(c, 24), dp(c, 24), dp(c, 24));
            int screenW = c.getResources().getDisplayMetrics().widthPixels;
            FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(
                    Math.min(screenW, dp(c, 480)), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            holder.addView(col, colLp);

            logo = new GlyphView(c, G_HEX, CYAN).setLabel("G").setGlow(true);
            col.addView(logo, new LinearLayout.LayoutParams(dp(c, 84), dp(c, 84)));
            col.addView(vspace(c, 18));

            TextView title = label(c, "GLADIATOR", 30, TEXT, true, 0.28f);
            title.setGravity(Gravity.CENTER);
            glow(title, CYAN, 14);
            col.addView(title);
            col.addView(vspace(c, 8));

            TextView sub = label(c, "LINUX // X11 // CONTENEDORES", 10, MUTED, false, 0.3f);
            sub.setGravity(Gravity.CENTER);
            col.addView(sub);

            if (chipText != null) {
                col.addView(vspace(c, 14));
                col.addView(badge(c, chipText, MAGENTA));
            }
            col.addView(vspace(c, 30));

            LinearLayout card = card(c, CYAN);
            col.addView(card, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout top = new LinearLayout(c);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            status = label(c, "", 13, TEXT, true, 0.04f);
            top.addView(status, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            percent = label(c, "0%", 13, CYAN, true, 0.04f);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.leftMargin = dp(c, 12);
            top.addView(percent, plp);
            card.addView(top);
            card.addView(vspace(c, 12));

            bar = new HudBar(c);
            card.addView(bar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 14)));
            card.addView(vspace(c, 14));

            float[] alphas = {0.30f, 0.48f, 0.68f};
            for (int i = 0; i < history.length; i++) {
                history[i] = label(c, "", 10.5f, MUTED, false, 0.03f);
                history[i].setAlpha(alphas[i]);
                history[i].setSingleLine(true);
                history[i].setEllipsize(TextUtils.TruncateAt.END);
                card.addView(history[i]);
                card.addView(vspace(c, 3));
            }

            action = new NeonButton(c, "Salir", NeonButton.DANGER);
            action.setVisibility(GONE);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alp.topMargin = dp(c, 14);
            card.addView(action, alp);
        }

        public void set(String msg, int pct) {
            if (last != null && !last.equals(msg)) push(last);
            last = msg;
            status.setText(msg);
            status.setTextColor(TEXT);
            bar.setError(false);
            bar.setProgress(pct);
            percent.setText(pct + "%");
        }

        private void push(String s) {
            for (int i = 0; i < history.length - 1; i++) history[i].setText(history[i + 1].getText());
            history[history.length - 1].setText("> " + s);
        }

        public void fail(String msg, String buttonLabel, final Runnable onClick) {
            failed = true;
            if (last != null) push(last);
            last = msg;
            status.setText(msg);
            status.setTextColor(RED);
            percent.setTextColor(RED);
            bar.setError(true);
            logo.setColor(RED);
            if (pulse != null) pulse.cancel();
            logo.setAlpha(1f);
            action.setText(buttonLabel.toUpperCase(Locale.getDefault()));
            action.setOnClickListener(v -> onClick.run());
            action.setVisibility(VISIBLE);
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (failed) return;
            pulse = ValueAnimator.ofFloat(0f, 1f);
            pulse.setDuration(1300);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.addUpdateListener(a -> {
                float v = (Float) a.getAnimatedValue();
                logo.setAlpha(0.65f + 0.35f * v);
                logo.setScaleX(0.96f + 0.06f * v);
                logo.setScaleY(0.96f + 0.06f * v);
            });
            pulse.start();
        }

        @Override protected void onDetachedFromWindow() {
            if (pulse != null) pulse.cancel();
            super.onDetachedFromWindow();
        }
    }

    public static class GameDialog {
        private final Activity act;
        private final Dialog dialog;
        private final LinearLayout card, body, buttons;

        public GameDialog(Activity a, String title, int accent) {
            act = a;
            dialog = new Dialog(a);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            card = new LinearLayout(a);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(a, 22), dp(a, 20), dp(a, 22), dp(a, 18));
            card.setBackground(new Chamfer(dp(a, 14), dp(a, 1.5f))
                    .fill(0xFF121C38, 0xFF0A1226).stroke(withAlpha(accent, 210))
                    .glow(accent).accentCuts(accent));

            LinearLayout head = new LinearLayout(a);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            View mark = new View(a);
            mark.setBackgroundColor(accent);
            head.addView(mark, new LinearLayout.LayoutParams(dp(a, 4), dp(a, 18)));
            TextView t = label(a, title.toUpperCase(Locale.getDefault()), 14, TEXT, true, 0.12f);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.leftMargin = dp(a, 10);
            head.addView(t, tlp);
            card.addView(head);
            card.addView(vspace(a, 14));

            body = new LinearLayout(a);
            body.setOrientation(LinearLayout.VERTICAL);
            card.addView(body, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            buttons = new LinearLayout(a);
            buttons.setOrientation(LinearLayout.HORIZONTAL);

            dialog.setContentView(card, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setDimAmount(0.78f);
                int sw = a.getResources().getDisplayMetrics().widthPixels;
                w.setLayout(Math.min(sw - dp(a, 24), dp(a, 460)), WindowManager.LayoutParams.WRAP_CONTENT);
            }
        }

        public GameDialog message(String msg) {
            body.addView(bodyText(act, msg, 14, TEXT_DIM));
            return this;
        }

        public GameDialog view(View v) {
            body.addView(v);
            return this;
        }

        public GameDialog cancelable(boolean c) {
            dialog.setCancelable(c);
            dialog.setCanceledOnTouchOutside(c);
            return this;
        }

        public GameDialog keyboard() {
            Window w = dialog.getWindow();
            if (w != null) w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            return this;
        }

        public GameDialog button(String label, int style, final Action action) {
            if (buttons.getParent() == null) {
                card.addView(vspace(act, 18));
                card.addView(buttons, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            NeonButton b = new NeonButton(act, label, style);
            b.setOnClickListener(v -> {
                if (action == null) dismiss(); else action.run(this);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (buttons.getChildCount() > 0) lp.leftMargin = dp(act, 10);
            buttons.addView(b, lp);
            return this;
        }

        public void show() {
            card.setAlpha(0f);
            card.setScaleX(0.92f);
            card.setScaleY(0.92f);
            dialog.setOnShowListener(d -> card.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setStartDelay(0).setDuration(180)
                    .setInterpolator(new DecelerateInterpolator()).start());
            dialog.show();
        }

        public void dismiss() {
            try { dialog.dismiss(); } catch (Exception ignored) {}
        }
    }

    // ============================================================ CHIPS
    public interface OnPick { void pick(int index); }

    public static class Chip extends TextView {
        private final Chamfer bg;
        public Chip(Context c, String label) {
            super(c);
            setText(label);
            setTypeface(mono(true));
            setTextSize(12);
            setGravity(Gravity.CENTER);
            setSingleLine(true);
            setMinHeight(dp(c, 44));
            setPadding(dp(c, 14), 0, dp(c, 14), 0);
            setClickable(true);
            setFocusable(true);
            bg = new Chamfer(dp(c, 8), dp(c, 1.2f));
            setBackground(bg);
            setChosen(false);
        }
        public void setChosen(boolean sel) {
            if (sel) {
                bg.fill(withAlpha(CYAN, 52), withAlpha(CYAN, 24)).stroke(CYAN).glow(CYAN);
                setTextColor(CYAN);
            } else {
                bg.fill(0xFF0E1830, 0xFF0A1226).stroke(LINE).glow(0);
                setTextColor(MUTED);
            }
        }
        @Override public void setPressed(boolean p) {
            super.setPressed(p);
            pressScale(this, p, 0.95f);
        }
    }

    public static View choices(Context c, String[] labels, int selected, boolean equalWidth, final OnPick cb) {
        final Chip[] chips = new Chip[labels.length];
        ViewGroup group;
        if (equalWidth) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            group = row;
        } else {
            group = new FlowLayout(c, dp(c, 8), dp(c, 8));
        }
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            Chip chip = new Chip(c, labels[i]);
            chips[i] = chip;
            chip.setChosen(i == selected);
            chip.setOnClickListener(v -> {
                for (int j = 0; j < chips.length; j++) chips[j].setChosen(j == idx);
                if (cb != null) cb.pick(idx);
            });
            if (equalWidth) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (i > 0) lp.leftMargin = dp(c, 8);
                group.addView(chip, lp);
            } else {
                group.addView(chip);
            }
        }
        return group;
    }

    public static class FlowLayout extends ViewGroup {
        private final int hGap, vGap;
        public FlowLayout(Context c, int hGap, int vGap) {
            super(c);
            this.hGap = hGap;
            this.vGap = vGap;
        }
        @Override protected void onMeasure(int ws, int hs) {
            int maxW = MeasureSpec.getSize(ws) - getPaddingLeft() - getPaddingRight();
            int x = 0, y = 0, rowH = 0, used = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View ch = getChildAt(i);
                if (ch.getVisibility() == GONE) continue;
                ch.measure(MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int cw = ch.getMeasuredWidth(), chh = ch.getMeasuredHeight();
                if (x > 0 && x + cw > maxW) { x = 0; y += rowH + vGap; rowH = 0; }
                x += cw + hGap;
                rowH = Math.max(rowH, chh);
                used = Math.max(used, x - hGap);
            }
            int totalH = y + rowH + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(resolveSize(used + getPaddingLeft() + getPaddingRight(), ws),
                    resolveSize(totalH, hs));
        }
        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int maxW = r - l - getPaddingLeft() - getPaddingRight();
            int x = 0, y = 0, rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View ch = getChildAt(i);
                if (ch.getVisibility() == GONE) continue;
                int cw = ch.getMeasuredWidth(), chh = ch.getMeasuredHeight();
                if (x > 0 && x + cw > maxW) { x = 0; y += rowH + vGap; rowH = 0; }
                int left = getPaddingLeft() + x, top = getPaddingTop() + y;
                ch.layout(left, top, left + cw, top + chh);
                x += cw + hGap;
                rowH = Math.max(rowH, chh);
            }
        }
    }
}
