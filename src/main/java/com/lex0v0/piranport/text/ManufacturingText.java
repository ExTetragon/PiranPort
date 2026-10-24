package com.lex0v0.piranport.text;

import java.util.Locale;

// 制造界面统一显示粒折锭数与向上取整到秒的任务时长。
public final class ManufacturingText {
    private ManufacturingText() {}
    public static String metal(long units) {
        return units % 9 == 0 ? Long.toString(units / 9) : String.format(Locale.ROOT, "%.2f", units / 9.0);
    }
    public static String time(int ticks) {
        int seconds = Math.max(0, (int) ((ticks + 19L) / 20));
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
