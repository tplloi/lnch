package com.italankin.lnch.feature.settings;

import android.os.Bundle;

import com.italankin.lnch.R;
import com.italankin.lnch.feature.settings.base.BasePreferenceFragment;

public class ShortcutsFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.prefs_shortcuts);
    }
}
