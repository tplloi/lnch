package com.italankin.lnch.feature.settings.lookfeel;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.italankin.lnch.R;
import com.italankin.lnch.feature.settings.base.AppPreferenceFragment;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.util.dialogfragment.ListenerFragment;
import com.italankin.lnch.util.widget.colorpicker.ColorPickerDialogFragment;
import com.italankin.lnch.util.widget.colorpicker.ColorPickerView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

public class LookAndFeelFragment extends AppPreferenceFragment implements MvpView {

    private static final String TAG_COLOR_OVERLAY = "color_overlay";

    @InjectPresenter
    LookAndFeelPresenter presenter;

    private Callbacks callbacks;
    private Preferences preferences;

    @ProvidePresenter
    LookAndFeelPresenter providePresenter() {
        return daggerService().presenters().lookAndFeel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = daggerService().main().getPreferences();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.save();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.prefs_look_and_feel);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findPreference(R.string.key_item_appearance).setOnPreferenceClickListener(preference -> {
            if (callbacks != null) {
                callbacks.showItemLookPreferences();
            }
            return true;
        });
        findPreference(Preferences.APPS_COLOR_OVERLAY_SHOW).setOnPreferenceChangeListener((preference, newValue) -> {
            updateColorOverlay((Boolean) newValue);
            return true;
        });
        findPreference(Preferences.APPS_COLOR_OVERLAY).setOnPreferenceClickListener(preference -> {
            onColorOverlayClick();
            return true;
        });
        updateColorOverlay(preferences.get(Preferences.APPS_COLOR_OVERLAY_SHOW));
    }

    private void updateColorOverlay(Boolean newValue) {
        Preference preference = findPreference(Preferences.APPS_COLOR_OVERLAY);
        if (newValue) {
            int color = preferences.get(Preferences.APPS_COLOR_OVERLAY);
            preference.setSummary(String.format("#%06x", color & 0x00ffffff));
        } else {
            preference.setSummary(null);
        }
    }

    private void onColorOverlayClick() {
        class SetAppsColorOverlay implements ListenerFragment<ColorPickerDialogFragment.Listener> {
            @Override
            public ColorPickerDialogFragment.Listener get(Fragment parentFragment) {
                LookAndFeelFragment fragment = (LookAndFeelFragment) parentFragment;
                return new ColorPickerDialogFragment.Listener() {
                    @Override
                    public void onColorPicked(int newColor) {
                        preferences.set(Preferences.APPS_COLOR_OVERLAY, newColor);
                        fragment.updateColorOverlay(true);
                    }

                    @Override
                    public void onColorReset() {
                        preferences.reset(Preferences.APPS_COLOR_OVERLAY);
                        fragment.updateColorOverlay(true);
                    }
                };
            }
        }
        new ColorPickerDialogFragment.Builder()
                .setColorModel(ColorPickerView.ColorModel.RGB)
                .setHexVisible(false)
                .setPreviewVisible(true)
                .setSelectedColor(preferences.get(Preferences.APPS_COLOR_OVERLAY))
                .showResetButton(true)
                .setListenerProvider(new SetAppsColorOverlay())
                .build()
                .show(getChildFragmentManager(), TAG_COLOR_OVERLAY);
    }

    public interface Callbacks {
        void showItemLookPreferences();
    }
}
