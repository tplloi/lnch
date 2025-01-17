package com.italankin.lnch.model.repository.descriptor.actions;

import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.repository.descriptor.DescriptorRepository;

import java.util.List;

import androidx.annotation.Nullable;
import timber.log.Timber;

public abstract class BaseAction implements DescriptorRepository.Editor.Action {

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T findById(List<Descriptor> items, String id) {
        for (Descriptor item : items) {
            if (item.getId().equals(id)) {
                return (T) item;
            }
        }
        Timber.w("findById: no descriptor found for id=%s", id);
        return null;
    }
}
