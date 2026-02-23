package com.newbulaco.showdown.api.content;

import org.jetbrains.annotations.Nullable;

// volatile effects are temporary conditions on a single pokemon that typically end on switch-out
public class CustomVolatileEffect {
    private final String id;
    private final String displayName;
    private final String description;
    private final int color;
    private final boolean showInUI;
    private final String modId;
    private final String showdownJs;

    private CustomVolatileEffect(Builder builder) {
        this.id = builder.id.toLowerCase().replace(" ", "").replace("_", "");
        this.displayName = builder.displayName != null ? builder.displayName : formatId(builder.id);
        this.description = builder.description != null ? builder.description : "";
        this.color = builder.color != 0 ? builder.color : 0xFFFF88FF; // default magenta
        this.showInUI = builder.showInUI;
        this.modId = builder.modId != null ? builder.modId : "unknown";
        this.showdownJs = builder.showdownJs;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getColor() { return color; }
    public boolean shouldShowInUI() { return showInUI; }
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
        private int color;
        private boolean showInUI = true;
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

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder showInUI(boolean show) {
            this.showInUI = show;
            return this;
        }

        public Builder hidden() {
            this.showInUI = false;
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

        public CustomVolatileEffect build() {
            return new CustomVolatileEffect(this);
        }
    }
}
