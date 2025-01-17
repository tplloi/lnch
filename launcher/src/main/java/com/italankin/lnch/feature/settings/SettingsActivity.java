package com.italankin.lnch.feature.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.italankin.lnch.LauncherApp;
import com.italankin.lnch.R;
import com.italankin.lnch.api.LauncherIntents;
import com.italankin.lnch.feature.base.BackButtonHandler;
import com.italankin.lnch.feature.common.preferences.SupportsOrientationDelegate;
import com.italankin.lnch.feature.home.fragmentresult.FragmentResultContract;
import com.italankin.lnch.feature.home.fragmentresult.FragmentResultManager;
import com.italankin.lnch.feature.settings.apps.AppsSettingsFragment;
import com.italankin.lnch.feature.settings.apps.details.AppDetailsFragment;
import com.italankin.lnch.feature.settings.apps.details.aliases.AppAliasesFragment;
import com.italankin.lnch.feature.settings.backup.BackupFragment;
import com.italankin.lnch.feature.settings.experimental.ExperimentalSettingsFragment;
import com.italankin.lnch.feature.settings.fonts.FontsFragment;
import com.italankin.lnch.feature.settings.hidden_items.HiddenItemsFragment;
import com.italankin.lnch.feature.settings.lookfeel.AppearanceFragment;
import com.italankin.lnch.feature.settings.lookfeel.LookAndFeelFragment;
import com.italankin.lnch.feature.settings.misc.MiscFragment;
import com.italankin.lnch.feature.settings.notifications.NotificationsFragment;
import com.italankin.lnch.feature.settings.preferencesearch.PreferenceSearchFragment;
import com.italankin.lnch.feature.settings.search.SearchFragment;
import com.italankin.lnch.feature.settings.searchstore.SettingsEntry;
import com.italankin.lnch.feature.settings.searchstore.SettingsStore;
import com.italankin.lnch.feature.settings.util.TargetPreference;
import com.italankin.lnch.feature.settings.wallpaper.WallpaperFragment;
import com.italankin.lnch.feature.settings.wallpaper.WallpaperOverlayFragment;
import com.italankin.lnch.feature.settings.widgets.WidgetsSettingsFragment;
import com.italankin.lnch.model.repository.prefs.Preferences;

