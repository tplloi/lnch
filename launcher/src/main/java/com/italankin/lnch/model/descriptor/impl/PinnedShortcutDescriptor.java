package com.italankin.lnch.model.descriptor.impl;

import com.italankin.lnch.model.descriptor.CustomColorDescriptor;
import com.italankin.lnch.model.descriptor.CustomLabelDescriptor;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.DescriptorModels;
import com.italankin.lnch.model.descriptor.IgnorableDescriptor;
import com.italankin.lnch.model.repository.store.json.model.PinnedShortcutDescriptorJson;
import com.italankin.lnch.model.ui.impl.PinnedShortcutDescriptorUi;

import java.util.UUID;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Pinned intent for {@code com.android.launcher.action.INSTALL_SHORTCUT}
 */
@DescriptorModels(json = PinnedShortcutDescriptorJson.class, ui = PinnedShortcutDescriptorUi.class)
public final class PinnedShortcutDescriptor implements Descriptor, CustomColorDescriptor, CustomLabelDescriptor,
        IgnorableDescriptor {

    public String id;
    public String uri;
    public String originalLabel;
    public String label;
    public int color;
    public String customLabel;
    public Integer customColor;
    public boolean ignored;

    public PinnedShortcutDescriptor() {
    }

    public PinnedShortcutDescriptor(String uri, String label, @ColorInt int color) {
        this.id = "shortcut/" + UUID.randomUUID().toString();
        this.uri = uri;
        this.originalLabel = this.label = label;
        this.color = color;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getOriginalLabel() {
        return originalLabel;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public void setCustomColor(Integer color) {
        customColor = color;
    }

    @Override
    public Integer getCustomColor() {
        return customColor;
    }

    @Override
    public String getLabel() {
        return label != null ? label : getOriginalLabel();
    }

    @Override
    public void setCustomLabel(String label) {
        this.label = label;
        this.originalLabel = label;
        this.customLabel = label;
    }

    @Override
    public String getCustomLabel() {
        return customLabel;
    }

    @Override
    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    @Override
    public boolean isIgnored() {
        return ignored;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj.getClass() != PinnedShortcutDescriptor.class) {
            return false;
        }
        PinnedShortcutDescriptor that = (PinnedShortcutDescriptor) obj;
        return this.id.equals(that.id);
    }

    @NonNull
    @Override
    public String toString() {
        return "Shortcut{" + uri + "}";
    }

    @Override
    public PinnedShortcutDescriptor copy() {
        PinnedShortcutDescriptor copy = new PinnedShortcutDescriptor();
        copy.id = id;
        copy.uri = uri;
        copy.originalLabel = originalLabel;
        copy.label = label;
        copy.color = color;
        copy.ignored = ignored;
        copy.customLabel = customLabel;
        copy.customColor = customColor;
        return copy;
    }
}
