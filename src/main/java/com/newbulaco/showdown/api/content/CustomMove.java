package com.newbulaco.showdown.api.content;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomMove {
    private final String id;
    private final String displayName;
    private final String description;
    private final String type;
    private final String category; // Physical, Special, Status
    private final int power;
    private final int accuracy;
    private final int pp;
    private final int priority;
    private final String modId;
    private final int num;
    private final String showdownJs;
    private final String target;
    private final List<String> flags;

    private CustomMove(Builder builder) {
        this.id = builder.id.toLowerCase().replace(" ", "").replace("_", "");
        this.displayName = builder.displayName != null ? builder.displayName : formatId(builder.id);
        this.description = builder.description != null ? builder.description : "";
        this.type = builder.type != null ? builder.type : "Normal";
        this.category = builder.category != null ? builder.category : "Physical";
        this.power = builder.power;
        this.accuracy = builder.accuracy;
        this.pp = builder.pp > 0 ? builder.pp : 5;
        this.priority = builder.priority;
        this.modId = builder.modId != null ? builder.modId : "unknown";
        this.num = builder.num;
        this.showdownJs = builder.showdownJs;
        this.target = builder.target != null ? builder.target : "normal";
        this.flags = builder.flags != null ? new ArrayList<>(builder.flags) : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public int getPower() { return power; }
    public int getAccuracy() { return accuracy; }
    public int getPp() { return pp; }
    public int getPriority() { return priority; }
    public String getModId() { return modId; }
    public int getNum() { return num; }
    public String getTarget() { return target; }
    public List<String> getFlags() { return Collections.unmodifiableList(flags); }

    @Nullable
    public String getShowdownJs() { return showdownJs; }

    public boolean hasShowdownJs() { return showdownJs != null && !showdownJs.isEmpty(); }

    public boolean isStatusMove() {
        return "Status".equalsIgnoreCase(category);
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
        private String type;
        private String category;
        private int power = 0;
        private int accuracy = 100;
        private int pp = 5;
        private int priority = 0;
        private String modId;
        private int num;
        private String showdownJs;
        private String target;
        private List<String> flags;

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

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder power(int power) {
            this.power = power;
            return this;
        }

        public Builder accuracy(int accuracy) {
            this.accuracy = accuracy;
            return this;
        }

        public Builder pp(int pp) {
            this.pp = pp;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
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

        // e.g. onBasePower, onHit, etc.
        public Builder showdownJs(String showdownJs) {
            this.showdownJs = showdownJs;
            return this;
        }

        // e.g. "normal", "any", "adjacentFoe", "allAdjacent"
        public Builder target(String target) {
            this.target = target;
            return this;
        }

        // e.g. "contact", "protect", "mirror", "slicing"
        public Builder flags(List<String> flags) {
            this.flags = flags;
            return this;
        }

        public Builder flag(String flag) {
            if (this.flags == null) this.flags = new ArrayList<>();
            this.flags.add(flag);
            return this;
        }

        public CustomMove build() {
            return new CustomMove(this);
        }
    }
}
