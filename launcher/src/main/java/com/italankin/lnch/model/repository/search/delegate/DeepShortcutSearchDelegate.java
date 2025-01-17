package com.italankin.lnch.model.repository.search.delegate;

import android.os.Build;

import com.italankin.lnch.feature.receiver.StartShortcutReceiver;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.impl.AppDescriptor;
import com.italankin.lnch.model.descriptor.impl.DeepShortcutDescriptor;
import com.italankin.lnch.model.repository.descriptor.DescriptorRepository;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.model.repository.search.SearchDelegate;
import com.italankin.lnch.model.repository.search.match.Match;
import com.italankin.lnch.model.repository.search.match.PartialDescriptorMatch;
import com.italankin.lnch.model.repository.search.match.PartialMatch;
import com.italankin.lnch.model.repository.shortcuts.Shortcut;
import com.italankin.lnch.model.repository.shortcuts.ShortcutsRepository;
import com.italankin.lnch.util.imageloader.resourceloader.ShortcutIconLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.RequiresApi;

import static com.italankin.lnch.util.SearchUtils.contains;

public class DeepShortcutSearchDelegate implements SearchDelegate {

    private final DescriptorRepository descriptorRepository;
    private final ShortcutsRepository shortcutsRepository;

    private volatile int stateKey = 0;
    private volatile Set<ShortcutData> shortcutData;

    public DeepShortcutSearchDelegate(DescriptorRepository descriptorRepository, ShortcutsRepository shortcutsRepository) {
        this.descriptorRepository = descriptorRepository;
        this.shortcutsRepository = shortcutsRepository;
    }

    @Override
    public List<Match> search(String query, EnumSet<Preferences.SearchTarget> searchTargets) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1
                || !searchTargets.contains(Preferences.SearchTarget.SHORTCUT)) {
            return Collections.emptyList();
        }
        List<Match> result = new ArrayList<>(4);
        for (ShortcutData data : getAllShortcuts()) {
            if (contains(data.shortcut.getShortLabel().toString(), query)) {
                Match match = createMatch(data.shortcut, data.descriptor);
                result.add(match);
            } else {
                PartialMatch.Type type = DescriptorSearchUtils.test(data.descriptor, query);
                if (type != null) {
                    Match match = createMatch(data.shortcut, data.descriptor, type);
                    result.add(match);
                }
            }
        }
        return result;
    }

    private Set<ShortcutData> getAllShortcuts() {
        int newStateKey = descriptorRepository.stateKey();
        if (shortcutData != null && stateKey == newStateKey) {
            return shortcutData;
        }
        Set<ShortcutData> result = new LinkedHashSet<>(64);
        for (DeepShortcutDescriptor descriptor : descriptorRepository.itemsOfType(DeepShortcutDescriptor.class)) {
            Shortcut shortcut = shortcutsRepository.getShortcut(descriptor.packageName, descriptor.id);
            if (shortcut != null && shortcut.isEnabled()) {
                result.add(new ShortcutData(shortcut, descriptor));
            }
        }
        for (AppDescriptor descriptor : descriptorRepository.itemsOfType(AppDescriptor.class)) {
            if ((descriptor.searchFlags & AppDescriptor.FLAG_SEARCH_SHORTCUTS_VISIBLE) == 0) {
                continue;
            }
            List<Shortcut> shortcuts = shortcutsRepository.getShortcuts(descriptor);
            if (shortcuts == null) {
                continue;
            }
            for (Shortcut shortcut : shortcuts) {
                if (shortcut.isEnabled()) {
                    result.add(new ShortcutData(shortcut, descriptor));
                }
            }
        }
        shortcutData = result;
        stateKey = newStateKey;
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private static Match createMatch(Shortcut shortcut, Descriptor descriptor) {
        return createMatch(shortcut, descriptor, PartialMatch.Type.CONTAINS);
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private static Match createMatch(Shortcut shortcut, Descriptor descriptor, PartialMatch.Type type) {
        PartialDescriptorMatch match;
        if (descriptor instanceof DeepShortcutDescriptor) {
            match = new PartialDescriptorMatch((DeepShortcutDescriptor) descriptor, shortcut, type);
        } else {
            match = new PartialDescriptorMatch(descriptor, PartialMatch.Type.CONTAINS, Match.Kind.SHORTCUT);
            match.icon = ShortcutIconLoader.uriFrom(shortcut, true);
            match.label = shortcut.getShortLabel();
            match.intent = StartShortcutReceiver.makeStartIntent(shortcut);
        }
        return match;
    }

    private static class ShortcutData {
        final Shortcut shortcut;
        final Descriptor descriptor;

        ShortcutData(Shortcut shortcut, Descriptor descriptor) {
            this.shortcut = shortcut;
            this.descriptor = descriptor;
        }

        @Override
        public int hashCode() {
            int result = shortcut.getPackageName().hashCode();
            result = 31 * result + shortcut.getId().hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ShortcutData) {
                ShortcutData that = (ShortcutData) obj;
                return this.shortcut.getPackageName().equals(that.shortcut.getPackageName())
                        && this.shortcut.getId().equals(that.shortcut.getId());
            }
            return false;
        }

        @Override
        public String toString() {
            return "ShortcutData{" +
                    "shortcut=" + shortcut.getId() +
                    ", descriptor=" + descriptor.getId() +
                    '}';
        }
    }
}
