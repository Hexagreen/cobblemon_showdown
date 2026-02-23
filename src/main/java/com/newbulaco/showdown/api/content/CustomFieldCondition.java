package com.newbulaco.showdown.api.content;

import org.jetbrains.annotations.Nullable;

public class CustomFieldCondition {

    public enum Type {
        WEATHER,
        TERRAIN,
        ROOM
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final Type type;
    private final int color;
    private final int defaultDuration;
    private final String modId;
    private final String showdownJs;

    private CustomFieldCondition(Builder builder) {
        this.id = builder.id.toLowerCase().replace(" ", "").replace("_", "");
        this.displayName = builder.displayName != null ? builder.displayName : formatId(builder.id);
        this.description = builder.description != null ? builder.description : "";
        this.type = builder.type != null ? builder.type : Type.WEATHER;
        this.color = builder.color != 0 ? builder.color : 0xFFFFFFFF;
        this.defaultDuration = builder.defaultDuration > 0 ? builder.defaultDuration : 5;
        this.modId = builder.modId != null ? builder.modId : "unknown";
        this.showdownJs = builder.showdownJs;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Type getType() { return type; }
    public int getColor() { return color; }
    public int getDefaultDuration() { return defaultDuration; }
    public String getModId() { return modId; }

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
        private Type type;
        private int color;
        private int defaultDuration;
        private String modId;
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

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder weather() {
            this.type = Type.WEATHER;
            return this;
        }

        public Builder terrain() {
            this.type = Type.TERRAIN;
            return this;
        }

        public Builder room() {
            this.type = Type.ROOM;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder defaultDuration(int turns) {
            this.defaultDuration = turns;
            return this;
        }

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder showdownJs(String showdownJs) {
            this.showdownJs = showdownJs;
            return this;
        }

        public CustomFieldCondition build() {
            return new CustomFieldCondition(this);
        }
    }
}
