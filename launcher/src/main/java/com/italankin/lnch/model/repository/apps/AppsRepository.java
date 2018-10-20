package com.italankin.lnch.model.repository.apps;

import com.italankin.lnch.model.descriptor.Descriptor;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface AppsRepository {

    Observable<List<Descriptor>> observe();

    Completable update();

    Editor edit();

    Completable clear();

    List<Descriptor> items();

    Descriptor findDescriptor(String id);

    interface Editor {

        Editor enqueue(Action action);

        boolean isEmpty();

        Editor clear();

        Completable commit();

        interface Action {
            void apply(List<Descriptor> items);
        }
    }
}
