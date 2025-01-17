/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.italankin.lnch.feature.widgets.host;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RemoteViews;

import com.italankin.lnch.feature.widgets.util.CheckLongPressHelper;

import timber.log.Timber;

public class LauncherAppWidgetHostView extends AppWidgetHostView implements View.OnLongClickListener {

    private final CheckLongPressHelper mLongPressHelper = new CheckLongPressHelper(this, this);

    private boolean mIsScrollable;
    private float mSlop;

    private int maxWidth;
    private int maxHeight;

    private float mStartX;
    private float mStartY;

    private final Bundle widgetOptions = new Bundle();

    LauncherAppWidgetHostView(Context context) {
        super(context);
    }

    public void setDimensionsConstraints(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        AppWidgetProviderInfo info = getAppWidgetInfo();
        setMinimumWidth(Math.max(minWidth, info.minWidth));
        setMinimumHeight(Math.max(minHeight, info.minHeight));
        if ((info.resizeMode & AppWidgetProviderInfo.RESIZE_HORIZONTAL) == 0) {
            this.maxWidth = Math.min(getMinimumWidth(), maxWidth);
        } else {
            this.maxWidth = maxWidth;
        }
        if ((info.resizeMode & AppWidgetProviderInfo.RESIZE_VERTICAL) == 0) {
            this.maxHeight = Math.min(getMinimumHeight(), maxHeight);
        } else {
            this.maxHeight = maxHeight;
        }
        widgetOptions.clear();
        requestLayout();
        invalidate();
    }

    @Override
    public boolean onLongClick(View view) {
        if (mIsScrollable) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        view.performLongClick();
        return true;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Just in case the previous long press hasn't been cleared, we make sure to start fresh
        // on touch down.
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mLongPressHelper.cancelLongPress();
        }

        // Consume any touch events for ourselves after longpress is triggered
        if (mLongPressHelper.hasPerformedLongPress()) {
            mLongPressHelper.cancelLongPress();
            return true;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = ev.getX();
                mStartY = ev.getY();
                if (mIsScrollable) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                float x = ev.getX();
                float y = ev.getY();
                if (!pointInView(this, x, y, mSlop) || Math.abs(x - mStartX) >= mSlop || Math.abs(y - mStartY) >= mSlop) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
        }

        // Otherwise continue letting touch events fall through to children
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // If the widget does not handle touch, then cancel
        // long press when we release the touch
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                float x = ev.getX();
                float y = ev.getY();
                if (!pointInView(this, x, y, mSlop) || Math.abs(x - mStartX) >= mSlop || Math.abs(y - mStartY) >= mSlop) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                constrainWidth(maxWidth),
                constrainHeight(heightMeasureSpec, maxHeight)
        );
        if (widgetOptions.isEmpty()) {
            widgetOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, getMeasuredWidth());
            widgetOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, getMeasuredHeight());
            updateAppWidgetOptions(widgetOptions);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mLongPressHelper.cancelLongPress();
    }

    @Override
    protected View getErrorView() {
        // TODO
        return super.getErrorView();
    }

    public void switchToErrorView() {
        // Update the widget with 0 Layout id, to reset the view to error view.
        updateAppWidget(new RemoteViews(getAppWidgetInfo().provider.getPackageName(), 0));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        try {
            super.onLayout(changed, left, top, right, bottom);
        } catch (RuntimeException e) {
            Timber.e(e);
            post(this::switchToErrorView);
        }
        mIsScrollable = checkScrollableRecursively(this);
    }

    private int constrainWidth(int max) {
        if ((getAppWidgetInfo().resizeMode & AppWidgetProviderInfo.RESIZE_HORIZONTAL) == 0) {
            return MeasureSpec.makeMeasureSpec(getMinimumWidth(), MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY);
        }
    }

    private int constrainHeight(int spec, int max) {
        if ((getAppWidgetInfo().resizeMode & AppWidgetProviderInfo.RESIZE_VERTICAL) == 0) {
            return MeasureSpec.makeMeasureSpec(getMinimumHeight(), MeasureSpec.EXACTLY);
        }
        int mode = MeasureSpec.getMode(spec);
        int size = MeasureSpec.getSize(spec);
        switch (mode) {
            case MeasureSpec.AT_MOST:
                return MeasureSpec.makeMeasureSpec(Math.min(size, max), MeasureSpec.AT_MOST);
            case MeasureSpec.EXACTLY:
                return MeasureSpec.makeMeasureSpec(Math.min(size, max), MeasureSpec.EXACTLY);
            default:
            case MeasureSpec.UNSPECIFIED:
                return MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST);
        }
    }

    private boolean checkScrollableRecursively(ViewGroup viewGroup) {
        if (viewGroup instanceof AdapterView) {
            return viewGroup.canScrollVertically(1) || viewGroup.canScrollVertically(-1);
        } else {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof ViewGroup) {
                    if (checkScrollableRecursively((ViewGroup) child)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean pointInView(View v, float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < (v.getWidth() + slop) && localY < (v.getHeight() + slop);
    }
}
