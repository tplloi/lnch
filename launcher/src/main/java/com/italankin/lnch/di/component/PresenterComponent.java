package com.italankin.lnch.di.component;

import android.content.Context;

import com.italankin.lnch.di.scope.AppScope;
import com.italankin.lnch.feature.home.HomePresenter;
import com.italankin.lnch.feature.home.apps.AppsPresenter;
import com.italankin.lnch.feature.home.apps.folder.EditFolderPresenter;
import com.italankin.lnch.feature.home.apps.folder.FolderPresenter;
import com.italankin.lnch.feature.intentfactory.componentselector.ComponentSelectorPresenter;
import com.italankin.lnch.feature.settings.apps.AppsSettingsPresenter;
import com.italankin.lnch.feature.settings.apps.details.AppDetailsPresenter;
import com.italankin.lnch.feature.settings.apps.details.aliases.AppAliasesPresenter;
import com.italankin.lnch.feature.settings.backup.BackupPresenter;
import com.italankin.lnch.feature.settings.fonts.FontsPresenter;
import com.italankin.lnch.feature.settings.hidden_items.HiddenItemsPresenter;
import com.italankin.lnch.feature.settings.lookfeel.LookAndFeelPresenter;
import com.italankin.lnch.feature.settings.preferencesearch.PreferenceSearchPresenter;
import com.italankin.lnch.feature.widgets.WidgetsPresenter;
import com.italankin.lnch.model.backup.BackupReader;
import com.italankin.lnch.model.backup.BackupWriter;
import com.italankin.lnch.model.repository.prefs.WidgetsState;

import dagger.Component;

@AppScope
@Component(dependencies = MainComponent.class)
public interface PresenterComponent {

    HomePresenter home();

    AppsPresenter apps();

    FolderPresenter folder();

    EditFolderPresenter editFolder();

    AppsSettingsPresenter appsSettings();

    HiddenItemsPresenter hiddenItems();

    FontsPresenter fonts();

    AppDetailsPresenter appDetails();

    AppAliasesPresenter appAliases();

    LookAndFeelPresenter lookAndFeel();

    BackupPresenter backup();

    PreferenceSearchPresenter preferenceSearch();

    WidgetsPresenter widgets();

    ComponentSelectorPresenter componentSelector();

    interface Dependencies {

        Context getContext();

        WidgetsState getWidgetsState();

        BackupReader getBackupReader();

        BackupWriter getBackupWriter();
    }
}
