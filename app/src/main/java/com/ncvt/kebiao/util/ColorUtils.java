package com.ncvt.kebiao.util;

import android.graphics.Color;

public final class ColorUtils {
    private static final int[] COLORS = {
            0xFFDCD8FF, 0xFFFFF0BE, 0xFFE4ECFF, 0xFFDFF3E4, 0xFFF9DFE7, 0xFFE6F7FF,
            0xFFFDE2D5, 0xFFECEBFF, 0xFFE8F6D8, 0xFFFFE2F2, 0xFFDFF6F1, 0xFFF3E5FF
    };

    private ColorUtils() {}

    public static int colorForCourse(String courseName) {
        return COLORS[(courseName.hashCode() & Integer.MAX_VALUE) % COLORS.length];
    }

    public static int textColorForBackground(int backgroundColor) {
        double darkness = 1 - (0.299 * Color.red(backgroundColor)
                + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255;
        return darkness >= 0.38 ? Color.WHITE : 0xFF4F5665;
    }
}
