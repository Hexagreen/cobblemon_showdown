package com.newbulaco.showdown.api.content;

import org.jetbrains.annotations.Nullable;

public class MoveModification {
    private final String moveId;
    private final String modId;
    private final Integer power;
    private final Integer accuracy;
    private final Integer pp;
    private final Integer priority;
    private final String description;
    private final String showdownJs;

    private MoveModification(Builder builder) {
        this.moveId = builder.moveId.toLowerCase().replace(" ", "").replace("_", "");
        this.modId = builder.modId != null ? builder.modId : "unknown";
        this.power = builder.power;
        this.accuracy = builder.accuracy;
        this.pp = builder.pp;
        this.priority = builder.priority;
        this.description = builder.description;
        this.showdownJs = builder.showdownJs;
    }

    public String getMoveId() { return moveId; }
    public String getModId() { return modId; }

    @Nullable public Integer getPower() { return power; }
    @Nullable public Integer getAccuracy() { return accuracy; }
    @Nullable public Integer getPp() { return pp; }
    @Nullable public Integer getPriority() { return priority; }
    @Nullable public String getDescription() { return description; }

    @Nullable
    public String getShowdownJs() { return showdownJs; }

    public boolean hasShowdownJs() { return showdownJs != null && !showdownJs.isEmpty(); }

    public static class Builder {
        private final String moveId;
        private String modId;
        private Integer power;
        private Integer accuracy;
        private Integer pp;
        private Integer priority;
        private String description;
        private String showdownJs;

        public Builder(String moveId) {
            this.moveId = moveId;
        }

        public Builder modId(String modId) {
            this.modId = modId;
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

        // overrides the default description in /dt and UI
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        // use inherit: true in the JS to keep base move properties
        public Builder showdownJs(String showdownJs) {
            this.showdownJs = showdownJs;
            return this;
        }

        public MoveModification build() {
            return new MoveModification(this);
        }
    }
}
