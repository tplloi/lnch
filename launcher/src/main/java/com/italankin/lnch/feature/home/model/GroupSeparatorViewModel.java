package com.italankin.lnch.feature.home.model;

import com.italankin.lnch.model.repository.descriptors.Descriptor;
import com.italankin.lnch.model.repository.descriptors.model.GroupDescriptor;

public class GroupSeparatorViewModel implements ItemViewModel {
    public final GroupDescriptor item;
    public final String label;
    public String customLabel;
    public boolean expanded = true;
    public int color;
    public Integer customColor;

    public GroupSeparatorViewModel(GroupDescriptor item) {
        this.item = item;
        this.label = item.label;
        this.color = item.color;
    }

    @Override
    public Descriptor getDescriptor() {
        return item;
    }

    @Override
    public String getVisibleLabel() {
        return customLabel != null ? customLabel : label;
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
    public int getVisibleColor() {
        return customColor != null ? customColor : color;
    }

    @Override
    public void setCustomColor(Integer color) {
        this.customColor = color;
    }
}
