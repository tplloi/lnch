package com.italankin.lnch.feature.home.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.italankin.lnch.R;
import com.italankin.lnch.feature.home.model.GroupViewModel;
import com.italankin.lnch.util.adapterdelegate.BaseAdapterDelegate;

public class GroupViewModelAdapter extends BaseAdapterDelegate<GroupViewModelHolder, GroupViewModel> {
    private final Listener listener;

    public GroupViewModelAdapter(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.item_group;
    }

    @NonNull
    @Override
    protected GroupViewModelHolder createViewHolder(View itemView) {
        GroupViewModelHolder holder = new GroupViewModelHolder(itemView);
        if (listener != null) {
            itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onGroupClick(pos, getItem(pos));
                }
            });
            itemView.setOnLongClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onGroupLongClick(pos, getItem(pos));
                }
                return true;
            });
        }
        return holder;
    }

    @Override
    public void onBind(GroupViewModelHolder holder, int position, GroupViewModel item) {
        holder.bind(item);
    }

    @Override
    public long getItemId(int position, GroupViewModel item) {
        return item.hashCode();
    }

    @Override
    public boolean isType(int position, Object item) {
        return item instanceof GroupViewModel;
    }

    public interface Listener {
        void onGroupClick(int position, GroupViewModel item);

        void onGroupLongClick(int position, GroupViewModel item);
    }
}


class GroupViewModelHolder extends RecyclerView.ViewHolder {
    final TextView label;

    GroupViewModelHolder(View itemView) {
        super(itemView);
        label = itemView.findViewById(R.id.label);
    }

    void bind(GroupViewModel item) {
        label.setText(item.label);
        label.setTextColor(item.color);
    }
}