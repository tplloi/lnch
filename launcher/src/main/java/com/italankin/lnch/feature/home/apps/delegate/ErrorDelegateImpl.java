package com.italankin.lnch.feature.home.apps.delegate;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class ErrorDelegateImpl implements ErrorDelegate {

    private final Context context;

    public ErrorDelegateImpl(Context context) {
        this.context = context;
    }

    @Override
    public void showError(@StringRes int message) {
        showError(context.getText(message));
    }

    @Override
    public void showError(CharSequence message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
