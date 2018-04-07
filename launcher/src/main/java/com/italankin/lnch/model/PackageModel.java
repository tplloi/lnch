package com.italankin.lnch.model;

import android.support.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Comparator;

public class PackageModel {
    public static final Comparator<PackageModel> CMP_NAME_ASC = new NameComparator(true);
    public static final Comparator<PackageModel> CMP_NAME_DESC = new NameComparator(false);
    public static final Comparator<PackageModel> CMP_ORDER = new OrderComparator();

    @Expose(serialize = false, deserialize = false)
    public String packageName;

    @SerializedName("versionCode")
    public int versionCode;

    @SerializedName("label")
    public String label;

    @SerializedName("customLabel")
    public String customLabel;

    @SerializedName("color")
    public int color;

    @SerializedName("customColor")
    public Integer customColor = -1;

    @SerializedName("order")
    public int order;

    @Keep
    public PackageModel() {
    }

    public PackageModel(String packageName) {
        this.packageName = packageName;
    }

    public String getLabel() {
        if (customLabel != null) {
            return customLabel;
        }
        return label;
    }

    public int getColor() {
        if (customColor != null) {
            return customColor;
        }
        return color;
    }
}

class NameComparator implements Comparator<PackageModel> {
    private final boolean asc;

    NameComparator(boolean asc) {
        this.asc = asc;
    }

    @Override
    public int compare(PackageModel lhs, PackageModel rhs) {
        int compare = String.CASE_INSENSITIVE_ORDER.compare(lhs.label, rhs.label);
        return asc ? compare : -compare;
    }
}

class OrderComparator implements Comparator<PackageModel> {
    @Override
    public int compare(PackageModel o1, PackageModel o2) {
        return o1.order > o2.order ? 1 : (o1.order == o2.order ? 0 : -1);
    }
}