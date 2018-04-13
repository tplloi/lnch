package com.italankin.lnch.ui.feature.home;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy;
import com.arellomobile.mvp.viewstate.strategy.SingleStateStrategy;
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType;
import com.italankin.lnch.model.AppItem;

import java.util.List;

interface IHomeView extends MvpView {

    @StateStrategyType(value = SingleStateStrategy.class)
    void showProgress();

    @StateStrategyType(value = SingleStateStrategy.class)
    void hideProgress();

    @StateStrategyType(value = SingleStateStrategy.class)
    void onAppsLoaded(List<AppItem> appList);

    @StateStrategyType(value = OneExecutionStateStrategy.class)
    void showError(Throwable e);

}
