package com.italankin.lnch.ui.feature.home;

import com.arellomobile.mvp.InjectViewState;
import com.italankin.lnch.model.AppItem;
import com.italankin.lnch.model.repository.apps.IAppsRepository;
import com.italankin.lnch.model.repository.search.ISearchRepository;
import com.italankin.lnch.ui.base.AppPresenter;
import com.italankin.lnch.util.AppPrefs;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

@InjectViewState
public class HomePresenter extends AppPresenter<IHomeView> {

    private static final Object NOTIFICATION = new Object();

    private final IAppsRepository appsRepository;
    private final ISearchRepository searchRepository;
    private final AppPrefs appPrefs;

    private final Subject<Object, Object> reloadApps = PublishSubject.create();
    private Subscription reloadAppsSub;

    @Inject
    HomePresenter(IAppsRepository appsRepository, ISearchRepository searchRepository, AppPrefs appPrefs) {
        this.appsRepository = appsRepository;
        this.searchRepository = searchRepository;
        this.appPrefs = appPrefs;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        loadApps();
    }

    void loadApps() {
        getViewState().showProgress();
        Subscription s = appsRepository.updates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new State<List<AppItem>>() {
                    @Override
                    protected void onNext(IHomeView viewState, List<AppItem> list) {
                        if (reloadAppsSub == null || reloadAppsSub.isUnsubscribed()) {
                            reloadAppsSub = reloadApps
                                    .debounce(1, TimeUnit.SECONDS)
                                    .subscribe(any -> appsRepository.reload());
                            subs.add(reloadAppsSub);
                        }
                        viewState.onAppsLoaded(list, searchRepository, appPrefs.homeLayout());
                    }

                    @Override
                    protected void onError(IHomeView viewState, Throwable e) {
                        viewState.showError(e);
                    }
                });
        subs.add(s);
    }

    void reloadApps() {
        reloadApps.onNext(NOTIFICATION);
    }

    void reloadAppsNow() {
        appsRepository.reload();
    }

    void swapItems(int from, int to) {
        appsRepository.swapAppsOrder(from, to);
    }

    void saveState() {
        appsRepository.writeChanges();
    }
}
