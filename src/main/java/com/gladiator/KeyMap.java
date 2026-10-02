package com.gladiator;

import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeyMap {
    public static final Map<String, Integer> KEYS = new LinkedHashMap<>();
    static {
        KEYS.put("A", KeyEvent.KEYCODE_A);
        KEYS.put("B", KeyEvent.KEYCODE_B);
        KEYS.put("C", KeyEvent.KEYCODE_C);
        KEYS.put("D", KeyEvent.KEYCODE_D);
        KEYS.put("E", KeyEvent.KEYCODE_E);
        KEYS.put("F", KeyEvent.KEYCODE_F);
        KEYS.put("G", KeyEvent.KEYCODE_G);
        KEYS.put("H", KeyEvent.KEYCODE_H);
        KEYS.put("I", KeyEvent.KEYCODE_I);
        KEYS.put("J", KeyEvent.KEYCODE_J);
        KEYS.put("K", KeyEvent.KEYCODE_K);
        KEYS.put("L", KeyEvent.KEYCODE_L);
        KEYS.put("M", KeyEvent.KEYCODE_M);
        KEYS.put("N", KeyEvent.KEYCODE_N);
        KEYS.put("O", KeyEvent.KEYCODE_O);
        KEYS.put("P", KeyEvent.KEYCODE_P);
        KEYS.put("Q", KeyEvent.KEYCODE_Q);
        KEYS.put("R", KeyEvent.KEYCODE_R);
        KEYS.put("S", KeyEvent.KEYCODE_S);
        KEYS.put("T", KeyEvent.KEYCODE_T);
        KEYS.put("U", KeyEvent.KEYCODE_U);
        KEYS.put("V", KeyEvent.KEYCODE_V);
        KEYS.put("W", KeyEvent.KEYCODE_W);
        KEYS.put("X", KeyEvent.KEYCODE_X);
        KEYS.put("Y", KeyEvent.KEYCODE_Y);
        KEYS.put("Z", KeyEvent.KEYCODE_Z);
        KEYS.put("0", KeyEvent.KEYCODE_0);
        KEYS.put("1", KeyEvent.KEYCODE_1);
        KEYS.put("2", KeyEvent.KEYCODE_2);
        KEYS.put("3", KeyEvent.KEYCODE_3);
        KEYS.put("4", KeyEvent.KEYCODE_4);
        KEYS.put("5", KeyEvent.KEYCODE_5);
        KEYS.put("6", KeyEvent.KEYCODE_6);
        KEYS.put("7", KeyEvent.KEYCODE_7);
        KEYS.put("8", KeyEvent.KEYCODE_8);
        KEYS.put("9", KeyEvent.KEYCODE_9);
        KEYS.put("UP", KeyEvent.KEYCODE_DPAD_UP);
        KEYS.put("DOWN", KeyEvent.KEYCODE_DPAD_DOWN);
        KEYS.put("LEFT", KeyEvent.KEYCODE_DPAD_LEFT);
        KEYS.put("RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT);
        KEYS.put("F1", KeyEvent.KEYCODE_F1);
        KEYS.put("F2", KeyEvent.KEYCODE_F2);
        KEYS.put("F3", KeyEvent.KEYCODE_F3);
        KEYS.put("F4", KeyEvent.KEYCODE_F4);
        KEYS.put("F5", KeyEvent.KEYCODE_F5);
        KEYS.put("F6", KeyEvent.KEYCODE_F6);
        KEYS.put("F7", KeyEvent.KEYCODE_F7);
        KEYS.put("F8", KeyEvent.KEYCODE_F8);
        KEYS.put("F9", KeyEvent.KEYCODE_F9);
        KEYS.put("F10", KeyEvent.KEYCODE_F10);
        KEYS.put("F11", KeyEvent.KEYCODE_F11);
        KEYS.put("F12", KeyEvent.KEYCODE_F12);
        KEYS.put("ESC", KeyEvent.KEYCODE_ESCAPE);
        KEYS.put("TAB", KeyEvent.KEYCODE_TAB);
        KEYS.put("SHIFT", KeyEvent.KEYCODE_SHIFT_LEFT);
        KEYS.put("CTRL", KeyEvent.KEYCODE_CTRL_LEFT);
        KEYS.put("ALT", KeyEvent.KEYCODE_ALT_LEFT);
        KEYS.put("SPACE", KeyEvent.KEYCODE_SPACE);
        KEYS.put("ENTER", KeyEvent.KEYCODE_ENTER);
        KEYS.put("BKSP", KeyEvent.KEYCODE_DEL);
        KEYS.put("DEL", KeyEvent.KEYCODE_FORWARD_DEL);
        KEYS.put("HOME", KeyEvent.KEYCODE_MOVE_HOME);
        KEYS.put("END", KeyEvent.KEYCODE_MOVE_END);
        KEYS.put("PGUP", KeyEvent.KEYCODE_PAGE_UP);
        KEYS.put("PGDN", KeyEvent.KEYCODE_PAGE_DOWN);
        KEYS.put("INS", KeyEvent.KEYCODE_INSERT);
        KEYS.put("CAPS", KeyEvent.KEYCODE_CAPS_LOCK);
        KEYS.put("-", KeyEvent.KEYCODE_MINUS);
        KEYS.put("=", KeyEvent.KEYCODE_EQUALS);
        KEYS.put("[", KeyEvent.KEYCODE_LEFT_BRACKET);
        KEYS.put("]", KeyEvent.KEYCODE_RIGHT_BRACKET);
        KEYS.put("\\", KeyEvent.KEYCODE_BACKSLASH);
        KEYS.put(";", KeyEvent.KEYCODE_SEMICOLON);
        KEYS.put("'", KeyEvent.KEYCODE_APOSTROPHE);
        KEYS.put(",", KeyEvent.KEYCODE_COMMA);
        KEYS.put(".", KeyEvent.KEYCODE_PERIOD);
        KEYS.put("/", KeyEvent.KEYCODE_SLASH);
        KEYS.put("`", KeyEvent.KEYCODE_GRAVE);
    }

    public static List<String> labels() { return new ArrayList<>(KEYS.keySet()); }

    public static String labelFor(int keyCode) {
        for (Map.Entry<String, Integer> e : KEYS.entrySet())
            if (e.getValue() == keyCode) return e.getKey();
        return "?";
    }

    public static int codeFor(String label) {
        Integer v = KEYS.get(label);
        return v == null ? 0 : v;
    }
}
