package com.italankin.lnch.feature.settings.backup.impl;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.repository.descriptor.DescriptorRepository;
import com.italankin.lnch.model.repository.store.DescriptorStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import io.reactivex.Completable;
import io.reactivex.Single;
import timber.log.Timber;

public class BackupReaderImpl implements BackupReader {

    private final ContentResolver contentResolver;
    private final Gson gson;
    private final DescriptorRepository descriptorRepository;
    private final DescriptorStore descriptorStore;
    private final PreferencesBackup preferencesBackup;

    public BackupReaderImpl(Context context,
            GsonBuilder gsonBuilder,
            DescriptorRepository descriptorRepository,
            DescriptorStore descriptorStore,
            PreferencesBackup preferencesBackup) {
        this.contentResolver = context.getContentResolver();
        this.gson = gsonBuilder.create();
        this.descriptorRepository = descriptorRepository;
        this.descriptorStore = descriptorStore;
        this.preferencesBackup = preferencesBackup;
    }

    @Override
    public Completable read(Uri uri) {
        return Single
                .fromCallable(() -> {
                    try (InputStreamReader reader = getReader(uri)) {
                        return gson.fromJson(reader, Backup.class);
                    } catch (JsonSyntaxException e) {
                        Timber.e(e, "read:");
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException("Restore failed: " + e.getMessage(), e);
                    }
                })
                .flatMapCompletable(backup -> {
                    Timber.d("restoring from backup\n%s", backup);
                    return Completable.mergeArray(writeDescriptors(backup.descriptors), writePrefs(backup.preferences));
                })
                .onErrorResumeNext(throwable -> {
                    if (!(throwable instanceof JsonSyntaxException)) {
                        return Completable.error(throwable);
                    }
                    return tryRestoreOldBackup(uri);
                });
    }

    private InputStreamReader getReader(Uri uri) throws IOException {
        return new InputStreamReader(new GZIPInputStream(contentResolver.openInputStream(uri), DefaultBufferSize.VALUE));
    }

    private Completable writeDescriptors(List<Descriptor> descriptors) {
        return descriptorRepository.edit()
                .enqueue(new ReplaceAction(descriptors))
                .commit();
    }

    private Completable writePrefs(Map<String, ?> map) {
        return Completable.fromRunnable(() -> preferencesBackup.write(map));
    }

    private Completable tryRestoreOldBackup(Uri uri) throws IOException {
        List<Descriptor> descriptors;
        try (InputStream in = contentResolver.openInputStream(uri)) {
            descriptors = descriptorStore.read(in);
        }
        return descriptorRepository.edit()
                .enqueue(new ReplaceAction(descriptors))
                .commit();
    }

    private static final class ReplaceAction implements DescriptorRepository.Editor.Action {
        private final List<Descriptor> newItems;

        private ReplaceAction(List<Descriptor> newItems) {
            this.newItems = newItems;
        }

        @Override
        public void apply(List<Descriptor> items) {
            items.clear();
            items.addAll(newItems);
        }
    }
}