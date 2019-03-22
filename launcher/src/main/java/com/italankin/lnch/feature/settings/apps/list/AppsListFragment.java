package com.italankin.lnch.feature.settings.apps.list;

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
import com.italankin.lnch.R;
import com.italankin.lnch.feature.base.AppFragment;
import com.italankin.lnch.feature.settings.apps.list.adapter.AppsListFilter;
import com.italankin.lnch.feature.settings.apps.list.adapter.AppsViewModelAdapter;
import com.italankin.lnch.feature.settings.apps.list.dialog.FilterFlagsDialogFragment;
import com.italankin.lnch.feature.settings.apps.list.model.FilterFlag;
import com.italankin.lnch.model.viewmodel.impl.AppViewModel;
import com.italankin.lnch.util.adapterdelegate.CompositeAdapter;
import com.italankin.lnch.util.dialogfragment.ListenerFragment;
import com.italankin.lnch.util.widget.LceLayout;
import com.squareup.picasso.Picasso;

import java.util.EnumSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

public class AppsListFragment extends AppFragment implements AppsListView,
        AppsViewModelAdapter.Listener,
        AppsListFilter.OnFilterResult {

    private static final String DATA_FILTER_FLAGS = "filter_flags";

    private static final String TAG_FILTER_FLAGS = "filter";

    @InjectPresenter
    AppsListPresenter presenter;

    private LceLayout lce;
    private RecyclerView list;
    private CompositeAdapter<AppViewModel> adapter;

    private final AppsListFilter filter = new AppsListFilter(this);

    @ProvidePresenter
    AppsListPresenter providePresenter() {
        return daggerService().presenters().apps();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_apps_list, container, false);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
        switch (item.getItemId()) {
            case R.id.action_filter:
                showFilterDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.saveChanges();
    }

    @Override
    public void showLoading() {
        lce.showLoading();
    }

    @Override
    public void onAppsLoaded(List<AppViewModel> apps) {
        adapter.setDataset(apps);
        adapter.notifyDataSetChanged();
        filter.setDataset(apps);
        lce.showContent();
    }

    @Override
    public void onItemChanged(int position) {
        adapter.notifyItemChanged(position);
    }

    @Override
    public void showError(Throwable e) {
        lce.error()
                .button(v -> presenter.loadApps())
                .message(e.getMessage())
                .show();
    }

    @Override
    public void onVisibilityClick(int position, AppViewModel item) {
        presenter.toggleAppVisibility(position, item);
    }

    @Override
    public void onFilterResult(String query, List<AppViewModel> items) {
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

    private void initAdapter() {
        Context context = requireContext();
        Picasso picasso = daggerService().main().getPicassoFactory().create(context);
        adapter = new CompositeAdapter.Builder<AppViewModel>(context)
                .add(new AppsViewModelAdapter(picasso, this))
                .recyclerView(list)
                .create();
    }

    private void addListDivider() {
        Drawable drawable = requireContext().getDrawable(R.drawable.settings_apps_divider);
        assert drawable != null;
        DividerItemDecoration decoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        decoration.setDrawable(drawable);
        list.addItemDecoration(decoration);
    }

    private void showFilterDialog() {
        new FilterFlagsDialogFragment.Builder()
                .setFlags(filter.getFlags())
                .setListenerProvider(new FilterFlagsDialogListenerProvider())
                .build()
                .show(getChildFragmentManager(), TAG_FILTER_FLAGS);
    }

    private static class FilterFlagsDialogListenerProvider implements ListenerFragment<FilterFlagsDialogFragment.Listener> {
        @Override
        public FilterFlagsDialogFragment.Listener get(Fragment parentFragment) {
            AppsListFragment fragment = (AppsListFragment) parentFragment;
            return new FilterFlagsDialogFragment.Listener() {
                @Override
                public void onFlagsSet(EnumSet<FilterFlag> newFlags) {
                    fragment.filter.setFlags(newFlags);
                }

                @Override
                public void onFlagsReset() {
                    fragment.filter.resetFlags();
                }
            };
        }
    }
}