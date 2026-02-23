package com.newbulaco.showdown.api.content;

import org.jetbrains.annotations.Nullable;

// side conditions affect an entire team (e.g. Stealth Rock, Light Screen, Tailwind)
public class CustomSideCondition {

    public enum Category {
        HAZARD,     // e.g. Stealth Rock, Spikes
        SCREEN,     // e.g. Light Screen, Reflect
        OTHER       // e.g. Tailwind, Lucky Chant
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final Category category;
    private final int color;
    private final boolean stackable;
    private final int maxStacks;
    private final String modId;
    private final String showdownJs;

    private CustomSideCondition(Builder builder) {
        this.id = builder.id.toLowerCase().replace(" ", "").replace("_", "");
        this.displayName = builder.displayName != null ? builder.displayName : formatId(builder.id);
        this.description = builder.description != null ? builder.description : "";
        this.category = builder.category != null ? builder.category : Category.OTHER;
        this.color = builder.color != 0 ? builder.color : getCategoryDefaultColor(this.category);
        this.stackable = builder.stackable;
        this.maxStacks = builder.maxStacks > 0 ? builder.maxStacks : 1;
        this.modId = builder.modId != null ? builder.modId : "unknown";
        this.showdownJs = builder.showdownJs;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public int getColor() { return color; }
    public boolean isStackable() { return stackable; }
    public int getMaxStacks() { return maxStacks; }
    public String getModId() { return modId; }

    @Nullable
    public String getShowdownJs() { return showdownJs; }

    public boolean hasShowdownJs() { return showdownJs != null && !showdownJs.isEmpty(); }

    private static int getCategoryDefaultColor(Category category) {
        return switch (category) {
            case HAZARD -> 0xFFAA5555;  // reddish
            case SCREEN -> 0xFFFFFF55;  // yellow
            case OTHER -> 0xFF55FFFF;   // cyan
        };
    }

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
        private Category category;
        private int color;
        private boolean stackable;
        private int maxStacks;
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

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder hazard() {
            this.category = Category.HAZARD;
            return this;
        }

        public Builder screen() {
            this.category = Category.SCREEN;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder stackable(int maxStacks) {
            this.stackable = true;
            this.maxStacks = maxStacks;
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

        public CustomSideCondition build() {
            return new CustomSideCondition(this);
        }
    }
}
