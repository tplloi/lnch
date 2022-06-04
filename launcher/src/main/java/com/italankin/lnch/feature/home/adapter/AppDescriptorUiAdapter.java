package com.italankin.lnch.feature.home.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.italankin.lnch.R;
import com.italankin.lnch.feature.home.model.UserPrefs;
import com.italankin.lnch.feature.home.util.NotificationDotDrawable;
import com.italankin.lnch.model.ui.impl.AppDescriptorUi;
import com.italankin.lnch.util.ResUtils;

import java.util.List;

public class AppDescriptorUiAdapter extends HomeAdapterDelegate<AppDescriptorUiAdapter.ViewHolder, AppDescriptorUi> {

    private final Listener listener;

    public AppDescriptorUiAdapter(Listener listener) {
        this(listener, Params.DEFAULT);
    }

    public AppDescriptorUiAdapter(Listener listener, Params params) {
        super(params);
        this.listener = listener;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.item_app;
    }

    @NonNull
    @Override
    protected ViewHolder createViewHolder(View itemView) {
        ViewHolder holder = new ViewHolder(itemView);
        itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onAppClick(pos, getItem(pos));
            }
        });
        itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onAppLongClick(pos, getItem(pos));
                return true;
            }
            return false;
        });
        return holder;
    }

    @Override
    protected void update(ViewHolder holder, TextView label, UserPrefs.ItemPrefs itemPrefs) {
        super.update(holder, label, itemPrefs);
        holder.notificationDot.setColor(itemPrefs.notificationDotColor);
    }

    @Override
    protected boolean isType(int position, Object item, boolean ignoreVisibility) {
        return item instanceof AppDescriptorUi &&
                (ignoreVisibility || !((AppDescriptorUi) item).isIgnored());
    }

    @Override
    public boolean onFailedToRecycle(ViewHolder holder) {
        holder.notificationDot.cancelAnimation();
        return true;
    }

    public interface Listener {
        void onAppClick(int position, AppDescriptorUi item);

        void onAppLongClick(int position, AppDescriptorUi item);
    }

    static class ViewHolder extends HomeAdapterDelegate.ViewHolder<AppDescriptorUi> {
        final TextView label;
        final NotificationDotDrawable notificationDot;

        ViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.label);

            notificationDot = new NotificationDotDrawable(
                    itemView.getResources().getDimensionPixelSize(R.dimen.notification_dot_size),
                    ContextCompat.getColor(itemView.getContext(), R.color.notification_dot),
                    ResUtils.resolveColor(label.getContext(), R.attr.colorItemShadowDefault));
            label.setForeground(notificationDot);
        }

        @Override
        protected void bind(AppDescriptorUi item) {
            bindItem(item);
            notificationDot.setBadgeVisible(item.isBadgeVisible(), false);
        }

        @Override
        protected void bind(AppDescriptorUi item, List<Object> payloads) {
            bindItem(item);
            notificationDot.setBadgeVisible(item.isBadgeVisible(), payloads.contains(AppDescriptorUi.PAYLOAD_BADGE));
        }

        @Nullable
        @Override
        protected TextView getLabel() {
            return label;
        }

        private void bindItem(AppDescriptorUi item) {
            label.setText(item.getVisibleLabel());
            label.setTextColor(item.getVisibleColor());
            Integer badgeColor = item.getCustomBadgeColor();
            notificationDot.setColor(badgeColor != null ? badgeColor : itemPrefs.notificationDotColor);
            notificationDot.setMargin(itemPrefs.itemPadding * 2);
        }
    }
}
