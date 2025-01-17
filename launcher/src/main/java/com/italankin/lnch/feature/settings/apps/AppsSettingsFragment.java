package com.italankin.lnch.feature.settings.apps;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.italankin.lnch.LauncherApp;
import com.italankin.lnch.R;
import com.italankin.lnch.feature.base.AppFragment;
import com.italankin.lnch.feature.home.fragmentresult.DescriptorFragmentResultContract;
import com.italankin.lnch.feature.settings.SettingsToolbarTitle;
import com.italankin.lnch.feature.settings.apps.adapter.AppsSettingsAdapter;
import com.italankin.lnch.feature.settings.apps.adapter.AppsSettingsFilter;
import com.italankin.lnch.feature.settings.apps.dialog.FilterFlagsDialogFragment;
import com.italankin.lnch.feature.settings.apps.model.FilterFlag;
import com.italankin.lnch.model.ui.impl.AppDescriptorUi;
import me.italankin.adapterdelegates.CompositeAdapter;
import com.italankin.lnch.util.filter.ListFilter;
import com.italankin.lnch.util.imageloader.ImageLoader;
import com.italankin.lnch.util.widget.LceLayout;

import java.util.EnumSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

public class AppsSettingsFragment extends AppFragment implements AppsSettingsView,
        AppsSettingsAdapter.Listener,
        FilterFlagsDialogFragment.Listener,
        ListFilter.OnFilterResult<AppDescriptorUi>,
        SettingsToolbarTitle {

    public static AppsSettingsFragment newInstance(String requestKey) {
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        AppsSettingsFragment fragment = new AppsSettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private static final String DATA_FILTER_FLAGS = "filter_flags";

    private static final String TAG_FILTER_FLAGS = "filter";

    @InjectPresenter
    AppsSettingsPresenter presenter;

    private LceLayout lce;
    private RecyclerView list;
    private CompositeAdapter<AppDescriptorUi> adapter;

    private final AppsSettingsFilter filter = new AppsSettingsFilter(this);

    @ProvidePresenter
    AppsSettingsPresenter providePresenter() {
        return LauncherApp.daggerService.presenters().appsSettings();
    }

    @Override
    public CharSequence getToolbarTitle(Context context) {
        return context.getString(R.string.settings_apps_list);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_items_list, container, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        list = view.findViewById(R.id.list);
        lce = view.findViewById(R.id.lce);
        initAdapter();
        addListDivider();
        if (savedInstanceState != null) {
            EnumSet<FilterFlag> flags = (EnumSet<FilterFlag>)
                    savedInstanceState.getSerializable(DATA_FILTER_FLAGS);
            if (flags != null) {
                filter.setFlags(flags);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(DATA_FILTER_FLAGS, filter.getFlags());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
        list = null;
        lce = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings_apps, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setQueryHint(getString(R.string.hint_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter.filter(newText);
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            showFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showLoading() {
        lce.showLoading();
    }

    @Override
    public void onAppsUpdated(List<AppDescriptorUi> apps) {
        filter.setDataset(apps);
        lce.showContent();
    }

    @Override
    public void showError(Throwable e) {
        lce.error()
                .button(v -> presenter.observeApps())
                .message(e.getMessage())
                .show();
    }

    @Override
    public void onVisibilityClick(int position, AppDescriptorUi item) {
        presenter.toggleAppVisibility(item);
    }

    @Override
    public void onAppClick(int position, AppDescriptorUi item) {
        sendResult(new ShowAppDetailsContract().result(item.getDescriptor().getId()));
    }

    @Override
    public void onFilterResult(String query, List<AppDescriptorUi> items) {
        if (adapter == null) {
            return;
        }
        if (items.isEmpty()) {
            String message = TextUtils.isEmpty(query)
                    ? getString(R.string.search_empty)
                    : getString(R.string.search_placeholder, query);
            lce.empty()
                    .message(message)
                    .show();
        } else {
            adapter.setDataset(items);
            adapter.notifyDataSetChanged();
            lce.showContent();
        }
    }

    @Override
    public void onFlagsSet(EnumSet<FilterFlag> newFlags) {
        filter.setFlags(newFlags);
    }

    @Override
    public void onFlagsReset() {
        filter.resetFlags();
    }

    private void initAdapter() {
        Context context = requireContext();
        ImageLoader imageLoader = LauncherApp.daggerService.main().imageLoader();
        adapter = new CompositeAdapter.Builder<AppDescriptorUi>(context)
                .add(new AppsSettingsAdapter(imageLoader, this))
                .recyclerView(list)
                .setHasStableIds(true)
                .create();
    }

    @SuppressWarnings("ConstantConditions")
    private void addListDivider() {
        Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.settings_list_divider);
        DividerItemDecoration decoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        decoration.setDrawable(drawable);
        list.addItemDecoration(decoration);
    }

    private void showFilterDialog() {
        new FilterFlagsDialogFragment.Builder()
                .setFlags(filter.getFlags())
                .build()
                .show(getChildFragmentManager(), TAG_FILTER_FLAGS);
    }

    public static class ShowAppDetailsContract extends DescriptorFragmentResultContract {
        public ShowAppDetailsContract() {
            super("show_app_details");
        }
    }
}
