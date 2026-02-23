package com.newbulaco.showdown.api.content;

import org.jetbrains.annotations.Nullable;

public class AbilityModification {
    private final String abilityId;
    private final String modId;
    private final String description;
    private final String shortDesc;
    private final String showdownJs;

    private AbilityModification(Builder builder) {
        this.abilityId = builder.abilityId.toLowerCase().replace(" ", "").replace("_", "");
        this.modId = builder.modId != null ? builder.modId : "unknown";
        this.description = builder.description;
        this.shortDesc = builder.shortDesc;
        this.showdownJs = builder.showdownJs;
    }

    public String getAbilityId() { return abilityId; }
    public String getModId() { return modId; }

    @Nullable public String getDescription() { return description; }
    @Nullable public String getShortDesc() { return shortDesc; }

    @Nullable
    public String getShowdownJs() { return showdownJs; }

    public boolean hasShowdownJs() { return showdownJs != null && !showdownJs.isEmpty(); }

    public static class Builder {
        private final String abilityId;
        private String modId;
        private String description;
        private String shortDesc;
        private String showdownJs;

        public Builder(String abilityId) {
            this.abilityId = abilityId;
        }

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        // overrides the default description in /dt and UI
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        // used in compact displays
        public Builder shortDesc(String shortDesc) {
            this.shortDesc = shortDesc;
            return this;
        }

        // use inherit: true in the JS to keep base ability properties
        public Builder showdownJs(String showdownJs) {
            this.showdownJs = showdownJs;
            return this;
        }

        public AbilityModification build() {
            return new AbilityModification(this);
        }
    }
}
