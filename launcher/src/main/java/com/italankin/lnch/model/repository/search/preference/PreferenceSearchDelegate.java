package com.italankin.lnch.model.repository.search.preference;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.italankin.lnch.R;
import com.italankin.lnch.feature.settings.SettingsActivity;
import com.italankin.lnch.feature.settings.searchstore.SettingsEntry;
import com.italankin.lnch.feature.settings.searchstore.SettingsStore;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.model.repository.search.SearchDelegate;
import com.italankin.lnch.model.repository.search.match.Match;
import com.italankin.lnch.util.ResUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PreferenceSearchDelegate implements SearchDelegate {

    private static final int MAX_RESULTS = 3;

    private final SettingsStore settingsStore;

    public PreferenceSearchDelegate(SettingsStore settingsStore) {
        this.settingsStore = settingsStore;
    }

    @Override
    public List<Match> search(String query, EnumSet<Preferences.SearchTarget> searchTargets) {
        if (!searchTargets.contains(Preferences.SearchTarget.PREFERENCE)) {
            return Collections.emptyList();
        }
        List<SettingsEntry> entries = settingsStore.search(query, MAX_RESULTS);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Match> result = new ArrayList<>(entries.size());
        for (SettingsEntry entry : entries) {
            result.add(new PreferenceMatch(entry));
        }
        return result;
    }
}

class PreferenceMatch implements Match {
    private final SettingsEntry entry;

    PreferenceMatch(SettingsEntry entry) {
        this.entry = entry;
    }

    @Override
    public Uri getIcon() {
        return null;
    }

    @Override
    public int getIconResource() {
        return R.drawable.ic_settings;
    }

    @Override
    public CharSequence getLabel(Context context) {
        return context.getString(entry.title());
    }

    @Override
    public int getColor(Context context) {
        return ResUtils.resolveColor(context, R.attr.colorText);
    }

    @Override
    public Intent getIntent(Context context) {
        return SettingsActivity.createIntent(context, entry.key());
    }

    @Override
    public Kind getKind() {
        return Kind.PREFERENCE;
    }

    @Override
    public Set<Action> availableActions() {
        return Collections.emptySet();
    }

    @Override
    public int hashCode() {
        return entry.key().hashCode();
    }
}
