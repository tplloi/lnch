package com.italankin.lnch.model.ui.impl;

import androidx.annotation.NonNull;

import com.italankin.lnch.model.descriptor.impl.FolderDescriptor;
import com.italankin.lnch.model.ui.BadgeDescriptorUi;
import com.italankin.lnch.model.ui.CustomColorDescriptorUi;
import com.italankin.lnch.model.ui.CustomLabelDescriptorUi;
import com.italankin.lnch.model.ui.DescriptorUi;
import com.italankin.lnch.model.ui.RemovableDescriptorUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FolderDescriptorUi implements DescriptorUi,
        CustomColorDescriptorUi,
        CustomLabelDescriptorUi,
        RemovableDescriptorUi,
        BadgeDescriptorUi {

    public static final Object PAYLOAD_BADGE = new Object();

    private final FolderDescriptor descriptor;
    private final String label;
    private final int color;
    private String customLabel;
    private Integer customColor;
    public Integer customBadgeColor;
    public List<String> items;
    private boolean badgeVisible;

    public FolderDescriptorUi(FolderDescriptor descriptor) {
        this.descriptor = descriptor;
        this.label = descriptor.label;
        this.customLabel = descriptor.customLabel;
        this.color = descriptor.color;
        this.customColor = descriptor.customColor;
        this.items = new ArrayList<>(descriptor.items);
    }

    public FolderDescriptorUi(FolderDescriptorUi item) {
        this.descriptor = item.descriptor;
        this.label = item.label;
        this.customLabel = item.customLabel;
        this.color = item.color;
        this.customColor = item.customColor;
        this.customBadgeColor = item.customBadgeColor;
        this.items = new ArrayList<>(item.items);
        this.badgeVisible = item.badgeVisible;
    }

    @Override
    public FolderDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getCustomLabel() {
        return customLabel;
    }

    @Override
    public void setCustomLabel(String label) {
        this.customLabel = label;
    }

    @Override
    public void setCustomColor(Integer color) {
        this.customColor = color;
    }

    @Override
    public Integer getCustomColor() {
        return customColor;
    }

    @Override
    public void setBadgeVisible(boolean visible) {
        this.badgeVisible = visible;
    }

    @Override
    public boolean isBadgeVisible() {
        return badgeVisible;
    }

    @Override
    public Integer getCustomBadgeColor() {
        return customBadgeColor;
    }

    @NonNull
    @Override
    public String toString() {
        return descriptor.toString();
    }

    @Override
    public boolean is(DescriptorUi another) {
        if (this == another) {
            return true;
        }
        if (this.getClass() != another.getClass()) {
            return false;
        }
        FolderDescriptorUi that = (FolderDescriptorUi) another;
        return this.descriptor.equals(that.descriptor);
    }

    @Override
    public boolean deepEquals(DescriptorUi another) {
        if (this == another) {
            return true;
        }
        if (this.getClass() != another.getClass()) {
            return false;
        }
        FolderDescriptorUi that = (FolderDescriptorUi) another;
        return this.descriptor.equals(that.descriptor)
                && Objects.equals(this.customLabel, that.customLabel)
                && Objects.equals(this.customColor, that.customColor)
                && this.badgeVisible == that.badgeVisible;
    }

    @Override
    public Object getChangePayload(DescriptorUi oldItem) {
        if (this.badgeVisible != ((FolderDescriptorUi) oldItem).badgeVisible) {
            return PAYLOAD_BADGE;
        }
        return null;
    }
}
