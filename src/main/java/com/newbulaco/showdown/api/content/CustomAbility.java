package com.newbulaco.showdown.api.content;

import org.jetbrains.annotations.Nullable;

public class CustomAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final String modId;
    private final int num;
    private final String showdownJs;

    private CustomAbility(Builder builder) {
        this.id = builder.id.toLowerCase().replace(" ", "").replace("_", "");
        this.displayName = builder.displayName != null ? builder.displayName : formatId(builder.id);
        this.description = builder.description != null ? builder.description : "";
        this.modId = builder.modId != null ? builder.modId : "unknown";
        this.num = builder.num;
        this.showdownJs = builder.showdownJs;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getModId() { return modId; }
    public int getNum() { return num; }

    // e.g. "onDamagingHit(damage, target, source, move) { ... }"
    @Nullable
    public String getShowdownJs() { return showdownJs; }

    public boolean hasShowdownJs() { return showdownJs != null && !showdownJs.isEmpty(); }

    private static String formatId(String id) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : id.toCharArray()) {
            if (c == '_' || c == ' ') {
                result.append(' ');
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString().trim();
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private String description;
        private String modId;
        private int num;
        private String showdownJs;

        public Builder(String id) {
            this.id = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        // use IDs >= 9000 to avoid conflicts with vanilla Showdown
        public Builder num(int num) {
            this.num = num;
            return this;
        }

        // e.g. "onDamagingHit(damage, target, source, move) { ... }"
        public Builder showdownJs(String showdownJs) {
            this.showdownJs = showdownJs;
            return this;
        }

        public CustomAbility build() {
            return new CustomAbility(this);
        }
    }
}
