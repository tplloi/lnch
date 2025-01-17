package com.italankin.lnch.feature.settings.apps.adapter;

import android.text.TextUtils;

import com.italankin.lnch.feature.settings.apps.model.FilterFlag;
import com.italankin.lnch.model.ui.impl.AppDescriptorUi;
import com.italankin.lnch.util.SearchUtils;
import com.italankin.lnch.util.filter.ListFilter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import androidx.annotation.Nullable;

import static java.util.Collections.synchronizedSet;
import static java.util.Collections.unmodifiableSet;

public class AppsSettingsFilter extends ListFilter<AppDescriptorUi> {
    private static final Set<FilterFlag> DEFAULT_FLAGS = unmodifiableSet(EnumSet.allOf(FilterFlag.class));

    private final Set<FilterFlag> flags = synchronizedSet(EnumSet.copyOf(DEFAULT_FLAGS));

    public AppsSettingsFilter(@Nullable OnFilterResult<AppDescriptorUi> onFilterResult) {
        super(onFilterResult);
    }

    public void setFlags(Set<FilterFlag> newFlags) {
        if (!flags.equals(newFlags)) {
            flags.clear();
            flags.addAll(newFlags);
            fireFilter();
        }
    }

    public void resetFlags() {
        setFlags(DEFAULT_FLAGS);
    }

    public EnumSet<FilterFlag> getFlags() {
        return flags.isEmpty() ? EnumSet.noneOf(FilterFlag.class) : EnumSet.copyOf(flags);
    }

    @Override
    protected FilterResults performFiltering(String query, List<AppDescriptorUi> unfiltered) {
        boolean includeVisible = flags.contains(FilterFlag.VISIBLE);
        boolean includeIgnored = flags.contains(FilterFlag.IGNORED);

        List<AppDescriptorUi> result = new ArrayList<>(unfiltered.size());

        for (AppDescriptorUi item : unfiltered) {
            if (!item.isIgnored() && !includeVisible) {
                continue;
            }
            if (item.isIgnored() && !includeIgnored) {
                continue;
            }
            result.add(item);
        }
        if (TextUtils.isEmpty(constraint)) {
            return of(result);
        }
        Iterator<AppDescriptorUi> iterator = result.iterator();
        while (iterator.hasNext()) {
            AppDescriptorUi item = iterator.next();
            if (!SearchUtils.contains(item.getDescriptor().label, query) &&
                    !SearchUtils.contains(item.getCustomLabel(), query) &&
                    !SearchUtils.contains(item.packageName, query)) {
                iterator.remove();
            }
        }
        return of(result);
    }

    protected void fireFilter() {
        if (!TextUtils.isEmpty(constraint) || !DEFAULT_FLAGS.equals(flags)) {
            filter(constraint);
        } else {
            publishResults(null, of(new ArrayList<>(unfiltered)));
        }
    }
}
