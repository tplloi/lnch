package com.italankin.lnch.model.provider.color;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.TypedValue;

public class DominantColorProvider implements ColorProvider {
    @Override
    public Integer get(PackageManager pm, ResolveInfo ri) {
        Bitmap bitmap = getIconBitmap(ri.loadIcon(pm));
        Palette palette = Palette.from(bitmap).generate();
        int dominant = palette.getDominantColor(Color.WHITE);
        float[] hsv = new float[3];
        Color.colorToHSV(dominant, hsv);
        if (hsv[2] < 0.15) {
            return palette.getLightVibrantColor(Color.WHITE);
        }
        return dominant;
    }

    private static Bitmap getIconBitmap(Drawable icon) {
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                Resources.getSystem().getDisplayMetrics());
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        icon.setBounds(0, 0, size, size);
        icon.draw(canvas);
        return bitmap;
    }
}
