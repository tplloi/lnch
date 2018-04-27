package com.italankin.lnch.model.repository.search.match;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;

import com.italankin.lnch.R;

public class GoogleMatch extends Match {
    public GoogleMatch(String query) {
        super(Type.OTHER);
        color = Color.WHITE;
        label = query.trim();
        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + query));
        iconRes = R.drawable.ic_search;
    }
}