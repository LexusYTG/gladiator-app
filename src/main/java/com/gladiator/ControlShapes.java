package com.gladiator;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Dibujo compartido entre el overlay del juego y el editor de controles. */
final class ControlShapes {
    private ControlShapes() {}

    private static final Paint KNOB = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ---------------------------------------------------------------- bala

    static void bulletPath(Path p, RectF r) {
        float w = r.width(), h = r.height(), cx = r.centerX();
        float shoulder = r.top + h * 0.40f;
        p.reset();
        p.moveTo(r.left, r.bottom);
        p.lineTo(r.left, shoulder);
        p.cubicTo(r.left, r.top + h * 0.20f, cx - w * 0.20f, r.top + h * 0.06f, cx, r.top);
        p.cubicTo(cx + w * 0.20f, r.top + h * 0.06f, r.right, r.top + h * 0.20f, r.right, shoulder);
        p.lineTo(r.right, r.bottom);
        p.close();
    }

    static void drawBullet(Canvas cv, RectF r, Path path, Paint fill, Paint stroke,
                           Paint accent, boolean pressed) {
        bulletPath(path, r);
        cv.drawPath(path, pressed ? accent : fill);
        cv.drawPath(path, stroke);
        float y = r.bottom - r.height() * 0.18f;      // ranura del casquillo
        cv.drawLine(r.left, y, r.right, y, stroke);
    }

    // -------------------------------------------------------------- scroll

    /** Recorrido maximo (px) del boton desde el centro, hacia arriba o abajo. */
    static float scrollTravel(float w, float h) {
        return Math.max(0f, h / 2f - w * 0.95f);
    }

    /**
     * Rueda: carril vertical con flechas arriba/abajo y una bolita central.
     * knobOff = desplazamiento de la bolita en px (negativo = arriba),
     * phase = avance acumulado en px para animar las marcas.
     */
    static void drawScroll(Canvas cv, RectF r, Path path, Paint fill, Paint stroke,
                           Paint accent, float knobOff, float phase, boolean active) {
        float w = r.width(), h = r.height(), cx = r.centerX(), cy = r.centerY();
        cv.drawRoundRect(r, w / 2f, w / 2f, fill);
        cv.drawRoundRect(r, w / 2f, w / 2f, stroke);

        float a = w * 0.20f;
        float ty = r.top + w * 0.55f, by = r.bottom - w * 0.55f;
        path.reset();
        path.moveTo(cx, ty - a); path.lineTo(cx + a, ty + a * 0.6f); path.lineTo(cx - a, ty + a * 0.6f);
        path.close();
        cv.drawPath(path, accent);
        path.reset();
        path.moveTo(cx, by + a); path.lineTo(cx + a, by - a * 0.6f); path.lineTo(cx - a, by - a * 0.6f);
        path.close();
        cv.drawPath(path, accent);

        // marcas que "ruedan" con el scroll
        float spacing = Math.max(8f, h * 0.12f);
        float limit = h / 2f - w * 1.0f;
        float off = ((phase % spacing) + spacing) % spacing;
        stroke.setAlpha(90);
        for (int k = -8; k <= 8; k++) {
            float yy = cy + k * spacing + off;
            if (Math.abs(yy - cy) < limit)
                cv.drawLine(cx - w * 0.18f, yy, cx + w * 0.18f, yy, stroke);
        }
        stroke.setAlpha(255);

        float kr = w * 0.34f;
        float ky = cy + knobOff;
        KNOB.setColor(active ? 0xCC00E5FF : 0xFF14233F);
        cv.drawCircle(cx, ky, kr, KNOB);
        cv.drawCircle(cx, ky, kr, stroke);
        cv.drawCircle(cx, ky, kr * 0.28f, stroke);     // el "0" del dibujo
    }
}
