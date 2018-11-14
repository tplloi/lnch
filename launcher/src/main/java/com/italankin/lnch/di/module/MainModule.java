package com.italankin.lnch.di.module;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.gson.GsonBuilder;
import com.italankin.lnch.BuildConfig;
import com.italankin.lnch.model.repository.apps.AppsRepository;
import com.italankin.lnch.model.repository.apps.LauncherAppsRepository;
import com.italankin.lnch.model.repository.descriptor.BackupDescriptorStore;
import com.italankin.lnch.model.repository.descriptor.DescriptorStore;
import com.italankin.lnch.model.repository.descriptor.VersioningDescriptorStore;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.model.repository.prefs.SeparatorState;
import com.italankin.lnch.model.repository.prefs.SeparatorStateImpl;
import com.italankin.lnch.model.repository.prefs.UserPreferences;
import com.italankin.lnch.model.repository.search.SearchDelegate;
import com.italankin.lnch.model.repository.search.SearchRepository;
import com.italankin.lnch.model.repository.search.SearchRepositoryImpl;
import com.italankin.lnch.model.repository.search.delegate.AppSearchDelegate;
import com.italankin.lnch.model.repository.search.delegate.DeepShortcutSearchDelegate;
import com.italankin.lnch.model.repository.search.delegate.PinnedShortcutSearchDelegate;
import com.italankin.lnch.model.repository.shortcuts.AppShortcutsRepository;
import com.italankin.lnch.model.repository.shortcuts.ShortcutsRepository;
import com.italankin.lnch.model.repository.shortcuts.StubShortcutsRepository;
import com.italankin.lnch.util.picasso.PicassoFactory;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

@Module
public class MainModule {

    @Provides
    @Singleton
    public PackageManager providePackageManager(Context context) {
        return context.getPackageManager();
    }

    @Provides
    @Singleton
    public Preferences providePreferences(Context context) {
        return new UserPreferences(context);
    }

    @Provides
    @Singleton
    public AppsRepository provideAppsRepository(Context context, PackageManager packageManager,
            DescriptorStore descriptorStore, ShortcutsRepository shortcutsRepository,
            Preferences preferences) {
        return new LauncherAppsRepository(context, packageManager, descriptorStore,
                shortcutsRepository, preferences);
    }

    @Provides
    @Singleton
    public DescriptorStore provideDescriptorStore() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        if (BuildConfig.DEBUG) {
            gsonBuilder.setPrettyPrinting();
        }
        return new BackupDescriptorStore(new VersioningDescriptorStore(gsonBuilder));
    }

    @Provides
    @Singleton
    public SearchRepository provideSearchRepository(PackageManager packageManager,
            AppsRepository appsRepository, ShortcutsRepository shortcutsRepository, Preferences preferences) {
        List<SearchDelegate> delegates = Arrays.asList(
                new AppSearchDelegate(packageManager, appsRepository),
                new DeepShortcutSearchDelegate(appsRepository, shortcutsRepository),
                new PinnedShortcutSearchDelegate(appsRepository)
        );
        return new SearchRepositoryImpl(delegates, preferences);
    }

    @Provides
    @Singleton
    public PicassoFactory providePicassoFactory(Context context) {
        return new PicassoFactory(context);
    }

    @Provides
    @Singleton
    public ShortcutsRepository.DescriptorProvider provideAppsProvider(Lazy<AppsRepository> lazy) {
        return () -> lazy.get().items();
    }

    @Provides
    @Singleton
    public ShortcutsRepository provideShortcutsRepository(Context context, ShortcutsRepository.DescriptorProvider descriptorProvider) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            return new AppShortcutsRepository(context, descriptorProvider);
        } else {
            return new StubShortcutsRepository();
        }
    }

    @Provides
    @Singleton
    public SeparatorState provideSeparatorState(Context context) {
        return new SeparatorStateImpl(context);
    }
}
