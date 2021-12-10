package com.italankin.lnch.feature.home.apps.folder;

import com.arellomobile.mvp.InjectViewState;
import com.italankin.lnch.feature.base.AppPresenter;
import com.italankin.lnch.feature.home.apps.folder.empty.EmptyFolderDescriptorUi;
import com.italankin.lnch.feature.home.model.UserPrefs;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.impl.FolderDescriptor;
import com.italankin.lnch.model.repository.descriptor.DescriptorRepository;
import com.italankin.lnch.model.repository.descriptor.actions.RemoveFromFolderAction;
import com.italankin.lnch.model.repository.prefs.Preferences;
import com.italankin.lnch.model.ui.CustomLabelDescriptorUi;
import com.italankin.lnch.model.ui.DescriptorUi;
import com.italankin.lnch.model.ui.InFolderDescriptorUi;
import com.italankin.lnch.model.ui.util.DescriptorUiFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@InjectViewState
public class FolderPresenter extends AppPresenter<FolderView> {

    private final DescriptorRepository descriptorRepository;
    private final Preferences preferences;

    private List<DescriptorUi> items;

    @Inject
    FolderPresenter(DescriptorRepository descriptorRepository, Preferences preferences) {
        this.descriptorRepository = descriptorRepository;
        this.preferences = preferences;
    }

    void loadFolder(String folderId) {
        Single
                .fromCallable(() -> {
                    return descriptorRepository.findById(FolderDescriptor.class, folderId);
                })
                .zipWith(descriptorsById(), (folder, descriptorsById) -> {
                    List<DescriptorUi> items = new ArrayList<>(4);
                    for (String id : folder.items) {
                        Descriptor descriptor = descriptorsById.get(id);
                        if (descriptor != null) {
                            DescriptorUi item = DescriptorUiFactory.createItem(descriptor);
                            ((InFolderDescriptorUi) item).setFolderId(folder.id);
                            items.add(item);
                        }
                    }
                    if (items.isEmpty()) {
                        items.add(new EmptyFolderDescriptorUi());
                    } else {
                        Collections.sort(items, new AscLabelComparator());
                    }
                    return new FolderState(folder, items, new UserPrefs(preferences));
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleState<FolderState>() {
                    @Override
                    protected void onSuccess(FolderView viewState, FolderState folderState) {
                        viewState.onShowFolder(folderState.descriptor, items = folderState.items, folderState.userPrefs);
                    }

                    @Override
                    protected void onError(FolderView viewState, Throwable e) {
                        viewState.onError(e);
                    }
                });
    }

    void removeFromFolder(String descriptorId, String folderId) {
        descriptorRepository.edit()
                .enqueue(new RemoveFromFolderAction(folderId, descriptorId))
                .commit()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableState() {
                    @Override
                    protected void onComplete(FolderView viewState) {
                        for (Iterator<DescriptorUi> i = items.iterator(); i.hasNext(); ) {
                            if (i.next().getDescriptor().getId().equals(descriptorId)) {
                                i.remove();
                                break;
                            }
                        }
                        if (items.isEmpty()) {
                            items.add(new EmptyFolderDescriptorUi());
                        }
                        viewState.onFolderUpdated(items);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Timber.e(e, "removeFromFolder: %s", e.getMessage());
                    }
                });
    }

    private Single<Map<String, Descriptor>> descriptorsById() {
        return Single.fromCallable(() -> {
            List<Descriptor> items = descriptorRepository.items();
            HashMap<String, Descriptor> result = new HashMap<>(items.size());
            for (Descriptor item : items) {
                result.put(item.getId(), item);
            }
            return result;
        });
    }

    private static class FolderState {
        final FolderDescriptor descriptor;
        final List<DescriptorUi> items;
        final UserPrefs userPrefs;

        FolderState(FolderDescriptor descriptor, List<DescriptorUi> items, UserPrefs userPrefs) {
            this.descriptor = descriptor;
            this.items = items;
            this.userPrefs = userPrefs;
        }
    }

    private static class AscLabelComparator implements Comparator<DescriptorUi> {
        @Override
        public int compare(DescriptorUi lhs, DescriptorUi rhs) {
            if (lhs instanceof CustomLabelDescriptorUi && rhs instanceof CustomLabelDescriptorUi) {
                String lhsLabel = ((CustomLabelDescriptorUi) lhs).getVisibleLabel();
                String rhsLabel = ((CustomLabelDescriptorUi) rhs).getVisibleLabel();
                return String.CASE_INSENSITIVE_ORDER.compare(lhsLabel, rhsLabel);
            } else {
                // should not happen
                return lhs.getDescriptor().getId().compareTo(rhs.getDescriptor().getId());
            }
        }
    }
}
