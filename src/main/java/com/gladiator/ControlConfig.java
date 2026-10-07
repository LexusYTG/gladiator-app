package com.gladiator;

public class ControlConfig {
    public static final int TYPE_BUTTON = 0;
    public static final int TYPE_JOYSTICK = 1;
    public static final int TYPE_DPAD = 2;
    /** Boton de disparo (forma de bala): mantiene click izq + mueve el mouse con el dedo. */
    public static final int TYPE_FIRE = 3;
    /** Rueda de scroll flotante. Solo activa en modo "Botones volumen". */
    public static final int TYPE_SCROLL = 4;

    public static final int SHAPE_SQUARE = 0;
    public static final int SHAPE_CIRCLE = 1;
    public static final int SHAPE_TRIANGLE = 2;

    public static final float DEFAULT_SENS = 1.5f;  // = sensibilidad de la camara tactil

    public String id;
    public String label;
    public int type = TYPE_BUTTON;
    public int shape = SHAPE_CIRCLE;
    public int keyCode;
    public float cx = 0.5f, cy = 0.5f, sizeFrac = 0.15f;
    public int keyUp, keyDown, keyLeft, keyRight;
    /** Solo TYPE_FIRE: multiplicador del movimiento del dedo -> mouse. */
    public float sens = DEFAULT_SENS;

    public ControlConfig() {}

    public ControlConfig(String id, String label, int keyCode) {
        this.id = id;
        this.label = label;
        this.keyCode = keyCode;
    }

    /** ancho / alto del control. sizeFrac siempre es el ALTO (fraccion del lado menor). */
    public float aspect() {
        switch (type) {
            case TYPE_FIRE:   return 0.55f;
            case TYPE_SCROLL: return 0.45f;
            default:          return 1f;
        }
    }
}