import java.util.List;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity {

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, SettingsActivity.class);
    }

    public static Intent createIntent(Context context, SettingsEntry.Key key) {
        return new Intent(context, SettingsActivity.class)
                .putExtra(EXTRA_SETTING_KEY, key);
    }

    private static final String EXTRA_SETTING_KEY = "setting_key";

    private static final String REQUEST_KEY_SETTINGS = "settings";
    private static final String REQUEST_KEY_PROXY_PREFIX = "proxy:";

    private Toolbar toolbar;
    private FragmentManager fragmentManager;

    private SettingsStore settingsStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Preferences preferences = LauncherApp.daggerService.main().preferences();
        SupportsOrientationDelegate.attach(this, preferences);

        super.onCreate(savedInstanceState);

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this::updateToolbar);

        settingsStore = LauncherApp.daggerService.main().settingsStore();

        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.settings_title);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        new FragmentResultManager(getSupportFragmentManager(), this, REQUEST_KEY_SETTINGS)
                .register(new SettingsRootFragment.ShowPreferenceSearch(), result -> {
                    showFragment(PreferenceSearchFragment.newInstance(REQUEST_KEY_SETTINGS));
                })
                .register(new PreferenceSearchFragment.ClosePreferenceSearchContract(), result -> {
                    fragmentManager.popBackStack();
                })
                .register(new PreferenceSearchFragment.ShowPreferenceContract(), result -> {
                    handleShowPreference(result);
                })
                .register(new SettingsRootFragment.LaunchEditModeContract(), result -> {
                    finish();
                    startActivity(new Intent(LauncherIntents.ACTION_EDIT_MODE));
                })
                .register(new SettingsRootFragment.ShowSearchPreferencesContract(), result -> {
                    showFragment(new SearchFragment());
                })
                .register(new SettingsRootFragment.ShowAppsSettings(), result -> {
                    showFragment(AppsSettingsFragment.newInstance(REQUEST_KEY_SETTINGS));
                })
                .register(new SettingsRootFragment.ShowShortcutsPreferences(), result -> {
                    showFragment(new ShortcutsFragment());
                })
                .register(new SettingsRootFragment.ShowNotificationsPreferences(), result -> {
                    showFragment(new NotificationsFragment());
                })
                .register(new SettingsRootFragment.ShowLookAndFeelPreferences(), result -> {
                    showFragment(LookAndFeelFragment.newInstance(REQUEST_KEY_SETTINGS));
                })
                .register(new SettingsRootFragment.ShowMiscPreferences(), result -> {
                    showFragment(MiscFragment.newInstance(REQUEST_KEY_SETTINGS));
                })
                .register(new SettingsRootFragment.ShowWidgetPreferences(), result -> {
                    showFragment(new WidgetsSettingsFragment());
                })
                .register(new SettingsRootFragment.ShowWallpaperPreferences(), result -> {
                    showFragment(WallpaperFragment.newInstance(REQUEST_KEY_SETTINGS));
                })
                .register(new SettingsRootFragment.ShowBackupPreferences(), result -> {
                    showFragment(new BackupFragment());
                })
                .register(new AppearanceFragment.AppearanceFinishedContract(), result -> {
                    fragmentManager.popBackStack();
                })
                .register(new AppearanceFragment.ShowFontSelectContract(), this::handleShowFontSelect)
                .register(new WallpaperFragment.ShowWallpaperOverlay(), result -> {
                    showFragment(WallpaperOverlayFragment.newInstance(REQUEST_KEY_SETTINGS));
                })
                .register(new WallpaperOverlayFragment.WallpaperOverlayFinishContract(), result -> {
                    fragmentManager.popBackStack();
                })
                .register(new LookAndFeelFragment.ShowItemLookPreferencesContract(), result -> {
                    showFragment(AppearanceFragment.newInstance(REQUEST_KEY_SETTINGS));
                })
                .register(new AppsSettingsFragment.ShowAppDetailsContract(), descriptorId -> {
                    showFragment(AppDetailsFragment.newInstance(REQUEST_KEY_SETTINGS, descriptorId));
                })
                .register(new MiscFragment.ShowHiddenItems(), result -> {
                    showFragment(new HiddenItemsFragment());
                })
                .register(new MiscFragment.ShowExperimentalPreferencesContract(), result -> {
                    showFragment(new ExperimentalSettingsFragment());
                })
                .register(new AppDetailsFragment.ShowAppAliasesContract(), descriptorId -> {
                    showFragment(AppAliasesFragment.newInstance(descriptorId));
                })
                .register(new AppDetailsFragment.AppDetailsErrorContract(), result -> {
                    fragmentManager.popBackStack();
                })
                .attach();

        if (savedInstanceState == null) {
            fragmentManager
                    .beginTransaction()
                    .add(R.id.container, SettingsRootFragment.newInstance(REQUEST_KEY_SETTINGS))
                    .commit();
        }

        handleShowPreferenceIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleShowPreferenceIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateToolbar();
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = fragmentManager.findFragmentById(R.id.container);
        if (fragment instanceof BackButtonHandler && !((BackButtonHandler) fragment).onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    private void handleShowPreference(SettingsEntry.Key key) {
        SettingsEntry entry = settingsStore.getByKey(key);
        if (entry == null) {
            Timber.w("no preference found for key=% in SettingsStore", key);
            return;
        }
        while (fragmentManager.popBackStackImmediate()) {
            // remove all from backstack
        }
        List<Fragment> fragments = entry.stackBuilder().createStack(REQUEST_KEY_SETTINGS);
        for (int i = 0; i < fragments.size() - 1; i++) {
            Fragment fragment = fragments.get(i);
            showFragment(fragment);
        }
        Fragment fragment = fragments.get(fragments.size() - 1);
        TargetPreference.set(this, fragment, key);
        showFragment(fragment);
    }

    private void updateToolbar() {
        Fragment fragment = fragmentManager.findFragmentById(R.id.container);
        if (fragment instanceof SettingsToolbarTitle) {
            toolbar.setTitle(((SettingsToolbarTitle) fragment).getToolbarTitle(this));
        } else {
            toolbar.setTitle(R.string.settings_title);
        }
        toolbar.setNavigationIcon(fragmentManager.getBackStackEntryCount() > 0
                ? R.drawable.ic_arrow_back
                : R.drawable.ic_close);
    }

    private void showFragment(Fragment fragment) {
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.animator.fragment_in, R.animator.fragment_out,
                        R.animator.fragment_bs_in, R.animator.fragment_bs_out)
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void handleShowFontSelect(String targetRequestKey) {
        String proxyRequestKey = REQUEST_KEY_PROXY_PREFIX + targetRequestKey;
        fragmentManager.setFragmentResultListener(proxyRequestKey, this, (requestKey, result) -> {
            String resultKey = result.getString(FragmentResultContract.RESULT_KEY);
            String onFontSelectedKey = new FontsFragment.OnFontSelected().key();
            if (onFontSelectedKey.equals(resultKey)) {
                fragmentManager.popBackStack();
            }
            fragmentManager.setFragmentResult(targetRequestKey, result);
        });
        showFragment(FontsFragment.newInstance(proxyRequestKey));
    }

    private void handleShowPreferenceIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_SETTING_KEY)) {
            SettingsEntry.Key key = (SettingsEntry.Key) intent.getSerializableExtra(EXTRA_SETTING_KEY);
            handleShowPreference(key);
            intent.removeExtra(EXTRA_SETTING_KEY);
        }
    }
}
