package com.italankin.lnch.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

public final class ResUtils {

    public static int px2dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }

    public static int px2dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                Resources.getSystem().getDisplayMetrics());
    }

    public static TypedValue resolveAttribute(Context context, @AttrRes int attr) {
        if (context == context.getApplicationContext()) {
            throw new IllegalArgumentException("Cannot accept app context");
        }
        TypedValue out = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, out, true)) {
            return out;
        }
        return null;
    }

    @ColorInt
    public static int resolveColor(Context context, @AttrRes int attr) {
        TypedValue out = resolveAttribute(context, attr);
        if (out == null) {
            return Color.BLACK;
        }
        return out.data;
    }

    private ResUtils() {
        // no instance
    }
}
