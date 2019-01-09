package com.italankin.lnch.feature.home;

import android.support.annotation.ColorInt;
import android.support.v7.util.DiffUtil;

import com.arellomobile.mvp.InjectViewState;
import com.italankin.lnch.feature.base.AppPresenter;
import com.italankin.lnch.feature.home.model.Update;
import com.italankin.lnch.feature.home.model.UserPrefs;
import com.italankin.lnch.model.descriptor.CustomColorDescriptor;
import com.italankin.lnch.model.descriptor.CustomLabelDescriptor;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.HiddenDescriptor;
import com.italankin.lnch.model.descriptor.impl.AppDescriptor;
import com.italankin.lnch.model.descriptor.impl.DeepShortcutDescriptor;
import com.italankin.lnch.model.descriptor.impl.GroupDescriptor;
import com.italankin.lnch.model.descriptor.impl.IntentDescriptor;
import com.italankin.lnch.model.repository.descriptor.DescriptorRepository;
import com.italankin.lnch.model.repository.descriptor.actions.AddAction;
import com.italankin.lnch.model.repository.descriptor.actions.RecolorAction;
import com.italankin.lnch.model.repository.descriptor.actions.RemoveAction;
import com.italankin.lnch.model.repository.descriptor.actions.RenameAction;
import com.italankin.lnch.model.repository.descriptor.actions.RunnableAction;
import com.italankin.lnch.model.repository.descriptor.actions.SetVisibilityAction;
import com.italankin.lnch.model.repository.descriptor.actions.SwapAction;
import com.italankin.lnch.model.repository.descriptor.actions.UnpinShortcutAction;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.model.repository.prefs.SeparatorState;
import com.italankin.lnch.model.repository.shortcuts.Shortcut;
import com.italankin.lnch.model.repository.shortcuts.ShortcutsRepository;
import com.italankin.lnch.model.viewmodel.CustomColorItem;
import com.italankin.lnch.model.viewmodel.CustomLabelItem;
import com.italankin.lnch.model.viewmodel.DescriptorItem;
import com.italankin.lnch.model.viewmodel.ExpandableItem;
import com.italankin.lnch.model.viewmodel.HiddenItem;
import com.italankin.lnch.model.viewmodel.VisibleItem;
import com.italankin.lnch.model.viewmodel.impl.AppViewModel;
import com.italankin.lnch.model.viewmodel.impl.DeepShortcutViewModel;
import com.italankin.lnch.model.viewmodel.impl.GroupViewModel;
import com.italankin.lnch.model.viewmodel.util.DescriptorItemDiffCallback;
import com.italankin.lnch.model.viewmodel.util.ViewModelFactory;
import com.italankin.lnch.util.ListUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.support.v7.util.DiffUtil.calculateDiff;

@InjectViewState
public class HomePresenter extends AppPresenter<HomeView> {

    private static final List<DescriptorItem> INITIAL = new ArrayList<>();

    private final DescriptorRepository descriptorRepository;
    private final ShortcutsRepository shortcutsRepository;
    private final Preferences preferences;
    private final SeparatorState separatorState;
    /**
     * View commands will dispatch this instance on every state restore, so any changes
     * made to this list will be visible to new views.
     */
    private List<DescriptorItem> items = INITIAL;
    private DescriptorRepository.Editor editor;

    @Inject
    HomePresenter(DescriptorRepository descriptorRepository, ShortcutsRepository shortcutsRepository,
            Preferences preferences, SeparatorState separatorState) {
        this.descriptorRepository = descriptorRepository;
        this.shortcutsRepository = shortcutsRepository;
        this.preferences = preferences;
        this.separatorState = separatorState;
    }

    @Override
    protected void onFirstViewAttach() {
        reloadApps();
    }

    void reloadApps() {
        observe();
        getViewState().showProgress();
        update();
    }

    void toggleExpandableItemState(int position, ExpandableItem item) {
        setItemExpanded(items, position, !item.isExpanded(), true);
    }

