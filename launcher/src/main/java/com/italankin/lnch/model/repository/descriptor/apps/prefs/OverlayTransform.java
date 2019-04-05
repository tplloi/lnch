package com.italankin.lnch.model.repository.descriptor.apps.prefs;

import com.italankin.lnch.model.descriptor.CustomColorDescriptor;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.impl.GroupDescriptor;
import com.italankin.lnch.model.repository.descriptor.apps.AppsData;
import com.italankin.lnch.model.repository.prefs.Preferences;

public class OverlayTransform implements ApplyPreferences.Transform {

    @Override
    public AppsData apply(AppsData appsData, Preferences preferences) {
        if (preferences.get(Preferences.APPS_COLOR_OVERLAY_SHOW)) {
            Integer colorOverlay = preferences.get(Preferences.APPS_COLOR_OVERLAY);
            for (Descriptor item : appsData.items) {
                if (item instanceof GroupDescriptor) {
                    continue;
                }
                if (item instanceof CustomColorDescriptor) {
                    ((CustomColorDescriptor) item).setCustomColor(colorOverlay);
                }
            }
        }
        return appsData;
    }
}
