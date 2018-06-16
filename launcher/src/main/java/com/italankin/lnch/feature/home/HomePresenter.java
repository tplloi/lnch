package com.italankin.lnch.feature.home;

import com.arellomobile.mvp.InjectViewState;
import com.italankin.lnch.feature.base.AppPresenter;
import com.italankin.lnch.feature.home.model.AppViewModel;
import com.italankin.lnch.model.repository.apps.AppsRepository;
import com.italankin.lnch.model.repository.apps.actions.RenameAction;
import com.italankin.lnch.model.repository.apps.actions.SetCustomColorAction;
import com.italankin.lnch.model.repository.apps.actions.SetVisibilityAction;
import com.italankin.lnch.model.repository.apps.actions.SwapAction;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.util.rx.ListMapper;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@InjectViewState
public class HomePresenter extends AppPresenter<HomeView> {

    private final AppsRepository appsRepository;
    private final Preferences preferences;
    /**
     * View commands will dispatch this instance on every state restore, so any changes
     * made to this list will be visible to new views.
     */
    private List<AppViewModel> apps;
    private AppsRepository.Editor editor;

    @Inject
    HomePresenter(AppsRepository appsRepository, Preferences preferences) {
        this.appsRepository = appsRepository;
        this.preferences = preferences;
    }

    @Override
    protected void onFirstViewAttach() {
        observeApps();
        initialLoad();
    }

    void reloadAppsImmediate() {
        update();
    }

    void startEditMode() {
        if (editor != null) {
            throw new IllegalStateException("Editor is not null!");
        }
        editor = appsRepository.edit();
        getViewState().onStartEditMode();
    }

    void swapApps(int from, int to) {
        if (editor == null) {
            throw new IllegalStateException();
        }
        editor.enqueue(new SwapAction(from, to));
        SwapAction.swap(apps, from, to);
        getViewState().onItemsSwap(from, to);
    }

    void renameApp(int position, AppViewModel item, String customLabel) {
        if (editor == null) {
            throw new IllegalStateException();
        }
        String s = customLabel.isEmpty() ? null : customLabel;
        editor.enqueue(new RenameAction(item.item, s));
        item.customLabel = s;
        getViewState().onItemChanged(position);
    }

    void changeAppCustomColor(int position, AppViewModel item, String value) {
        if (editor == null) {
            throw new IllegalStateException();
        }
        Integer customColor;
        if (value != null && !value.isEmpty()) {
            try {
                customColor = Integer.decode("0x" + value) + 0xff000000;
            } catch (Exception e) {
                getViewState().showError(e);
                return;
            }
        } else {
            customColor = null;
        }
        editor.enqueue(new SetCustomColorAction(item.item, customColor));
        item.customColor = customColor;
        getViewState().onItemChanged(position);
    }

    void hideApp(int position, AppViewModel item) {
        if (editor == null) {
            throw new IllegalStateException("Editor is null!");
        }
        editor.enqueue(new SetVisibilityAction(item.item, false));
        item.hidden = true;
        getViewState().onItemChanged(position);
    }

    void discardChanges() {
        if (editor == null) {
            throw new IllegalStateException("Editor is null!");
        }
        editor = null;
        getViewState().onChangesDiscarded();
        update();
    }

    void stopEditMode() {
        if (editor == null) {
            throw new IllegalStateException("Editor is null!");
        }
        editor.commit()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableState() {
                    @Override
                    protected void onComplete(HomeView viewState) {
                        viewState.onStopEditMode();
                        update();
                    }

                    @Override
                    protected void onError(HomeView viewState, Throwable e) {
                        viewState.showError(e);
                    }
                });
        editor = null;
    }

    private void initialLoad() {
        getViewState().showProgress();
        update();
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

    private void observeApps() {
        appsRepository.observeApps()
                .filter(appItems -> editor == null)
                .map(ListMapper.create(AppViewModel::new))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new State<List<AppViewModel>>() {
                    @Override
                    protected void onNext(HomeView viewState, List<AppViewModel> list) {
                        Timber.d("Receive update: %s", list);
                        apps = list;
                        viewState.onAppsLoaded(apps, preferences.homeLayout());
                    }
                });
    }
}