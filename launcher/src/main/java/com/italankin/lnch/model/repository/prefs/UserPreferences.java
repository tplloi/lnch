package com.italankin.lnch.model.repository.prefs;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class UserPreferences implements Preferences {

    private static final Map<String, Pref<?>> PREFS;

    static {
        PREFS = new HashMap<>(Preferences.ALL.size());
        for (Pref<?> pref : Preferences.ALL) {
            PREFS.put(pref.key(), pref);
        }
    }

    private final SharedPreferences prefs;
    private final PublishSubject<String> removedKeys = PublishSubject.create();
    private final Observable<String> updates;

    @Inject
    public UserPreferences(SharedPreferences sharedPreferences) {
        this.prefs = sharedPreferences;
        this.updates = Observable
                .<String>create(emitter -> {
                    OnSharedPreferenceChangeListener listener = (sp, key) -> {
                        Timber.d("onPrefChanged: key=%s", key);
                        if (!emitter.isDisposed()) {
                            emitter.onNext(key);
                        }
                    };
                    prefs.registerOnSharedPreferenceChangeListener(listener);
                    emitter.setCancellable(() -> prefs.unregisterOnSharedPreferenceChangeListener(listener));
                })
                .mergeWith(removedKeys)
                .debounce(100, TimeUnit.MILLISECONDS)
                .share();
    }

    @Override
    public <T> T get(Pref<T> pref) {
        return pref.fetcher().fetch(prefs, pref.key());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Pref<T> find(String key) {
        return (Pref<T>) PREFS.get(key);
    }

    @Override
    public <T> void set(Pref<T> pref, T newValue) {
        pref.updater().update(prefs, pref.key(), newValue);
    }

    @Override
    public void reset(Pref<?>... prefs) {
        List<String> keys = new ArrayList<>(prefs.length);
        SharedPreferences.Editor editor = this.prefs.edit();
        for (Pref<?> pref : prefs) {
            String key = pref.key();
            editor.remove(key);
            keys.add(key);
        }
        editor.apply();
        for (String key : keys) {
            Timber.d("removed pref: key=%s", key);
            removedKeys.onNext(key);
        }
    }

    @Override
    public Observable<Pref<?>> observe() {
        return updates.map(PREFS::get);
    }

    @Override
    public <T> Observable<T> observe(Pref<T> pref) {
        return observeValue(pref)
                .map(Value::get)
                .startWith(get(pref));
    }

    @Override
    public <T> Observable<Value<T>> observeValue(Pref<T> pref) {
        return updates
                .filter(pref.key()::equals)
                .map(key -> new Value<>(get(pref)));
    }
}
