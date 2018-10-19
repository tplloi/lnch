package com.italankin.lnch.feature.home;

import android.support.annotation.ColorInt;

import com.arellomobile.mvp.InjectViewState;
import com.italankin.lnch.feature.base.AppPresenter;
import com.italankin.lnch.feature.home.model.UserPrefs;
import com.italankin.lnch.model.descriptor.CustomColorDescriptor;
import com.italankin.lnch.model.descriptor.CustomLabelDescriptor;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.HiddenDescriptor;
import com.italankin.lnch.model.descriptor.impl.AppDescriptor;
import com.italankin.lnch.model.descriptor.impl.GroupDescriptor;
import com.italankin.lnch.model.descriptor.impl.ShortcutDescriptor;
import com.italankin.lnch.model.repository.apps.AppsRepository;
import com.italankin.lnch.model.repository.apps.actions.AddAction;
import com.italankin.lnch.model.repository.apps.actions.RecolorAction;
import com.italankin.lnch.model.repository.apps.actions.RemoveAction;
import com.italankin.lnch.model.repository.apps.actions.RenameAction;
import com.italankin.lnch.model.repository.apps.actions.SetVisibilityAction;
import com.italankin.lnch.model.repository.apps.actions.SwapAction;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.model.repository.shortcuts.Shortcut;
import com.italankin.lnch.model.repository.shortcuts.ShortcutsRepository;
import com.italankin.lnch.model.viewmodel.CustomColorItem;
import com.italankin.lnch.model.viewmodel.CustomLabelItem;
import com.italankin.lnch.model.viewmodel.DescriptorItem;
import com.italankin.lnch.model.viewmodel.ExpandableItem;
import com.italankin.lnch.model.viewmodel.HiddenItem;
import com.italankin.lnch.model.viewmodel.VisibleItem;
import com.italankin.lnch.model.viewmodel.impl.AppViewModel;
import com.italankin.lnch.model.viewmodel.impl.GroupViewModel;
import com.italankin.lnch.model.viewmodel.impl.ShortcutViewModel;
import com.italankin.lnch.util.ListUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@InjectViewState
public class HomePresenter extends AppPresenter<HomeView> {

    private final AppsRepository appsRepository;
    private final ShortcutsRepository shortcutsRepository;
    private final Preferences preferences;
    /**
     * View commands will dispatch this instance on every state restore, so any changes
     * made to this list will be visible to new views.
     */
    private List<DescriptorItem> items;
    private AppsRepository.Editor editor;
    private final UserPrefs userPrefs = new UserPrefs();

    @Inject
    HomePresenter(AppsRepository appsRepository, ShortcutsRepository shortcutsRepository, Preferences preferences) {
        this.appsRepository = appsRepository;
        this.shortcutsRepository = shortcutsRepository;
        this.preferences = preferences;
    }

    @Override
    protected void onFirstViewAttach() {
        reloadApps();
    }

    void loadApps() {
        getViewState().showProgress();
        update();
    }

    void reloadApps() {
        observeApps();
        loadApps();
    }

    void reloadAppsImmediate() {
        update();
    }

    void toggleExpandableItemState(ExpandableItem item) {
        setItemExpanded(item, !item.isExpanded());
    }

    void startCustomize() {
        if (editor != null) {
            throw new IllegalStateException("Editor is not null!");
        }
        editor = appsRepository.edit();
        expandAll();
        getViewState().onStartCustomize();
    }

    void swapApps(int from, int to) {
        requireEditor();
        editor.enqueue(new SwapAction(from, to));
        ListUtils.swap(items, from, to);
        getViewState().onItemsSwap(from, to);
    }

    void renameItem(int position, CustomLabelItem item, String customLabel) {
        requireEditor();
        String s = customLabel.trim().isEmpty() ? null : customLabel;
        editor.enqueue(new RenameAction((CustomLabelDescriptor) item.getDescriptor(), s));
        item.setCustomLabel(s);
        getViewState().onItemChanged(position);
    }

    void changeItemCustomColor(int position, CustomColorItem item, Integer color) {
        requireEditor();
        editor.enqueue(new RecolorAction((CustomColorDescriptor) item.getDescriptor(), color));
        item.setCustomColor(color);
        getViewState().onItemChanged(position);
    }

    void hideItem(int position, HiddenItem item) {
        requireEditor();
        editor.enqueue(new SetVisibilityAction((HiddenDescriptor) item.getDescriptor(), false));
        item.setHidden(true);
        getViewState().onItemChanged(position);
    }

    void addGroup(int position, String label, @ColorInt int color) {
        requireEditor();
        GroupDescriptor item = new GroupDescriptor(label, color);
        editor.enqueue(new AddAction(position, item));
        items.add(position, new GroupViewModel(item));
        getViewState().onItemInserted(position);
    }