    void startCustomize() {
        if (editor != null) {
            throw new IllegalStateException("Editor is not null!");
        }
        editor = descriptorRepository.edit();
        for (int i = 0, size = items.size(); i < size; i++) {
            DescriptorItem item = items.get(i);
            if (item instanceof ExpandableItem) {
                setItemExpanded(items, i, true, true);
            }
        }
        getViewState().onStartCustomize();
    }

    void swapApps(int from, int to) {
        editor.enqueue(new SwapAction(from, to));
        ListUtils.swap(items, from, to);
        getViewState().onItemsSwap(from, to);
    }

    void renameItem(int position, CustomLabelItem item, String customLabel) {
        String s = customLabel.trim().isEmpty() ? null : customLabel;
        editor.enqueue(new RenameAction((CustomLabelDescriptor) item.getDescriptor(), s));
        item.setCustomLabel(s);
        getViewState().onItemChanged(position);
    }

    void changeItemCustomColor(int position, CustomColorItem item, Integer color) {
        editor.enqueue(new RecolorAction((CustomColorDescriptor) item.getDescriptor(), color));
        item.setCustomColor(color);
        getViewState().onItemChanged(position);
    }

    void hideItem(int position, HiddenItem item) {
        editor.enqueue(new SetVisibilityAction((HiddenDescriptor) item.getDescriptor(), false));
        item.setHidden(true);
        getViewState().onItemChanged(position);
    }

    void addGroup(int position, String label, @ColorInt int color) {
        GroupDescriptor item = new GroupDescriptor(label, color);
        editor.enqueue(new AddAction(position, item));
        items.add(position, new GroupViewModel(item));
        getViewState().onItemInserted(position);
    }

    void removeItem(int position, DescriptorItem item) {
        Descriptor descriptor = item.getDescriptor();
        if (descriptor instanceof DeepShortcutDescriptor) {
            editor.enqueue(new UnpinShortcutAction(shortcutsRepository, (DeepShortcutDescriptor) descriptor));
        } else {
            editor.enqueue(new RemoveAction(position));
            editor.enqueue(new RunnableAction(() -> separatorState.remove(descriptor.getId())));
        }
        items.remove(position);
        getViewState().onItemsRemoved(position, 1);
    }

    void confirmDiscardChanges() {
        if (editor.isEmpty()) {
            discardChanges();
        } else {
            getViewState().onConfirmDiscardChanges();
        }
    }

    void discardChanges() {
        editor = null;
        getViewState().onChangesDiscarded();
        update();
    }

    void stopCustomize() {
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

    void updateShortcuts(AppDescriptor descriptor) {
        shortcutsRepository.loadShortcuts(descriptor)
                .subscribeOn(Schedulers.io())
                .subscribe(new CompletableState() {
                    @Override
                    public void onComplete() {
                        Timber.d("Shortcuts updated for id=%s", descriptor.getId());
                    }
                });
    }

    void pinShortcut(Shortcut shortcut) {
        shortcutsRepository.pinShortcut(shortcut)
                .andThen(descriptorRepository.update())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableState() {
                    @Override
                    protected void onComplete(HomeView viewState) {
                        viewState.onShortcutPinned(shortcut);
                    }

                    @Override
                    protected void onError(HomeView viewState, Throwable e) {
                        viewState.showError(e);
                    }
                });
    }

