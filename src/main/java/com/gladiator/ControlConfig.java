package com.gladiator;

public class ControlConfig {
    public static final int TYPE_BUTTON = 0;
    public static final int TYPE_JOYSTICK = 1;
    public static final int TYPE_DPAD = 2;

    public static final int SHAPE_SQUARE = 0;
    public static final int SHAPE_CIRCLE = 1;
    public static final int SHAPE_TRIANGLE = 2;

    public String id;
    public String label;
    public int type = TYPE_BUTTON;
    public int shape = SHAPE_CIRCLE;
    public int keyCode;
    public float cx = 0.5f, cy = 0.5f, sizeFrac = 0.15f;
    public int keyUp, keyDown, keyLeft, keyRight;

    public ControlConfig() {}

    public ControlConfig(String id, String label, int keyCode) {
        this.id = id;
        this.label = label;
        this.keyCode = keyCode;
    }
}
