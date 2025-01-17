package com.italankin.lnch.feature.home.repository;

import com.italankin.lnch.model.ui.DescriptorUi;
import com.italankin.lnch.model.ui.impl.FolderDescriptorUi;
import com.italankin.lnch.util.ListUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.Nullable;

public class HomeDescriptorsStateImpl implements HomeDescriptorsState {

    private static final List<DescriptorUi> INITIAL = new ArrayList<>(0);

    private List<DescriptorUi> items = INITIAL;

    private final List<Callback> callbacks = new CopyOnWriteArrayList<>();

    @Override
    public boolean isInitialState() {
        return items == INITIAL;
    }

    @Override
    public void setItems(List<DescriptorUi> items) {
        if (items == null) {
            throw new NullPointerException("items cannot be null");
        }
        this.items = items;
        for (int i = callbacks.size() - 1; i >= 0; i--) {
            Callback callback = callbacks.get(i);
            callback.onNewItems(items);
        }
    }

    @Override
    public List<DescriptorUi> items() {
        return items;
    }

    @Nullable
    @Override
    public HomeEntry<? extends DescriptorUi> find(String id) {
        for (int i = 0, s = items.size(); i < s; i++) {
            DescriptorUi item = items.get(i);
            if (item.getDescriptor().getId().equals(id)) {
                return new HomeEntry<>(i, item);
            }
        }
        return null;
    }

    @Override
    public void removeById(String id) {
        for (int i = 0, s = items.size(); i < s; i++) {
            DescriptorUi item = items.get(i);
            if (item.getDescriptor().getId().equals(id)) {
                items.remove(i);
                for (int j = callbacks.size() - 1; j >= 0; j--) {
                    Callback callback = callbacks.get(j);
                    callback.onItemRemoved(i, item);
                }
                break;
            }
        }
    }

    @Nullable
    @Override
    public <T extends DescriptorUi> HomeEntry<T> find(Class<T> type, String id) {
        for (int i = 0, s = items.size(); i < s; i++) {
            DescriptorUi item = items.get(i);
            if (item.getDescriptor().getId().equals(id) && type.isAssignableFrom(item.getClass())) {
                return new HomeEntry<>(i, type.cast(item));
            }
        }
        return null;
    }

    @Override
    public <T extends DescriptorUi> List<T> allByType(Class<T> type) {
        List<T> result = new ArrayList<>(items.size() / 2);
        for (DescriptorUi item : items) {
            if (type.isAssignableFrom(item.getClass())) {
                result.add(type.cast(item));
            }
        }
        return result;
    }

    @Override
    public List<DescriptorUi> folderItems(FolderDescriptorUi folder) {
        if (folder.items.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, DescriptorUi> map = new HashMap<>(items.size());
        for (DescriptorUi item : items) {
            map.put(item.getDescriptor().getId(), item);
        }
        List<DescriptorUi> result = new ArrayList<>();
        for (String id : folder.items) {
            DescriptorUi item = map.get(id);
            if (item != null) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void insertItem(DescriptorUi item) {
        int position = items.size();
        items.add(item);
        for (int i = callbacks.size() - 1; i >= 0; i--) {
            Callback callback = callbacks.get(i);
            callback.onItemInserted(position, item);
        }
    }

    @Override
    public void updateItem(DescriptorUi item) {
        for (int i = callbacks.size() - 1; i >= 0; i--) {
            Callback callback = callbacks.get(i);
            callback.onItemChanged(items.indexOf(item), item);
        }
    }

    @Override
    public void moveItem(int fromPosition, int toPosition) {
        ListUtils.move(items, fromPosition, toPosition);
        for (int i = callbacks.size() - 1; i >= 0; i--) {
            Callback callback = callbacks.get(i);
            callback.onItemMoved(fromPosition, toPosition);
        }
    }

    @Override
    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
    }
}
