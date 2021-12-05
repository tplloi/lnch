package com.italankin.lnch.feature.home.apps;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.italankin.lnch.LauncherApp;
import com.italankin.lnch.R;
import com.italankin.lnch.api.LauncherIntents;
import com.italankin.lnch.feature.base.AppFragment;
import com.italankin.lnch.feature.base.BackButtonHandler;
import com.italankin.lnch.feature.home.adapter.AppDescriptorUiAdapter;
import com.italankin.lnch.feature.home.adapter.DeepShortcutDescriptorUiAdapter;
import com.italankin.lnch.feature.home.adapter.GroupDescriptorUiAdapter;
import com.italankin.lnch.feature.home.adapter.HomeAdapter;
import com.italankin.lnch.feature.home.adapter.IntentDescriptorUiAdapter;
import com.italankin.lnch.feature.home.adapter.NotVisibleDescriptorUiAdapter;
import com.italankin.lnch.feature.home.adapter.PinnedShortcutDescriptorUiAdapter;
import com.italankin.lnch.feature.home.apps.delegate.AppClickDelegate;
import com.italankin.lnch.feature.home.apps.delegate.AppClickDelegateImpl;
import com.italankin.lnch.feature.home.apps.delegate.DeepShortcutClickDelegate;
import com.italankin.lnch.feature.home.apps.delegate.DeepShortcutClickDelegateImpl;
import com.italankin.lnch.feature.home.apps.delegate.ErrorDelegate;
import com.italankin.lnch.feature.home.apps.delegate.ErrorDelegateImpl;
import com.italankin.lnch.feature.home.apps.delegate.IntentClickDelegate;
import com.italankin.lnch.feature.home.apps.delegate.IntentClickDelegateImpl;
import com.italankin.lnch.feature.home.apps.delegate.ItemPopupDelegate;
import com.italankin.lnch.feature.home.apps.delegate.ItemPopupDelegateImpl;
import com.italankin.lnch.feature.home.apps.delegate.PinnedShortcutClickDelegate;
import com.italankin.lnch.feature.home.apps.delegate.PinnedShortcutClickDelegateImpl;
import com.italankin.lnch.feature.home.apps.delegate.PopupDelegate;
import com.italankin.lnch.feature.home.apps.delegate.PopupDelegateImpl;
import com.italankin.lnch.feature.home.apps.delegate.SearchIntentStarterDelegate;
import com.italankin.lnch.feature.home.apps.delegate.SearchIntentStarterDelegateImpl;
import com.italankin.lnch.feature.home.apps.delegate.ShortcutStarterDelegate;
import com.italankin.lnch.feature.home.apps.delegate.ShortcutStarterDelegateImpl;
import com.italankin.lnch.feature.home.apps.folder.FolderFragment;
import com.italankin.lnch.feature.home.behavior.SearchBarBehavior;
import com.italankin.lnch.feature.home.model.Update;
import com.italankin.lnch.feature.home.model.UserPrefs;
import com.italankin.lnch.feature.home.util.IntentQueue;
import com.italankin.lnch.feature.home.util.SwapItemHelper;
import com.italankin.lnch.feature.home.widget.EditModePanel;
import com.italankin.lnch.feature.home.widget.HomeRecyclerView;
import com.italankin.lnch.feature.home.widget.SearchBar;
import com.italankin.lnch.feature.intentfactory.IntentFactoryActivity;
import com.italankin.lnch.feature.intentfactory.IntentFactoryResult;
import com.italankin.lnch.feature.settings.SettingsActivity;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.impl.GroupDescriptor;
import com.italankin.lnch.model.descriptor.impl.IntentDescriptor;
import com.italankin.lnch.model.repository.descriptor.NameNormalizer;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.model.repository.search.match.DescriptorMatch;
import com.italankin.lnch.model.repository.search.match.Match;
import com.italankin.lnch.model.repository.shortcuts.Shortcut;
import com.italankin.lnch.model.repository.shortcuts.ShortcutsRepository;
import com.italankin.lnch.model.ui.CustomColorDescriptorUi;
import com.italankin.lnch.model.ui.CustomLabelDescriptorUi;
import com.italankin.lnch.model.ui.DescriptorUi;
import com.italankin.lnch.model.ui.IgnorableDescriptorUi;
import com.italankin.lnch.model.ui.RemovableDescriptorUi;
import com.italankin.lnch.model.ui.VisibleDescriptorUi;
import com.italankin.lnch.model.ui.impl.AppDescriptorUi;
import com.italankin.lnch.model.ui.impl.DeepShortcutDescriptorUi;
import com.italankin.lnch.model.ui.impl.GroupDescriptorUi;
import com.italankin.lnch.model.ui.impl.IntentDescriptorUi;
import com.italankin.lnch.model.ui.impl.PinnedShortcutDescriptorUi;
import com.italankin.lnch.util.DescriptorUtils;
import com.italankin.lnch.util.IntentUtils;
import com.italankin.lnch.util.PackageUtils;
import com.italankin.lnch.util.ResUtils;
import com.italankin.lnch.util.StatusBarUtils;
import com.italankin.lnch.util.ViewUtils;
import com.italankin.lnch.util.picasso.PackageIconHandler;
import com.italankin.lnch.util.widget.ActionPopupWindow;
import com.italankin.lnch.util.widget.EditTextAlertDialog;
import com.italankin.lnch.util.widget.LceLayout;
import com.italankin.lnch.util.widget.colorpicker.ColorPickerDialog;
import com.squareup.picasso.Picasso;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class AppsFragment extends AppFragment implements AppsView,
        BackButtonHandler,
        IntentQueue.OnIntentAction,
        DeepShortcutDescriptorUiAdapter.Listener,
        IntentDescriptorUiAdapter.Listener,
        AppDescriptorUiAdapter.Listener,
        GroupDescriptorUiAdapter.Listener,
        PinnedShortcutDescriptorUiAdapter.Listener {

    private static final int ANIM_LIST_APPEARANCE_DURATION = 400;

    @InjectPresenter
    AppsPresenter presenter;

    private Preferences preferences;
    private Picasso picasso;
    private IntentQueue intentQueue;

    private LceLayout lce;
    private HomeRecyclerView list;
    private HomeAdapter adapter;
    private EditModePanel editModePanel;
    private SearchBar searchBar;
    private CoordinatorLayout coordinator;

    private SearchBarBehavior searchBarBehavior;

    private ItemTouchHelper touchHelper;
    private Preferences.HomeLayout layout;

    private boolean animateOnResume;
    private boolean editMode;

    private AppClickDelegate appClickDelegate;
    private PopupDelegate popupDelegate;
    private ErrorDelegate errorDelegate;
    private ShortcutStarterDelegate shortcutStarterDelegate;
    private ItemPopupDelegate itemPopupDelegate;
    private PinnedShortcutClickDelegate pinnedShortcutClickDelegate;
    private DeepShortcutClickDelegate deepShortcutClickDelegate;
    private IntentClickDelegate intentClickDelegate;
    private SearchIntentStarterDelegate searchIntentStarterDelegate;

    private final ActivityResultLauncher<Void> createIntentLauncher = registerForActivityResult(
            new IntentFactoryActivity.CreateContract(),
            this::onNewIntentCreated);

    private final ActivityResultLauncher<IntentDescriptor> editIntentLauncher = registerForActivityResult(
            new IntentFactoryActivity.EditContract(),
            this::onIntentEdited);

    @ProvidePresenter
    AppsPresenter providePresenter() {
        return LauncherApp.daggerService.presenters().apps();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        preferences = LauncherApp.daggerService.main().preferences();
        picasso = LauncherApp.daggerService.main().picassoFactory().create(requireContext());
        intentQueue = LauncherApp.daggerService.main().intentQueue();
    }

    @Override
    public void onStart() {
        super.onStart();
        animateOnResume = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                preferences.get(Preferences.APPS_LIST_ANIMATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (animateOnResume) {
            animateOnResume = false;
            animateListAppearance();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        searchBarBehavior.hide();
    }

    public void onRestart() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        lce = view.findViewById(R.id.lce_apps);
        searchBar = view.findViewById(R.id.search_bar);
        list = view.findViewById(R.id.list);
        coordinator = view.findViewById(R.id.coordinator);

        setupSearchBar();

        touchHelper = new ItemTouchHelper(new SwapItemHelper(presenter::swapApps));
        touchHelper.attachToRecyclerView(list);

        registerWindowInsets(view);

        initDelegates(requireContext());

        intentQueue.registerOnIntentAction(this);
    }

    private void initDelegates(Context context) {
        ShortcutsRepository shortcutsRepository = LauncherApp.daggerService.main().shortcutsRepository();

        popupDelegate = new PopupDelegateImpl(list);
        errorDelegate = new ErrorDelegateImpl(context);
        itemPopupDelegate = new ItemPopupDelegateImpl(context, picasso, popupDelegate) {
            @Override
            protected void removeItemImmediate(RemovableDescriptorUi item) {
                presenter.removeItemImmediate(item);
            }
        };
        shortcutStarterDelegate = new ShortcutStarterDelegateImpl(context, errorDelegate) {
            @Override
            public void startCustomize() {
                presenter.startCustomize();
            }
        };
        pinnedShortcutClickDelegate = new PinnedShortcutClickDelegateImpl(context, errorDelegate, itemPopupDelegate);
        deepShortcutClickDelegate = new DeepShortcutClickDelegateImpl(shortcutStarterDelegate,
                itemPopupDelegate, shortcutsRepository);
        searchIntentStarterDelegate = new SearchIntentStarterDelegateImpl(context, preferences, errorDelegate) {
            @Override
            protected void startCustomize() {
                presenter.startCustomize();
            }
        };
        intentClickDelegate = new IntentClickDelegateImpl(searchIntentStarterDelegate, itemPopupDelegate);
        appClickDelegate = new AppClickDelegateImpl(context, picasso, errorDelegate, popupDelegate,
                shortcutStarterDelegate, preferences, shortcutsRepository) {
            @Override
            protected void pinShortcut(Shortcut shortcut) {
                presenter.pinShortcut(shortcut);
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        intentQueue.unregisterOnIntentAction(this);
    }

    @Override
    public boolean onIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return false;
        }
        switch (action) {
            case Intent.ACTION_MAIN: {
                popupDelegate.dismissPopup();
                dismissFolder();
                if (searchBarBehavior.isShown()) {
                    searchBar.reset();
                    searchBarBehavior.hide();
                } else if (preferences.get(Preferences.SCROLL_TO_TOP)) {
                    scrollToTop();
                }
                return true;
            }
            case LauncherIntents.ACTION_EDIT_MODE: {
                dismissFolder();
                if (!editMode) {
                    animateOnResume = false;
                    presenter.startCustomize();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReceiveUpdate(Update update) {
        setItems(update);
        if (update.items.isEmpty()) {
            lce.empty()
                    .message(R.string.apps_list_empty)
                    .button(R.string.open_settings, this::startLnchSettings)
                    .show();
        } else {
            lce.showContent();
        }

        applyUserPrefs(update.userPrefs);

        boolean needsFullUpdate = adapter.updateUserPrefs(update.userPrefs);
        if (needsFullUpdate) {
            adapter.notifyDataSetChanged();
        } else {
            update.dispatchTo(list.getAdapter());
        }
    }

    @Override
    public boolean onBackPressed() {
        if (popupDelegate.dismissPopup()) {
            return true;
        }
        if (editMode) {
            presenter.confirmDiscardChanges();
            return true;
        }
        if (searchBarBehavior.isShown()) {
            searchBarBehavior.hide();
        }
        if (dismissFolder()) {
            return true;
        }
        scrollToTop();
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Start
    ///////////////////////////////////////////////////////////////////////////

    private boolean startAppActivity(ComponentName componentName, View view) {
        Rect bounds = ViewUtils.getViewBounds(view);
        Bundle opts = IntentUtils.getActivityLaunchOptions(view, bounds);
        return IntentUtils.safeStartMainActivity(requireContext(), componentName, bounds, opts);
    }

    private boolean startAppSettings(String packageName, @Nullable View view) {
        return IntentUtils.safeStartAppSettings(requireContext(), packageName, view);
    }

    private void startLnchSettings(View view) {
        startAppActivity(SettingsActivity.getComponentName(requireContext()), view);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Adapter callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onAppClick(int position, AppDescriptorUi item) {
        if (editMode) {
            showCustomizePopup(position, item);
        } else {
            View view = list.findViewForAdapterPosition(position);
            appClickDelegate.onAppClick(item, view);
        }
    }

    @Override
    public void onAppLongClick(int position, AppDescriptorUi item) {
        if (editMode) {
            startDrag(position);
        } else {
            View view = list.findViewForAdapterPosition(position);
            appClickDelegate.onAppLongClick(item, view);
        }
    }

    @Override
    public void onGroupClick(int position, GroupDescriptorUi item) {
        if (editMode) {
            showCustomizePopup(position, item);
        } else {
            showFolder(position, item.getDescriptor());
        }
    }

    @Override
    public void onGroupLongClick(int position, GroupDescriptorUi item) {
        if (editMode) {
            startDrag(position);
        } else {
            View anchor = list.findViewForAdapterPosition(position);
            itemPopupDelegate.showItemPopup(item, anchor);
        }
    }

    @Override
    public void onPinnedShortcutClick(int position, PinnedShortcutDescriptorUi item) {
        if (editMode) {
            showCustomizePopup(position, item);
        } else {
            pinnedShortcutClickDelegate.onPinnedShortcutClick(item);
        }
    }

    @Override
    public void onPinnedShortcutLongClick(int position, PinnedShortcutDescriptorUi item) {
        if (editMode) {
            startDrag(position);
        } else {
            View view = list.findViewForAdapterPosition(position);
            pinnedShortcutClickDelegate.onPinnedShortcutLongClick(item, view);
        }
    }

    @Override
    public void onDeepShortcutClick(int position, DeepShortcutDescriptorUi item) {
        if (editMode) {
            showCustomizePopup(position, item);
        } else {
            View view = list.findViewForAdapterPosition(position);
            deepShortcutClickDelegate.onDeepShortcutClick(item, view);
        }
    }

    @Override
    public void onDeepShortcutLongClick(int position, DeepShortcutDescriptorUi item) {
        if (editMode) {
            startDrag(position);
        } else {
            View view = list.findViewForAdapterPosition(position);
            deepShortcutClickDelegate.onDeepShortcutLongClick(item, view);
        }
    }

    @Override
    public void onIntentClick(int position, IntentDescriptorUi item) {
        if (editMode) {
            showCustomizePopup(position, item);
        } else {
            intentClickDelegate.onIntentClick(item);
        }
    }

    @Override
    public void onIntentLongClick(int position, IntentDescriptorUi item) {
        if (editMode) {
            startDrag(position);
        } else {
            View view = list.findViewForAdapterPosition(position);
            intentClickDelegate.onIntentLongClick(item, view);
        }
    }

    private void showFolder(int position, GroupDescriptor descriptor) {
        Point point = null;
        View view = list.findViewForAdapterPosition(position);
        if (view != null) {
            int[] loc = new int[2];
            view.getLocationInWindow(loc);
            point = new Point(loc[0] + view.getWidth() / 2, loc[1]);
        }

        FolderFragment.newInstance(descriptor, point)
                .show(getParentFragmentManager(), android.R.id.content);
    }

    private void setEditMode(boolean value) {
        if (editMode == value) {
            return;
        }
        editMode = value;
        searchBarBehavior.hide();
        searchBarBehavior.setEnabled(!value);
        if (value) {
            dismissFolder();
            popupDelegate.dismissPopup();
            searchBar.hideSoftKeyboard();
            EditModePanel panel = new EditModePanel(requireContext())
                    .setMessage(R.string.customize_hint)
                    .setOnAddActionClickListener(this::showEditModeAddPopup)
                    .setOnSaveActionClickListener(v -> {
                        if (this.editModePanel != null && this.editModePanel.isShown()) {
                            presenter.stopCustomize();
                        }
                    });
            editModePanel = panel.show(coordinator);
        } else if (editModePanel != null) {
            editModePanel.dismiss();
            editModePanel = null;
        }
    }

    private void showCustomizePopup(int position, DescriptorUi item) {
        Context context = requireContext();
        ActionPopupWindow popup = new ActionPopupWindow(context, picasso);
        if (item instanceof IgnorableDescriptorUi) {
            popup.addAction(new ActionPopupWindow.ItemBuilder(context)
                    .setIcon(R.drawable.ic_action_hide)
                    .setOnClickListener(v -> presenter.ignoreItem(position, (IgnorableDescriptorUi) item))
            );
        }
        if (item instanceof CustomLabelDescriptorUi) {
            popup.addShortcut(new ActionPopupWindow.ItemBuilder(context)
                    .setLabel(R.string.customize_item_rename)
                    .setIcon(R.drawable.ic_action_rename)
                    .setIconDrawableTintAttr(R.attr.colorAccent)
                    .setOnClickListener(v -> setItemCustomLabel(position, (CustomLabelDescriptorUi) item))
            );
        }
        if (item instanceof CustomColorDescriptorUi) {
            popup.addShortcut(new ActionPopupWindow.ItemBuilder(context)
                    .setLabel(R.string.customize_item_color)
                    .setEnabled(!preferences.get(Preferences.APPS_COLOR_OVERLAY_SHOW))
                    .setIcon(R.drawable.ic_action_color)
                    .setIconDrawableTintAttr(R.attr.colorAccent)
                    .setOnClickListener(v -> setItemColor(position, (CustomColorDescriptorUi) item))
            );
        }
        if (item instanceof IntentDescriptorUi && preferences.get(Preferences.EXPERIMENTAL_INTENT_FACTORY)) {
            popup.addShortcut(new ActionPopupWindow.ItemBuilder(context)
                    .setLabel(R.string.customize_item_edit_intent)
                    .setIcon(R.drawable.ic_action_intent_edit)
                    .setIconDrawableTintAttr(R.attr.colorAccent)
                    .setOnClickListener(v -> {
                        IntentDescriptor descriptor = ((IntentDescriptorUi) item).getDescriptor();
                        editIntentLauncher.launch(descriptor);
                    })
            );
        }
        if (item instanceof VisibleDescriptorUi) {
            popup.addShortcut(new ActionPopupWindow.ItemBuilder(context)
                    .setLabel(R.string.customize_item_add_to_folder)
                    .setIcon(R.drawable.ic_action_add_to_folder)
                    .setIconDrawableTintAttr(R.attr.colorAccent)
                    .setOnClickListener(v -> {
                        presenter.showGroupSelect((VisibleDescriptorUi) item);
                    })
            );
        }
        if (item instanceof RemovableDescriptorUi) {
            popup.addAction(new ActionPopupWindow.ItemBuilder(context)
                    .setIcon(R.drawable.ic_action_delete)
                    .setOnClickListener(v -> presenter.removeItem(position, item))
            );
        }
        View view = list.findViewForAdapterPosition(position);
        popupDelegate.showPopupWindow(popup, view);
    }

    private void showEditModeAddPopup(View anchor) {
        Context context = requireContext();
        ActionPopupWindow popup = new ActionPopupWindow(context, picasso);
        popup.addShortcut(new ActionPopupWindow.ItemBuilder(context)
                .setLabel(R.string.edit_add_folder)
                .setOnClickListener(v -> {
                    String label = getString(R.string.new_folder_default_label);
                    int color = ResUtils.resolveColor(requireContext(), R.attr.colorGroupTitleDefault);
                    presenter.addGroup(label, color);
                }));
        if (preferences.get(Preferences.EXPERIMENTAL_INTENT_FACTORY)) {
            popup.addShortcut(new ActionPopupWindow.ItemBuilder(context)
                    .setLabel(R.string.edit_add_intent)
                    .setOnClickListener(v -> {
                        createIntentLauncher.launch(null);
                    }));
        }
        popupDelegate.showPopupWindow(popup, anchor);
    }

    private void onNewIntentCreated(@Nullable IntentFactoryResult result) {
        if (result == null) {
            return;
        }
        NameNormalizer nameNormalizer = LauncherApp.daggerService.main().nameNormalizer();
        String label = nameNormalizer.normalize(result.label);
        IntentDescriptor intentDescriptor = new IntentDescriptor(result.intent, label);
        presenter.addIntent(intentDescriptor);
    }

    private void onIntentEdited(@Nullable IntentFactoryResult result) {
        if (result == null || result.descriptorId == null) {
            return;
        }
        NameNormalizer nameNormalizer = LauncherApp.daggerService.main().nameNormalizer();
        String label = nameNormalizer.normalize(result.label);
        presenter.editIntent(result.descriptorId, result.intent, label);
    }

    private void setItemCustomLabel(int position, CustomLabelDescriptorUi item) {
        String visibleLabel = item.getVisibleLabel();
        EditTextAlertDialog.builder(requireContext())
                .setTitle(item.getVisibleLabel())
                .customizeEditText(editText -> {
                    editText.setText(visibleLabel);
                    editText.setSingleLine(true);
                    editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                    if (visibleLabel != null) {
                        editText.setSelection(visibleLabel.length());
                    }
                })
                .setPositiveButton(R.string.ok, (dialog, editText) -> {
                    String newLabel = editText.getText().toString().trim();
                    if (!newLabel.equals(visibleLabel)) {
                        presenter.renameItem(position, item, newLabel);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.customize_action_reset, (dialog, which) -> {
                    presenter.renameItem(position, item, "");
                })
                .setCancellable(false)
                .show();
    }

    private void setItemColor(int position, CustomColorDescriptorUi item) {
        int visibleColor = item.getVisibleColor();
        ColorPickerDialog.builder(requireContext())
                .setHexVisible(false)
                .setSelectedColor(visibleColor)
                .setOnColorPickedListener(color -> {
                    if (color != visibleColor) {
                        presenter.changeItemCustomColor(position, item, color);
                    }
                })
                .setResetButton(getString(R.string.customize_action_reset), (dialog, which) -> {
                    presenter.changeItemCustomColor(position, item, null);
                })
                .setCancellable(false)
                .show();
    }

    @Override
    public void showProgress() {
        lce.showLoading();
    }

    @Override
    public void onStartCustomize() {
        setEditMode(true);
    }

    @Override
    public void onConfirmDiscardChanges() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.customize_discard_message)
                .setPositiveButton(R.string.customize_discard, (dialog, which) -> presenter.discardChanges())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onStopCustomize() {
        setEditMode(false);
        Toast.makeText(requireContext(), R.string.customize_saved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChangesDiscarded() {
        setEditMode(false);
    }

    @Override
    public void onShortcutPinned(Shortcut shortcut) {
        Toast.makeText(requireContext(), getString(R.string.deep_shortcut_pinned, shortcut.getShortLabel()),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShortcutAlreadyPinnedError(Shortcut shortcut) {
        Toast.makeText(requireContext(), getString(R.string.deep_shortcut_already_pinned, shortcut.getShortLabel()),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemsSwap(int from, int to) {
        adapter.notifyItemMoved(from, to);
    }

    @Override
    public void onItemChanged(int position) {
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onItemInserted(int position) {
        adapter.notifyItemInserted(position);
        list.scrollToPosition(position);
    }

    @Override
    public void onItemsInserted(int startIndex, int count) {
        adapter.notifyItemRangeInserted(startIndex, count);
        list.scrollToPosition(startIndex + Math.min(1, count));
    }

    @Override
    public void onItemsRemoved(int startIndex, int count) {
        adapter.notifyItemRangeRemoved(startIndex, count);
    }

    @Override
    public void onReceiveUpdateError(Throwable e) {
        lce.error()
                .button(v -> presenter.reloadApps())
                .message(e.getMessage())
                .show();
    }

    @Override
    public void showSelectFolderDialog(VisibleDescriptorUi item, List<GroupDescriptor> descriptors) {
        if (descriptors.isEmpty()) {
            errorDelegate.showError(R.string.folder_select_no_folders);
            return;
        }
        CharSequence[] items = new CharSequence[descriptors.size()];
        for (int i = 0; i < descriptors.size(); i++) {
            items[i] = descriptors.get(i).getVisibleLabel();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.customize_item_select_folder)
                .setItems(items, (dialog, which) -> {
                    GroupDescriptor selected = descriptors.get(which);
                    presenter.addToGroup(selected.getId(), item);
                    String text = getString(R.string.folder_select_selected, selected.getVisibleLabel());
                    Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Errors
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void showError(Throwable e) {
        errorDelegate.showError(getString(R.string.error));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Popup
    ///////////////////////////////////////////////////////////////////////////

    private boolean dismissFolder() {
        if (!getParentFragmentManager().isStateSaved()) {
            return FolderFragment.dismiss(getParentFragmentManager());
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // SearchBar
    ///////////////////////////////////////////////////////////////////////////

    private void setupSearchBar() {
        searchBarBehavior = new SearchBarBehavior(searchBar, list, new SearchBarBehavior.Listener() {
            @Override
            public void onShow() {
                if (preferences.get(Preferences.SEARCH_SHOW_SOFT_KEYBOARD)) {
                    searchBar.focusEditText();
                }
            }

            @Override
            public void onHide() {
                searchBar.reset();
            }

            @Override
            public void onShowExpand() {
                Boolean expandNotifications = preferences.get(Preferences.EXPAND_NOTIFICATIONS);
                if (expandNotifications) {
                    StatusBarUtils.expandStatusBar(requireContext());
                }
            }
        });
        searchBarBehavior.setEnabled(!editMode);
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) searchBar.getLayoutParams();
        lp.setBehavior(searchBarBehavior);
        searchBar.setLayoutParams(lp);

        SearchBar.Listener listener = new SearchBar.Listener() {
            @Override
            public void handleIntent(Intent intent) {
                searchIntentStarterDelegate.handleSearchIntent(intent);
            }

            @Override
            public void onSearchFired() {
                searchBarBehavior.hide();
            }

            @Override
            public void onSearchItemPinClick(Match match) {
                searchBarBehavior.hide();
                NameNormalizer nameNormalizer = LauncherApp.daggerService.main().nameNormalizer();
                String label = nameNormalizer.normalize(match.getLabel(requireContext()));
                IntentDescriptor intentDescriptor = new IntentDescriptor(match.getIntent(),
                        label, match.getColor(requireContext()));
                presenter.pinIntent(intentDescriptor);
            }

            @Override
            public void onSearchItemInfoClick(Match match) {
                if (!(match instanceof DescriptorMatch)) {
                    return;
                }
                Descriptor descriptor = ((DescriptorMatch) match).getDescriptor();
                String packageName = DescriptorUtils.getPackageName(descriptor);
                if (packageName != null && startAppSettings(packageName, null)) {
                    searchBarBehavior.hide();
                }
            }
        };
        searchBar.setListener(listener);
        searchBar.setupSettings(v -> {
            searchBarBehavior.hide();
            startAppActivity(SettingsActivity.getComponentName(requireContext()), v);
        }, v -> {
            searchBarBehavior.hide(presenter::startCustomize);
            return true;
        });

        if (preferences.get(Preferences.SEARCH_SHOW_GLOBAL_SEARCH)) {
            setupGlobalSearchButton();
        }
    }

    private void setupGlobalSearchButton() {
        ComponentName searchActivity = PackageUtils.getGlobalSearchActivity(requireContext());
        if (searchActivity == null) {
            searchBar.hideGlobalSearch();
        } else {
            Uri icon = PackageIconHandler.uriFrom(searchActivity.getPackageName());
            searchBar.setupGlobalSearch(icon, v -> {
                Intent intent = new Intent().setComponent(searchActivity);
                if (IntentUtils.safeStartActivity(requireContext(), intent)) {
                    searchBarBehavior.hide();
                } else {
                    errorDelegate.showError(R.string.error);
                }
            }, v -> {
                return IntentUtils.safeStartAppSettings(requireContext(), searchActivity.getPackageName(), v);
            });
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other
    ///////////////////////////////////////////////////////////////////////////

    private void startDrag(int position) {
        if (preferences.get(Preferences.APPS_SORT_MODE) != Preferences.AppsSortMode.MANUAL) {
            errorDelegate.showError(R.string.error_manual_sorting_required);
            return;
        }
        RecyclerView.LayoutManager layoutManager = list.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        View view = layoutManager.findViewByPosition(position);
        if (view == null) {
            return;
        }
        touchHelper.startDrag(list.getChildViewHolder(view));
    }

    private void animateListAppearance() {
        float endY = searchBarBehavior.isShown() ? list.getTranslationY() : 0;
        float startY = -endY - getResources().getDimension(R.dimen.list_appearance_translation_offset);
        list.setTranslationY(startY);
        list.setAlpha(0);
        ValueAnimator translationAnimator = ValueAnimator.ofFloat(startY, endY);
        translationAnimator.addUpdateListener(animation -> {
            list.setTranslationY((float) animation.getAnimatedValue());
        });
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0, 1);
        alphaAnimator.addUpdateListener(animation -> {
            list.setAlpha(animation.getAnimatedFraction());
        });
        AnimatorSet animations = new AnimatorSet();
        animations.playTogether(translationAnimator, alphaAnimator);
        animations.setInterpolator(new DecelerateInterpolator(3));
        animations.setDuration(ANIM_LIST_APPEARANCE_DURATION);
        if (searchBarBehavior.isEnabled()) {
            animations.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    searchBarBehavior.setEnabled(true);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    searchBarBehavior.setEnabled(true);
                }
            });
            searchBarBehavior.setEnabled(false);
        }
        animations.start();
    }

    private void applyUserPrefs(UserPrefs userPrefs) {
        if (userPrefs.globalSearch && !searchBar.isGlobalSearchVisible()) {
            setupGlobalSearchButton();
        } else if (!userPrefs.globalSearch && searchBar.isGlobalSearchVisible()) {
            searchBar.hideGlobalSearch();
        }
        searchBar.setSearchBarSizeDimen(userPrefs.largeSearchBar
                ? R.dimen.search_bar_size_large
                : R.dimen.search_bar_size);
        setLayout(userPrefs.homeLayout, userPrefs.homeAlignment);
        list.setVerticalScrollBarEnabled(userPrefs.showScrollbar);
    }

    private void setItems(Update update) {
        if (adapter == null) {
            adapter = new HomeAdapter.Builder(getContext())
                    .add(new AppDescriptorUiAdapter(this))
                    .add(new NotVisibleDescriptorUiAdapter())
                    .add(new GroupDescriptorUiAdapter(this))
                    .add(new PinnedShortcutDescriptorUiAdapter(this))
                    .add(new IntentDescriptorUiAdapter(this))
                    .add(new DeepShortcutDescriptorUiAdapter(this))
                    .recyclerView(list)
                    .setHasStableIds(true)
                    .create();
            adapter.updateUserPrefs(update.userPrefs);
        }
        adapter.setDataset(update.items);
        list.setVisibility(View.VISIBLE);
    }

    private void setLayout(Preferences.HomeLayout layout, Preferences.HomeAlignment homeAlignment) {
        if (layout != this.layout) {
            this.layout = layout;
            list.setLayoutManager(new FlexboxLayoutManager(requireContext(), FlexDirection.ROW));
        }
        if (list.getLayoutManager() instanceof FlexboxLayoutManager) {
            FlexboxLayoutManager lm = (FlexboxLayoutManager) list.getLayoutManager();
            switch (homeAlignment) {
                case START:
                    lm.setJustifyContent(JustifyContent.FLEX_START);
                    break;
                case CENTER:
                    lm.setJustifyContent(JustifyContent.CENTER);
                    break;
                case END:
                    lm.setJustifyContent(JustifyContent.FLEX_END);
                    break;
            }
        }
    }

    private void scrollToTop() {
        if (preferences.get(Preferences.SMOOTH_SCROLL_TO_TOP)) {
            list.smoothScrollToPosition(0);
        } else {
            list.post(() -> list.scrollToPosition(0));
        }
    }

    private void registerWindowInsets(View view) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            list.setBottomInset(insets.getStableInsetBottom());
            return insets;
        });

        WindowInsets insets = view.getRootWindowInsets();
        if (insets != null) {
            list.setBottomInset(insets.getStableInsetBottom());
        }
    }
}
