package com.italankin.lnch.model.repository.prefs;

public interface Preferences {

    boolean searchShowSoftKeyboard();

    HomeLayout homeLayout();

    void setOverlayColor(int color);

    int overlayColor();

    boolean useCustomTabs();

    boolean showScrollbar();

    void setItemTextSize(float size);

    float itemTextSize();

    void setItemPadding(int padding);

    int itemPadding();

    void setItemShadowRadius(float radius);

    float itemShadowRadius();

    void resetItemSettings();

    enum HomeLayout {
        COMPACT("compact"),
        GRID("grid"),
        LINEAR("linear");

        static HomeLayout from(String s) {
            for (HomeLayout value : values()) {
                if (value.name.equals(s)) {
                    return value;
                }
            }
            return COMPACT;
        }

        private final String name;

        HomeLayout(String name) {
            this.name = name;
        }
    }
}
