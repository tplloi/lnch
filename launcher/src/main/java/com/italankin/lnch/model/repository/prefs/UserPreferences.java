package com.italankin.lnch.model.repository.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import com.italankin.lnch.R;

import javax.inject.Inject;

public class UserPreferences implements Preferences {
    private final Context context;
    private final SharedPreferences prefs;

    @Inject
    public UserPreferences(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    @Override
    public boolean searchShowSoftKeyboard() {
        return prefs.getBoolean(context.getString(R.string.prefs_search_show_soft_keyboard), true);
    }

    @Override
    public String homeLayout() {
        return prefs.getString(context.getString(R.string.prefs_home_layout), null);
    }

    @Override
    public void setOverlayColor(int color) {
        prefs.edit().putInt(context.getString(R.string.prefs_wallpaper_overlay_color), color).apply();
    }

    @Override
    public int overlayColor() {
        boolean show = prefs.getBoolean(
                context.getString(R.string.prefs_wallpaper_overlay_show), false);
        int defValue = Color.TRANSPARENT;
        return show
                ? prefs.getInt(context.getString(R.string.prefs_wallpaper_overlay_color), defValue)
                : defValue;
    }
}
