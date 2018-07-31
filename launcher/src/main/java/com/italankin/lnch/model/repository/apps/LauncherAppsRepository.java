package com.italankin.lnch.model.repository.apps;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.italankin.lnch.BuildConfig;
import com.italankin.lnch.bean.AppItem;
import com.italankin.lnch.bean.AppItem_v1;
import com.italankin.lnch.bean.GroupSeparator;
import com.italankin.lnch.model.provider.Preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class LauncherAppsRepository implements AppsRepository {
    private final Context context;
    private final PackageManager packageManager;
    private final Preferences preferences = new Preferences();
    private final LauncherApps launcherApps;
    private final Completable updater;
    private final BehaviorSubject<List<AppItem>> updatesSubject = BehaviorSubject.create();
    private final Subject<String> packageChangesSubject = PublishSubject.create();
    private final CompositeDisposable disposeBag = new CompositeDisposable();

    public LauncherAppsRepository(Context context, PackageManager packageManager) {
        this.context = context;
        this.packageManager = packageManager;
        launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        updater = loadAll()
                .doOnSuccess(appsData -> {
                    if (appsData.changed) {
                        Timber.d("data has changed, write to disk");
                        writeToDisk(appsData.apps);
                    }
                })
                .map(appsData -> Collections.unmodifiableList(appsData.apps))
                .doOnSuccess(updatesSubject::onNext)
                .doOnError(e -> Timber.e(e, "updater:"))
                .ignoreElement();
        packageChangesSubject
                .doOnNext(Timber::d)
                .debounce(1, TimeUnit.SECONDS)
                .flatMapCompletable(change -> updater.onErrorComplete())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposeBag.add(d);
                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "change:");
                    }
                });
        //noinspection ConstantConditions
        launcherApps.registerCallback(new LauncherCallbacks());
    }

    @Override
    public Completable update() {
        return updater;
    }

    @Override
    public Observable<List<AppItem>> observeApps() {
        return updatesSubject;
    }

    @Override
    public Single<List<AppItem>> fetchApps() {
        return loadAll().map(appsData -> appsData.apps);
    }

    @Override
    public List<AppItem> getApps() {
        return updatesSubject.getValue();
    }

    @Override
    public AppsRepository.Editor edit() {
        return new Editor(updatesSubject.getValue());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////

    private Single<AppsData> loadAll() {
        return Single
                .fromCallable(() -> launcherApps.getActivityList(null, Process.myUserHandle()))
                .flatMap(infoList -> {
                    Single<AppsData> fromList = loadFromList(infoList);
                    return loadFromFile(infoList)
                            .switchIfEmpty(fromList)
                            .onErrorResumeNext(throwable -> {
                                Timber.e(throwable, "loadAll:");
                                return fromList;
                            });
                });
    }

    private Single<AppsData> loadFromList(List<LauncherActivityInfo> infoList) {
        return Single
                .fromCallable(() -> {
                    List<AppItem> apps = new ArrayList<>(16);
                    for (int i = 0, s = infoList.size(); i < s; i++) {
                        apps.add(createItem(infoList.get(i)));
                    }
                    Collections.sort(apps, AppItem.CMP_NAME_ASC);
                    return new AppsData(apps, true);
                });
    }

    private Maybe<AppsData> loadFromFile(List<LauncherActivityInfo> infoList) {
        return Maybe
                .create(emitter -> {
                    if (!getPackagesFile().exists()) {
                        emitter.onComplete();
                        return;
                    }
                    List<AppItem> savedItems;
                    boolean oldVersion = false;
                    try {
                        savedItems = readFromDisk();
                    } catch (JsonSyntaxException e) {
                        savedItems = fromVersion1();
                        oldVersion = true;
                    }
                    if (savedItems != null) {
                        List<AppItem> apps = new ArrayList<>(savedItems.size());
                        List<AppItem> deletedApps = new ArrayList<>(8);
                        Map<String, List<LauncherActivityInfo>> infosByPackageName = infosByPackageName(infoList);
                        for (AppItem item : savedItems) {
                            if (GroupSeparator.ID.equals(item.id)) {
                                apps.add(item);
                                continue;
                            }
                            LauncherActivityInfo info = findInfo(infosByPackageName, item);
                            if (info != null) {
                                int versionCode = getVersionCode(item.id);
                                if (item.versionCode != versionCode) {
                                    item.versionCode = versionCode;
                                    item.label = preferences.label.get(info);
                                    item.color = preferences.color.get(info);
                                }
                                apps.add(item);
                            } else {
                                deletedApps.add(item);
                            }
                        }
                        for (List<LauncherActivityInfo> infos : infosByPackageName.values()) {
                            if (infos.size() == 1) {
                                apps.add(createItem(infos.get(0)));
                            } else {
                                for (LauncherActivityInfo info : infos) {
                                    AppItem item = createItem(info);
                                    item.componentName = getComponentName(info);
                                    apps.add(item);
                                }
                            }
                        }
                        boolean changed = oldVersion || !deletedApps.isEmpty() || !infosByPackageName.isEmpty();
                        emitter.onSuccess(new AppsData(apps, changed));
                    }
                    emitter.onComplete();
                });
    }

    private Map<String, List<LauncherActivityInfo>> infosByPackageName(List<LauncherActivityInfo> infoList) {
        Map<String, List<LauncherActivityInfo>> infosByPackageName = new HashMap<>(infoList.size());
        for (LauncherActivityInfo info : infoList) {
            String packageName = info.getApplicationInfo().packageName;
            List<LauncherActivityInfo> list = infosByPackageName.get(packageName);
            if (list == null) {
                list = new ArrayList<>(1);
                infosByPackageName.put(packageName, list);
            }
            list.add(info);
        }
        return infosByPackageName;
    }

    private LauncherActivityInfo findInfo(Map<String, List<LauncherActivityInfo>> map, AppItem item) {
        List<LauncherActivityInfo> infos = map.get(item.id);
        if (infos == null) {
            return null;
        }
        LauncherActivityInfo result = null;
        if (infos.size() == 1) {
            result = infos.remove(0);
        } else if (item.componentName != null) {
            for (LauncherActivityInfo info : infos) {
                String componentName = getComponentName(info);
                if (componentName.equals(item.componentName)) {
                    result = info;
                    break;
                }
            }
        }
        if (result != null) {
            infos.remove(result);
            if (infos.isEmpty()) {
                map.remove(item.id);
            }
        }
        return result;
    }

    private AppItem createItem(LauncherActivityInfo info) {
        String packageName = info.getApplicationInfo().packageName;
        AppItem item = new AppItem(packageName);
        item.versionCode = getVersionCode(packageName);
        item.label = preferences.label.get(info);
        item.color = preferences.color.get(info);
        return item;
    }

    private int getVersionCode(String packageName) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private String getComponentName(LauncherActivityInfo info) {
        return info.getComponentName().flattenToString();
    }

    private void writeToDisk(List<AppItem> apps) {
        Timber.d("writeToDisk");
        try {
            FileWriter fw = new FileWriter(getPackagesFile());
            try {
                GsonBuilder builder = new GsonBuilder();
                if (BuildConfig.DEBUG) {
                    builder.setPrettyPrinting();
                }
                Gson gson = builder.create();
                String json = gson.toJson(apps);
                fw.write(json);
            } finally {
                fw.close();
            }
        } catch (IOException e) {
            Timber.e(e, "writeToDisk:");
        }
    }

    private List<AppItem> readFromDisk() {
        Timber.d("readFromDisk");
        Gson gson = new Gson();
        Type type = new TypeToken<List<AppItem>>() {
        }.getType();
        try {
            return gson.fromJson(new FileReader(getPackagesFile()), type);
        } catch (JsonSyntaxException e) {
            return fromVersion1();
        } catch (Exception e) {
            Timber.e(e, "readFromDisk:");
            return null;
        }
    }

    private List<AppItem> fromVersion1() {
        Timber.d("fromVersion1");
        try {
            Map<String, AppItem_v1> map = new Gson().fromJson(new FileReader(getPackagesFile()),
                    new TypeToken<Map<String, AppItem_v1>>() {
                    }.getType());
            List<AppItem_v1> appItems_v1 = new ArrayList<>(map.size());
            for (Map.Entry<String, AppItem_v1> entry : map.entrySet()) {
                AppItem_v1 item = entry.getValue();
                item.packageName = entry.getKey();
                appItems_v1.add(item);
            }
            Collections.sort(appItems_v1);
            List<AppItem> appItems = new ArrayList<>(map.size());
            for (AppItem_v1 item : appItems_v1) {
                appItems.add(item.toAppItem());
            }
            createBackup(map);
            return appItems;
        } catch (FileNotFoundException e) {
            Timber.e(e, "fromVersion1:");
            return null;
        }
    }

    private static void createBackup(Map<String, AppItem_v1> map) {
        try {
            FileWriter fileWriter = new FileWriter("packages.json.backup");
            new Gson().toJson(map, new TypeToken<Map<String, AppItem_v1>>() {
            }.getType(), fileWriter);
            fileWriter.close();
        } catch (Exception ignored) {
        }
    }

    private File getPackagesFile() {
        return new File(context.getFilesDir(), "packages.json");
    }

    private static class AppsData {
        final List<AppItem> apps;
        final boolean changed;

        public AppsData(List<AppItem> apps, boolean changed) {
            this.apps = apps;
            this.changed = changed;
        }
    }

    final class LauncherCallbacks extends LauncherApps.Callback {
        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            notify(user, "package removed");
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            notify(user, "package added");
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            notify(user, "package changed");
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            notify(user, "packages available");
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            notify(user, "packages unavailable");
        }

        private void notify(UserHandle user, String s) {
            if (Process.myUserHandle().equals(user)) {
                packageChangesSubject.onNext(s);
            }
        }
    }

    final class Editor implements AppsRepository.Editor {
        private final Queue<AppsRepository.Editor.Action> actions = new ArrayDeque<>();
        private final List<AppItem> apps;
        private volatile boolean used;

        Editor(List<AppItem> apps) {
            this.apps = apps;
        }

        @Override
        public Editor enqueue(AppsRepository.Editor.Action action) {
            if (used) {
                throw new IllegalStateException();
            }
            actions.offer(action);
            return this;
        }

        @Override
        public Completable commit() {
            if (used) {
                throw new IllegalStateException();
            }
            Consumer<Disposable> onSubscribe = d -> used = true;
            if (actions.isEmpty()) {
                Timber.d("commit: no actions");
                return Completable.complete()
                        .doOnSubscribe(onSubscribe);
            }
            Timber.d("commit: apply actions");
            return Single
                    .fromCallable(() -> {
                        List<AppItem> result = new ArrayList<>(apps);
                        Iterator<AppsRepository.Editor.Action> iter = actions.iterator();
                        while (iter.hasNext()) {
                            iter.next().apply(result);
                            iter.remove();
                        }
                        return result;
                    })
                    .doOnSubscribe(onSubscribe)
                    .doOnSuccess(LauncherAppsRepository.this::writeToDisk)
                    .ignoreElement();
        }
    }
}
