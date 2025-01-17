package com.italankin.lnch.feature.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.italankin.lnch.LauncherApp;
import com.italankin.lnch.R;
import com.italankin.lnch.model.descriptor.impl.PinnedShortcutDescriptor;
import com.italankin.lnch.model.repository.descriptor.DescriptorRepository;
import com.italankin.lnch.model.repository.descriptor.NameNormalizer;
import com.italankin.lnch.model.repository.descriptor.actions.AddAction;

import timber.log.Timber;

@SuppressWarnings("deprecation")
public class InstallShortcutReceiver extends BroadcastReceiver {

    private static final String ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent target = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (target == null || !ACTION.equals(target.getAction())) {
            Timber.e("Invalid intent: %s", intent);
            return;
        }

        if (target.getCategories() != null
                && target.getCategories().contains(Intent.CATEGORY_LAUNCHER)
                && Intent.ACTION_MAIN.equals(target.getAction())) {
            // probably initiated by PlayStore
            Timber.e("Ignoring intent: %s", intent);
            return;
        }
        String name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        String uri = target.toUri(0);

        String label = name != null ? name : context.getString(R.string.pinned_shortcut_default_label);
        PinnedShortcutDescriptor descriptor = new PinnedShortcutDescriptor(
                uri, label, context.getColor(R.color.pinned_shortcut_default));
        NameNormalizer nameNormalizer = LauncherApp.daggerService.main().nameNormalizer();
        descriptor.originalLabel = descriptor.label = nameNormalizer.normalize(label);

        DescriptorRepository apps = LauncherApp.daggerService
                .main()
                .descriptorRepository();
        Throwable error = apps.edit()
                .enqueue(new AddAction(descriptor))
                .commit()
                .blockingGet();
        if (error == null) {
            Timber.d("Shortcut added: %s", intent);
        } else {
            Timber.e(error, "Commit changes:");
        }
    }
}