    void pinIntent(IntentDescriptor descriptor) {
        descriptorRepository.edit()
                .enqueue(new AddAction(descriptor))
                .commit()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableState() {
                    @Override
                    protected void onError(HomeView viewState, Throwable e) {
                        viewState.showError(e);
                    }
                });
    }

    void startShortcut(DeepShortcutViewModel item) {
        Shortcut shortcut = shortcutsRepository.getShortcut(item.packageName, item.id);
        if (shortcut != null) {
            if (shortcut.isEnabled()) {
                getViewState().startShortcut(shortcut);
            } else {
                getViewState().onShortcutDisabled(shortcut.getDisabledMessage());
            }
        } else {
            getViewState().onShortcutNotFound();
        }
    }

    void removeItemImmediate(int position, DescriptorItem item) {
        Descriptor descriptor = item.getDescriptor();
        DescriptorRepository.Editor editor = descriptorRepository.edit();
        if (descriptor instanceof DeepShortcutDescriptor) {
            editor.enqueue(new UnpinShortcutAction(shortcutsRepository, (DeepShortcutDescriptor) descriptor));
        } else {
            editor.enqueue(new RemoveAction(position));
            if (descriptor instanceof GroupDescriptor) {
                editor.enqueue(new RunnableAction(() -> separatorState.remove(descriptor.getId())));
            }
        }
        editor.commit()
                .subscribeOn(Schedulers.io())
                .subscribe(new CompletableState() {
                    @Override
                    public void onComplete() {
                        Timber.d("Item removed: %s", descriptor);
                    }
                });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////

    private void update() {
        descriptorRepository.update()
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

    private void observe() {
        Observable.combineLatest(observeApps(), observeUserPrefs(), Update::with)
                .filter(appItems -> editor == null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new State<Update>() {
                    @Override
                    protected void onNext(HomeView viewState, Update update) {
                        Timber.d("Update: %s", update);
                        items = update.items;
                        viewState.onReceiveUpdate(update);
                        updateShortcuts();
                    }

                    @Override
                    protected void onError(HomeView viewState, Throwable e) {
                        if (items == INITIAL) {
                            viewState.onReceiveUpdateError(e);
                        } else {
                            viewState.showError(e);
                        }
                    }
                });
    }

    private Observable<Update> observeApps() {
        return descriptorRepository.observe()
                .map(ViewModelFactory::createItems)
                .doOnNext(this::restoreGroupsState)
                .scan(Update.EMPTY, this::calculateUpdates)
                .skip(1);
    }

    private Observable<UserPrefs> observeUserPrefs() {
        return preferences.observe()
                .map(s -> new UserPrefs(preferences))
                .startWith(new UserPrefs(preferences))
                .distinctUntilChanged();
    }

    private void restoreGroupsState(List<DescriptorItem> items) {
        for (int i = 0, size = items.size(); i < size; i++) {
            DescriptorItem item = items.get(i);
            if (item instanceof ExpandableItem) {
                ExpandableItem expandableItem = (ExpandableItem) item;
                String id = expandableItem.getDescriptor().getId();
                setItemExpanded(items, i, separatorState.isExpanded(id), false);
            }
        }
    }

    private Update calculateUpdates(Update previous, List<DescriptorItem> newItems) {
        DescriptorItemDiffCallback callback = new DescriptorItemDiffCallback(previous.items, newItems);
        DiffUtil.DiffResult diffResult = calculateDiff(callback, true);
        return new Update(newItems, diffResult);
    }

    private void setItemExpanded(List<DescriptorItem> items, int position, boolean expanded, boolean notify) {
        ExpandableItem item = (ExpandableItem) items.get(position);
        if (expanded == item.isExpanded()) {
            return;
        }
        int startIndex = position + 1;
        int endIndex = findNextExpandableItemIndex(items, startIndex);
        if (endIndex < 0) {
            endIndex = items.size();
        }
        int count = endIndex - startIndex;
        if (count <= 0) {
            return;
        }
        item.setExpanded(expanded);
        separatorState.setExpanded(item.getDescriptor().getId(), expanded);
        for (int i = startIndex; i < endIndex; i++) {
            VisibleItem visibleItem = (VisibleItem) items.get(i);
            visibleItem.setVisible(expanded);
        }
        if (!notify) {
            return;
        }
        if (expanded) {
            getViewState().onItemsInserted(startIndex, count);
        } else {
            getViewState().onItemsRemoved(startIndex, count);
        }
    }

    private static int findNextExpandableItemIndex(List<DescriptorItem> items, int startPosition) {
        for (int i = startPosition; i < items.size(); i++) {
            if (items.get(i) instanceof ExpandableItem) {
                return i;
            }
        }
        return -1;
    }
}
