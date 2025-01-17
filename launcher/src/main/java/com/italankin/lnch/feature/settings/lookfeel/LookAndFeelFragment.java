package com.italankin.lnch.feature.settings.lookfeel;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.italankin.lnch.LauncherApp;
import com.italankin.lnch.R;
import com.italankin.lnch.feature.home.fragmentresult.SignalFragmentResultContract;
import com.italankin.lnch.feature.settings.SettingsToolbarTitle;
import com.italankin.lnch.feature.settings.base.AppPreferenceFragment;
import com.italankin.lnch.model.repository.prefs.Preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

public class LookAndFeelFragment extends AppPreferenceFragment implements MvpView, SettingsToolbarTitle {

    public static LookAndFeelFragment newInstance(String requestKey) {
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        LookAndFeelFragment fragment = new LookAndFeelFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @InjectPresenter
    LookAndFeelPresenter presenter;

    private Preferences preferences;

    @ProvidePresenter
    LookAndFeelPresenter providePresenter() {
        return LauncherApp.daggerService.presenters().lookAndFeel();
    }

    @Override
    public CharSequence getToolbarTitle(Context context) {
        return context.getString(R.string.settings_home_laf);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = LauncherApp.daggerService.main().preferences();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.saveData();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.prefs_look_and_feel);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findPreference(R.string.pref_key_appearance).setOnPreferenceClickListener(preference -> {
            sendResult(new ShowItemLookPreferencesContract().result());
            return true;
        });

        Preference folderOverlayColor = findPreference(Preferences.FOLDER_OVERLAY_COLOR);
        folderOverlayColor.setEnabled(
                preferences.get(Preferences.FULLSCREEN_FOLDERS) || preferences.get(Preferences.FOLDER_SHOW_OVERLAY));
        Preference folderShowOverlay = findPreference(Preferences.FOLDER_SHOW_OVERLAY);
        folderShowOverlay.setEnabled(!preferences.get(Preferences.FULLSCREEN_FOLDERS));
        folderShowOverlay.setOnPreferenceChangeListener((preference, newValue) -> {
            folderOverlayColor.setEnabled(preferences.get(Preferences.FULLSCREEN_FOLDERS) || (boolean) newValue);
            return true;
        });
        findPreference(Preferences.FULLSCREEN_FOLDERS).setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isEnabled = (boolean) newValue;
            folderOverlayColor.setEnabled(isEnabled || preferences.get(Preferences.FOLDER_SHOW_OVERLAY));
            folderShowOverlay.setEnabled(!isEnabled);
            return true;
        });
        findPreference(Preferences.NOTIFICATION_DOT_COLOR)
                .setEnabled(preferences.get(Preferences.NOTIFICATION_DOT));
        findPreference(Preferences.APPS_LIST_ANIMATE)
                .setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        findPreference(Preferences.HIDE_STATUS_BAR).setOnPreferenceChangeListener((preference, newValue) -> {
            updateStatusBarColorDependency(((Boolean) newValue));
            return true;
        });
        updateStatusBarColorDependency(preferences.get(Preferences.HIDE_STATUS_BAR));
        scrollToTarget();
    }

    private void updateStatusBarColorDependency(Boolean hideStatusBar) {
        findPreference(Preferences.STATUS_BAR_COLOR).setEnabled(!hideStatusBar);
    }

    public static class ShowItemLookPreferencesContract extends SignalFragmentResultContract {
        public ShowItemLookPreferencesContract() {
            super("show_item_look_preferences");
        }
    }
}
