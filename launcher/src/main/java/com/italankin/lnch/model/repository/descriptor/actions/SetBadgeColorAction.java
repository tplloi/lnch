package com.italankin.lnch.model.repository.descriptor.actions;

import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.impl.AppDescriptor;

import java.util.List;

public class SetBadgeColorAction extends BaseAction {
    private final String id;
    private final Integer newColor;

    public SetBadgeColorAction(AppDescriptor descriptor, Integer newColor) {
        this.id = descriptor.getId();
        this.newColor = newColor;
    }

    @Override
    public void apply(List<Descriptor> items) {
        AppDescriptor descriptor = findById(items, id);
        if (descriptor != null) {
            descriptor.customBadgeColor = newColor;
        }
    }
}