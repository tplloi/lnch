package com.italankin.lnch.model.repository.shortcuts.backport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;

import com.italankin.lnch.model.descriptor.impl.AppDescriptor;
import com.italankin.lnch.model.descriptor.impl.DeepShortcutDescriptor;
import com.italankin.lnch.model.descriptor.impl.IntentDescriptor;
import com.italankin.lnch.model.repository.descriptor.DescriptorRepository;
import com.italankin.lnch.model.repository.descriptor.NameNormalizer;
import com.italankin.lnch.model.repository.descriptor.actions.AddAction;
import com.italankin.lnch.model.repository.shortcuts.Shortcut;
import com.italankin.lnch.model.repository.shortcuts.ShortcutsRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public class BackportShortcutsRepository implements ShortcutsRepository {

    private final Context context;
    private final PackageManager packageManager;
    private final LauncherApps launcherApps;
    private final Lazy<DescriptorRepository> descriptorRepository;
    private final NameNormalizer nameNormalizer;

    private final ConcurrentHashMap<String, List<ShortcutBackport>> shortcutsCache = new ConcurrentHashMap<>();

    public BackportShortcutsRepository(Context context, Lazy<DescriptorRepository> descriptorRepository,
            NameNormalizer nameNormalizer) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        this.launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        this.descriptorRepository = descriptorRepository;
        this.nameNormalizer = nameNormalizer;
    }

    @Override
    public Completable loadShortcuts() {
        return Observable
                .defer(() -> Observable.fromIterable(descriptorRepository.get().itemsOfType(AppDescriptor.class)))
                .collectInto(new HashMap<String, List<ShortcutBackport>>(), (map, descriptor) -> {
                    map.put(descriptor.getId(), queryShortcuts(descriptor));
                })
                .doOnSuccess(result -> {
                    shortcutsCache.clear();
                    shortcutsCache.putAll(result);
                })
                .ignoreElement();
    }

    @Override
    public List<Shortcut> getShortcuts(AppDescriptor descriptor) {
        List<ShortcutBackport> list = shortcutsCache.get(descriptor.getId());
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    @Override
    public Completable loadShortcuts(AppDescriptor descriptor) {
        return Single.fromCallable(() -> queryShortcuts(descriptor))
                .doOnSuccess(list -> shortcutsCache.put(descriptor.getId(), list))
                .ignoreElement();
    }

    @Override
    public Shortcut getShortcut(String packageName, String shortcutId) {
        for (String key : shortcutsCache.keySet()) {
            if (!key.contains(packageName)) {
                continue;
            }
            List<ShortcutBackport> list = shortcutsCache.get(key);
            if (list == null) {
                return null;
            }
            for (Shortcut shortcut : list) {
                if (shortcut.getId().equals(shortcutId)) {
                    return shortcut;
                }
            }
        }
        return null;
    }

    @Override
    public Single<Boolean> pinShortcut(Shortcut shortcut) {
        if (shortcut instanceof ShortcutBackport) {
            return Single.defer(() -> {
                ShortcutBackport sb = (ShortcutBackport) shortcut;
                DescriptorRepository descriptorRepository = this.descriptorRepository.get();
                List<DeepShortcutDescriptor> deepShortcuts = descriptorRepository
                        .itemsOfType(DeepShortcutDescriptor.class);
                String packageName = sb.getPackageName();
                String id = sb.getId();
                for (DeepShortcutDescriptor deepShortcut : deepShortcuts) {
                    if (deepShortcut.packageName.equals(packageName) && deepShortcut.id.equals(id)) {
                        return Single.just(false);
                    }
                }
                Intent intent = ShortcutBackport.stripPackage(sb.getIntent());
                IntentDescriptor descriptor = new IntentDescriptor(intent, sb.getShortLabel().toString());
                descriptor.label = nameNormalizer.normalize(sb.getShortLabel());
                return descriptorRepository.edit()
                        .enqueue(new AddAction(descriptor))
                        .commit()
                        .toSingleDefault(true);
            });
        } else {
            return Single.error(new IllegalArgumentException("Unknown shortcut type"));
        }
    }

    @Override
    public Single<Boolean> pinShortcut(String packageName, String shortcutId) {
        Shortcut shortcut = getShortcut(packageName, shortcutId);
        if (shortcut == null) {
            return Single.error(new NullPointerException("Shortcut not found"));
        }
        return pinShortcut(shortcut);
    }

    private List<ShortcutBackport> queryShortcuts(AppDescriptor descriptor) {
        return DeepShortcutManagerBackport.getForPackage(context, packageManager, launcherApps,
                getComponentName(descriptor), descriptor.packageName);
    }

    private static ComponentName getComponentName(AppDescriptor descriptor) {
        return descriptor.componentName != null
                ? ComponentName.unflattenFromString(descriptor.componentName)
                : null;
    }
}