    void removeItem(int position) {
        requireEditor();
        editor.enqueue(new RemoveAction(position));
        items.remove(position);
        getViewState().onItemsRemoved(position, 1);
    }

    void confirmDiscardChanges() {
        requireEditor();
        if (editor.isEmpty()) {
            discardChanges();
        } else {
            getViewState().onConfirmDiscardChanges();
        }
    }

    void discardChanges() {
        requireEditor();
        editor = null;
        getViewState().onChangesDiscarded();
        update();
    }

    void stopCustomize() {
        requireEditor();
        editor.commit()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableState() {
                    @Override
                    protected void onComplete(HomeView viewState) {
                        viewState.onStopCustomize();
                    }

                    @Override
                    protected void onError(HomeView viewState, Throwable e) {
                        viewState.showError(e);
                    }
                });
        editor = null;
    }

    void showAppPopup(int position, AppViewModel item) {
        List<Shortcut> shortcuts = shortcutsRepository.getShortcuts(item.getDescriptor());
        getViewState().showAppPopup(position, item, shortcuts);
    }

    private void requireEditor() {
        if (editor == null) {
            throw new IllegalStateException();
        }
    }

    private void update() {
        appsRepository.update()
                .subscribeOn(Schedulers.io())
                .subscribe(new CompletableState() {
                    @Override
                    public void onComplete() {
                        Timber.d("Apps updated");
                    }
                });
    }

    private void updateShortcuts() {
        shortcutsRepository.loadShortcuts()
                .subscribeOn(Schedulers.io())
                .subscribe(new CompletableState() {
                    @Override
                    public void onComplete() {
                        Timber.d("Shortcuts updated");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "updateShortcuts");
                    }
                });
    }

    private void observeApps() {
        appsRepository.observe()
                .filter(appItems -> editor == null)
                .map(this::mapItems)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new State<List<DescriptorItem>>() {
                    @Override
                    protected void onNext(HomeView viewState, List<DescriptorItem> list) {
                        Timber.d("Receive update: %s", list);
                        items = list;
                        updateUserPrefs();
                        viewState.onAppsLoaded(items, userPrefs);
                        updateShortcuts();
                    }

                    @Override
                    protected void onError(HomeView viewState, Throwable e) {
                        if (items != null) {
                            viewState.showError(e);
                        } else {
                            viewState.onAppsLoadError(e);
                        }
                    }
                });
    }

    private void updateUserPrefs() {
        userPrefs.homeLayout = preferences.homeLayout();
        userPrefs.overlayColor = preferences.overlayColor();
        userPrefs.showScrollbar = preferences.showScrollbar();
        userPrefs.itemTextSize = preferences.itemTextSize();
        userPrefs.itemPadding = preferences.itemPadding();
        userPrefs.itemShadowRadius = preferences.itemShadowRadius();
        userPrefs.itemFont = preferences.itemFont().typeface();
    }

    private List<DescriptorItem> mapItems(List<Descriptor> descriptors) {
        List<DescriptorItem> result = new ArrayList<>(descriptors.size());
        for (Descriptor descriptor : descriptors) {
            if (descriptor instanceof AppDescriptor) {
                result.add(new AppViewModel((AppDescriptor) descriptor));
            } else if (descriptor instanceof GroupDescriptor) {
                result.add(new GroupViewModel((GroupDescriptor) descriptor));
            } else if (descriptor instanceof ShortcutDescriptor) {
                result.add(new ShortcutViewModel((ShortcutDescriptor) descriptor));
            }
        }
        return result;
    }

    private void setItemExpanded(ExpandableItem item, boolean expanded) {
        int position = items.indexOf(item);
        if (position < 0) {
            return;
        }
        int startIndex = position + 1;
        int endIndex = findExpandableItemIndex(startIndex);
        if (endIndex < 0) {
            endIndex = items.size();
        }
        int count = endIndex - startIndex;
        if (count <= 0) {
            return;
        }
        item.setExpanded(expanded);
        for (int i = startIndex; i < endIndex; i++) {
            VisibleItem visibleItem = (VisibleItem) items.get(i);
            visibleItem.setVisible(expanded);
        }
        if (expanded) {
            getViewState().onItemsInserted(startIndex, count);
        } else {
            getViewState().onItemsRemoved(startIndex, count);
        }
    }

    private int findExpandableItemIndex(int startPosition) {
        for (int i = startPosition; i < items.size(); i++) {
            if (items.get(i) instanceof ExpandableItem) {
                return i;
            }
        }
        return -1;
    }

    private void expandAll() {
        for (int i = 0, size = items.size(); i < size; i++) {
            DescriptorItem item = items.get(i);
            if (item instanceof ExpandableItem) {
                setItemExpanded((ExpandableItem) item, true);
            }
        }
    }
}
