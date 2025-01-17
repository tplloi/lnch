package com.italankin.lnch.model.repository.store.json.model;

import com.google.gson.annotations.SerializedName;
import com.italankin.lnch.model.descriptor.Descriptor;
import com.italankin.lnch.model.descriptor.impl.AppDescriptor;

import java.util.List;

import androidx.annotation.Keep;

public final class AppDescriptorJson implements DescriptorJson {

    public static final String TYPE = "app";

    @Keep
    @SerializedName(PROPERTY_TYPE)
    public String type = TYPE;

    @SerializedName("package_name")
    public String packageName;

    @SerializedName("version_code")
    public long versionCode;

    @SerializedName("component_name")
    public String componentName;

    @SerializedName("original_label")
    public String originalLabel;

    @SerializedName("label")
    public String label;

    @SerializedName("custom_label")
    public String customLabel;

    @SerializedName("color")
    public int color;

    @SerializedName("custom_color")
    public Integer customColor;

    @SerializedName("custom_badge_color")
    public Integer customBadgeColor;

    @SerializedName(value = "ignored", alternate = "hidden")
    public Boolean ignored;

    @SerializedName("search_flags")
    public Integer searchFlags;

    @SerializedName("show_shortcuts")
    public Boolean showShortcuts;

    @SerializedName("aliases")
    public List<String> aliases;

    @Keep
    public AppDescriptorJson() {
    }

    public AppDescriptorJson(AppDescriptor descriptor) {
        this.packageName = descriptor.packageName;
        this.versionCode = descriptor.versionCode;
        this.componentName = descriptor.componentName;
        this.originalLabel = descriptor.originalLabel;
        this.label = descriptor.label;
        this.customLabel = descriptor.customLabel;
        this.color = descriptor.color;
        this.customColor = descriptor.customColor;
        this.customBadgeColor = descriptor.customBadgeColor;
        this.ignored = descriptor.ignored ? true : null;
        this.searchFlags = descriptor.searchFlags != AppDescriptor.SEARCH_DEFAULT_FLAGS ? descriptor.searchFlags : null;
        this.showShortcuts = descriptor.showShortcuts ? null : false;
        this.aliases = descriptor.aliases.isEmpty() ? null : descriptor.aliases;
    }

    @Override
    public Descriptor toDescriptor() {
        AppDescriptor descriptor = new AppDescriptor();
        descriptor.packageName = this.packageName;
        descriptor.versionCode = this.versionCode;
        descriptor.componentName = this.componentName;
        descriptor.label = this.label;
        descriptor.originalLabel = this.originalLabel;
        descriptor.customLabel = this.customLabel;
        descriptor.color = this.color;
        descriptor.customColor = this.customColor;
        descriptor.customBadgeColor = this.customBadgeColor;
        descriptor.ignored = this.ignored != null && this.ignored;
        descriptor.searchFlags = this.searchFlags != null ? this.searchFlags : AppDescriptor.SEARCH_DEFAULT_FLAGS;
        descriptor.showShortcuts = this.showShortcuts == null || this.showShortcuts;
        descriptor.setAliases(aliases);
        return descriptor;
    }
}
