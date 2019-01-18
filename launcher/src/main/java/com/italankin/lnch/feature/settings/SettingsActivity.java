package com.italankin.lnch.feature.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.italankin.lnch.LauncherApp;
import com.italankin.lnch.R;
import com.italankin.lnch.feature.home.HomeActivity;
import com.italankin.lnch.feature.settings.apps.AppsFragment;
import com.italankin.lnch.feature.settings.backup.BackupFragment;
import com.italankin.lnch.feature.settings.base.SimplePreferencesFragment;
import com.italankin.lnch.feature.settings.itemlook.ItemLookFragment;
import com.italankin.lnch.feature.settings.wallpaper.WallpaperFragment;
import com.italankin.lnch.feature.settings.wallpaper.WallpaperOverlayFragment;
import com.italankin.lnch.model.repository.prefs.Preferences;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SettingsActivity extends AppCompatActivity implements
        SettingsRootFragment.Callbacks,
        ItemLookFragment.Callbacks,
        WallpaperFragment.Callbacks,
        WallpaperOverlayFragment.Callbacks {

    public static Intent getStartIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    private Toolbar toolbar;
    private FragmentManager fragmentManager;
    private Disposable screenOrientationDisposable;
    private Preferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = LauncherApp.daggerService.main().getPreferences();

        setScreenOrientation();
        setTheme();

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this::updateToolbar);

        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (savedInstanceState == null) {
            fragmentManager
                    .beginTransaction()
                    .add(R.id.container, new SettingsRootFragment())
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateToolbar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (screenOrientationDisposable != null && !screenOrientationDisposable.isDisposed()) {
            screenOrientationDisposable.dispose();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = fragmentManager.findFragmentById(R.id.container);
        if (fragment instanceof BackButtonHandler && !((BackButtonHandler) fragment).onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void launchEditMode() {
        finish();
        startActivity(new Intent(HomeActivity.ACTION_EDIT_MODE));
    }

    @Override
    public void showSearchPreferences() {
        showFragment(SimplePreferencesFragment.newInstance(R.xml.prefs_search), R.string.title_settings_search);
    }

    @Override
    public void showAppsPreferences() {
        showFragment(new AppsFragment(), R.string.title_settings_apps_list);
    }

    @Override
    public void showItemLookPreferences() {
        showFragment(new ItemLookFragment(), R.string.title_settings_home_item_look);
    }

    @Override
    public void showMiscPreferences() {
        showFragment(SimplePreferencesFragment.newInstance(R.xml.prefs_misc), R.string.title_settings_home_misc);
    }

    @Override
    public void showWallpaperPreferences() {
        showFragment(new WallpaperFragment(), R.string.title_settings_wallpaper);
    }

    @Override
    public void showBackupPreferences() {
        showFragment(new BackupFragment(), R.string.title_settings_backups);
    }

    @Override
    public void onItemLookFinish() {
        fragmentManager.popBackStack();
    }

    @Override
    public void showWallpaperOverlayPreferences() {
        showFragment(new WallpaperOverlayFragment(), R.string.title_settings_wallpaper_overlay_color);
    }

    @Override
    public void onWallpaperOverlayFinish() {
        fragmentManager.popBackStack();
    }

    private void updateToolbar() {
        toolbar.setTitle(getFragmentTitle());
        toolbar.setNavigationIcon(fragmentManager.getBackStackEntryCount() > 0
                ? R.drawable.ic_arrow_back
                : R.drawable.ic_close);
    }

    private CharSequence getFragmentTitle() {
        int index = fragmentManager.getBackStackEntryCount() - 1;
        return index < 0
                ? getString(R.string.title_settings)
                : fragmentManager.getBackStackEntryAt(index).getBreadCrumbTitle();
    }

    private void showFragment(Fragment fragment, @StringRes int title) {
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.animator.fragment_in, R.animator.fragment_out,
                        R.animator.fragment_in, R.animator.fragment_out)
                .replace(R.id.container, fragment)
                .setBreadCrumbTitle(title)
                .addToBackStack(null)
                .commit();
    }

    private void setScreenOrientation() {
        setRequestedOrientation(preferences.screenOrientation().value());
        String key = getString(R.string.pref_misc_screen_orientation);
        screenOrientationDisposable = preferences.observe()
                .filter(key::equals)
                .map(s -> preferences.screenOrientation().value())
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setRequestedOrientation);
    }

    private void setTheme() {
        switch (preferences.colorTheme()) {
            case DARK:
                setTheme(R.style.AppTheme_Dark_Preferences);
                break;
            case LIGHT:
                setTheme(R.style.AppTheme_Light_Preferences);
                break;
        }
    }
}
