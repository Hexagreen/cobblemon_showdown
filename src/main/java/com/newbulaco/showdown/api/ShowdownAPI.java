package com.newbulaco.showdown.api;

import com.newbulaco.showdown.api.content.*;
import com.newbulaco.showdown.api.registry.ContentRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

// register content during mod initialization (FMLCommonSetupEvent)
public final class ShowdownAPI {

    private ShowdownAPI() {}

    public static void registerAbility(CustomAbility ability) {
        ContentRegistry.registerAbility(ability);
    }

    @Nullable
    public static CustomAbility getAbility(String id) {
        return ContentRegistry.getAbility(id);
    }

    public static Collection<CustomAbility> getAllAbilities() {
        return ContentRegistry.getAllAbilities();
    }

    public static void registerMove(CustomMove move) {
        ContentRegistry.registerMove(move);
    }

    @Nullable
    public static CustomMove getMove(String id) {
        return ContentRegistry.getMove(id);
    }

    public static Collection<CustomMove> getAllMoves() {
        return ContentRegistry.getAllMoves();
    }

    public static void registerMoveModification(MoveModification mod) {
        ContentRegistry.registerMoveModification(mod);
    }

    @Nullable
    public static MoveModification getMoveModification(String moveId) {
        return ContentRegistry.getMoveModification(moveId);
    }

    public static Collection<MoveModification> getAllMoveModifications() {
        return ContentRegistry.getAllMoveModifications();
    }

    public static void registerAbilityModification(AbilityModification mod) {
        ContentRegistry.registerAbilityModification(mod);
    }

    @Nullable
    public static AbilityModification getAbilityModification(String abilityId) {
        return ContentRegistry.getAbilityModification(abilityId);
    }

    public static Collection<AbilityModification> getAllAbilityModifications() {
        return ContentRegistry.getAllAbilityModifications();
    }

    // injected before abilities/moves load; use for shared constants like ALL_TYPES or utility functions
    public static void registerHelperJs(String js) {
        ContentRegistry.registerHelperJs(js);
    }

    public static List<String> getAllHelperJs() {
        return ContentRegistry.getAllHelperJs();
    }

    public static void registerFieldCondition(CustomFieldCondition condition) {
        ContentRegistry.registerFieldCondition(condition);
    }

    @Nullable
    public static CustomFieldCondition getFieldCondition(String id) {
        return ContentRegistry.getFieldCondition(id);
    }

    public static Collection<CustomFieldCondition> getAllFieldConditions() {
        return ContentRegistry.getAllFieldConditions();
    }

    public static void registerVolatileEffect(CustomVolatileEffect effect) {
        ContentRegistry.registerVolatileEffect(effect);
    }

    @Nullable
    public static CustomVolatileEffect getVolatileEffect(String id) {
        return ContentRegistry.getVolatileEffect(id);
    }

    public static Collection<CustomVolatileEffect> getAllVolatileEffects() {
        return ContentRegistry.getAllVolatileEffects();
    }

    public static void registerSideCondition(CustomSideCondition condition) {
        ContentRegistry.registerSideCondition(condition);
    }

    @Nullable
    public static CustomSideCondition getSideCondition(String id) {
        return ContentRegistry.getSideCondition(id);
    }

    public static Collection<CustomSideCondition> getAllSideConditions() {
        return ContentRegistry.getAllSideConditions();
    }

    public static boolean hasCustomAbility(String id) {
        return ContentRegistry.getAbility(id) != null;
    }

    public static boolean hasCustomMove(String id) {
        return ContentRegistry.getMove(id) != null;
    }

    public static boolean hasMoveModification(String moveId) {
        return ContentRegistry.getMoveModification(moveId) != null;
    }

    public static boolean hasAbilityModification(String abilityId) {
        return ContentRegistry.getAbilityModification(abilityId) != null;
    }

    public static boolean hasShowdownJs() {
        return ContentRegistry.hasShowdownJs();
    }

    public static String getAbilityDisplayName(String id) {
        CustomAbility ability = ContentRegistry.getAbility(id);
        return ability != null ? ability.getDisplayName() : formatId(id);
    }

    public static String getMoveDisplayName(String id) {
        CustomMove move = ContentRegistry.getMove(id);
        return move != null ? move.getDisplayName() : formatId(id);
    }

    public static String getFieldConditionDisplayName(String id) {
        CustomFieldCondition condition = ContentRegistry.getFieldCondition(id);
        return condition != null ? condition.getDisplayName() : formatId(id);
    }

    public static String getVolatileEffectDisplayName(String id) {
        CustomVolatileEffect effect = ContentRegistry.getVolatileEffect(id);
        return effect != null ? effect.getDisplayName() : formatId(id);
    }

    public static String getSideConditionDisplayName(String id) {
        CustomSideCondition condition = ContentRegistry.getSideCondition(id);
        return condition != null ? condition.getDisplayName() : formatId(id);
    }

    private static String formatId(String id) {
        if (id == null || id.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : id.toCharArray()) {
            if (c == '_' || c == ' ') {
                result.append(' ');
                capitalizeNext = true;
            } else if (Character.isUpperCase(c)) {
                if (result.length() > 0 && !capitalizeNext) {
                    result.append(' ');
                }
                result.append(c);
                capitalizeNext = false;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }

        return result.toString().trim();
    }
}
