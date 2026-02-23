package com.newbulaco.showdown.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ShowdownConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue CHALLENGE_RADIUS;
    public static final ForgeConfigSpec.BooleanValue CHALLENGE_RADIUS_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Challenge Settings").push("challenge");

        CHALLENGE_RADIUS_ENABLED = builder
                .comment("Whether to require players to be within a certain distance to challenge each other")
                .define("radiusEnabled", true);

        CHALLENGE_RADIUS = builder
                .comment("Maximum distance (in blocks) between players for a challenge to be valid")
                .defineInRange("radius", 15.0, 1.0, 1000.0);

        builder.pop();

        SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static double getChallengeRadius() {
        return CHALLENGE_RADIUS.get();
    }

    public static boolean isChallengeRadiusEnabled() {
        return CHALLENGE_RADIUS_ENABLED.get();
    }
}
