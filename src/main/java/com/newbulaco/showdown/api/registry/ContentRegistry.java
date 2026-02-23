package com.newbulaco.showdown.api.registry;

import com.newbulaco.showdown.api.content.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// thread-safe for registration during mod loading
public final class ContentRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobblemonShowdown");

    private static final Map<String, CustomAbility> abilities = new ConcurrentHashMap<>();
    private static final Map<String, CustomMove> moves = new ConcurrentHashMap<>();
    private static final Map<String, MoveModification> moveModifications = new ConcurrentHashMap<>();
    private static final Map<String, AbilityModification> abilityModifications = new ConcurrentHashMap<>();
    private static final Map<String, CustomFieldCondition> fieldConditions = new ConcurrentHashMap<>();
    private static final Map<String, CustomVolatileEffect> volatileEffects = new ConcurrentHashMap<>();
    private static final Map<String, CustomSideCondition> sideConditions = new ConcurrentHashMap<>();
    private static final List<String> helperJs = Collections.synchronizedList(new ArrayList<>());

    private ContentRegistry() {}

    public static void registerAbility(CustomAbility ability) {
        String id = ability.getId();
        if (abilities.containsKey(id)) {
            LOGGER.warn("Overwriting existing custom ability: {}", id);
        }
        abilities.put(id, ability);
        LOGGER.info("Registered custom ability: {} ({})", ability.getDisplayName(), id);
    }

    @Nullable
    public static CustomAbility getAbility(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase().replace(" ", "").replace("_", "");
        return abilities.get(normalized);
    }

    public static Collection<CustomAbility> getAllAbilities() {
        return Collections.unmodifiableCollection(abilities.values());
    }

    public static void registerMove(CustomMove move) {
        String id = move.getId();
        if (moves.containsKey(id)) {
            LOGGER.warn("Overwriting existing custom move: {}", id);
        }
        moves.put(id, move);
        LOGGER.info("Registered custom move: {} ({})", move.getDisplayName(), id);
    }

    @Nullable
    public static CustomMove getMove(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase().replace(" ", "").replace("_", "");
        return moves.get(normalized);
    }

    public static Collection<CustomMove> getAllMoves() {
        return Collections.unmodifiableCollection(moves.values());
    }

    public static void registerMoveModification(MoveModification mod) {
        String id = mod.getMoveId();
        if (moveModifications.containsKey(id)) {
            LOGGER.warn("Overwriting existing move modification: {}", id);
        }
        moveModifications.put(id, mod);
        LOGGER.info("Registered move modification: {} (from {})", id, mod.getModId());
    }

    @Nullable
    public static MoveModification getMoveModification(String moveId) {
        if (moveId == null) return null;
        String normalized = moveId.toLowerCase().replace(" ", "").replace("_", "");
        return moveModifications.get(normalized);
    }

    public static Collection<MoveModification> getAllMoveModifications() {
        return Collections.unmodifiableCollection(moveModifications.values());
    }

    public static void registerAbilityModification(AbilityModification mod) {
        String id = mod.getAbilityId();
        if (abilityModifications.containsKey(id)) {
            LOGGER.warn("Overwriting existing ability modification: {}", id);
        }
        abilityModifications.put(id, mod);
        LOGGER.info("Registered ability modification: {} (from {})", id, mod.getModId());
    }

    @Nullable
    public static AbilityModification getAbilityModification(String abilityId) {
        if (abilityId == null) return null;
        String normalized = abilityId.toLowerCase().replace(" ", "").replace("_", "");
        return abilityModifications.get(normalized);
    }

    public static Collection<AbilityModification> getAllAbilityModifications() {
        return Collections.unmodifiableCollection(abilityModifications.values());
    }

    // injected before abilities/moves load; use for shared constants or utility functions
    public static void registerHelperJs(String js) {
        if (js != null && !js.isEmpty()) {
            helperJs.add(js);
            LOGGER.info("Registered helper JavaScript ({} chars)", js.length());
        }
    }

    public static List<String> getAllHelperJs() {
        return Collections.unmodifiableList(new ArrayList<>(helperJs));
    }

    public static void registerFieldCondition(CustomFieldCondition condition) {
        String id = condition.getId();
        if (fieldConditions.containsKey(id)) {
            LOGGER.warn("Overwriting existing custom field condition: {}", id);
        }
        fieldConditions.put(id, condition);
        LOGGER.info("Registered custom field condition: {} ({})", condition.getDisplayName(), id);
    }

    @Nullable
    public static CustomFieldCondition getFieldCondition(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase().replace(" ", "").replace("_", "");
        return fieldConditions.get(normalized);
    }

    public static Collection<CustomFieldCondition> getAllFieldConditions() {
        return Collections.unmodifiableCollection(fieldConditions.values());
    }

    public static void registerVolatileEffect(CustomVolatileEffect effect) {
        String id = effect.getId();
        if (volatileEffects.containsKey(id)) {
            LOGGER.warn("Overwriting existing custom volatile effect: {}", id);
        }
        volatileEffects.put(id, effect);
        LOGGER.info("Registered custom volatile effect: {} ({})", effect.getDisplayName(), id);
    }

    @Nullable
    public static CustomVolatileEffect getVolatileEffect(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase().replace(" ", "").replace("_", "");
        return volatileEffects.get(normalized);
    }

    public static Collection<CustomVolatileEffect> getAllVolatileEffects() {
        return Collections.unmodifiableCollection(volatileEffects.values());
    }

    public static void registerSideCondition(CustomSideCondition condition) {
        String id = condition.getId();
        if (sideConditions.containsKey(id)) {
            LOGGER.warn("Overwriting existing custom side condition: {}", id);
        }
        sideConditions.put(id, condition);
        LOGGER.info("Registered custom side condition: {} ({})", condition.getDisplayName(), id);
    }

    @Nullable
    public static CustomSideCondition getSideCondition(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase().replace(" ", "").replace("_", "");
        return sideConditions.get(normalized);
    }

    public static Collection<CustomSideCondition> getAllSideConditions() {
        return Collections.unmodifiableCollection(sideConditions.values());
    }

    public static int getTotalRegistrations() {
        return abilities.size() + moves.size() + moveModifications.size() + abilityModifications.size()
             + fieldConditions.size() + volatileEffects.size() + sideConditions.size();
    }

    public static void logRegistrationSummary() {
        LOGGER.info("Custom content registered: {} abilities, {} moves, {} move mods, {} ability mods, {} field conditions, {} volatile effects, {} side conditions, {} helper scripts",
                abilities.size(), moves.size(), moveModifications.size(), abilityModifications.size(), fieldConditions.size(),
                volatileEffects.size(), sideConditions.size(), helperJs.size());
    }

    public static boolean hasShowdownJs() {
        for (CustomAbility ability : abilities.values()) {
            if (ability.hasShowdownJs()) return true;
        }
        for (CustomMove move : moves.values()) {
            if (move.hasShowdownJs()) return true;
        }
        for (MoveModification mod : moveModifications.values()) {
            if (mod.hasShowdownJs()) return true;
        }
        for (AbilityModification mod : abilityModifications.values()) {
            if (mod.hasShowdownJs()) return true;
        }
        for (CustomVolatileEffect effect : volatileEffects.values()) {
            if (effect.hasShowdownJs()) return true;
        }
        for (CustomSideCondition condition : sideConditions.values()) {
            if (condition.hasShowdownJs()) return true;
        }
        for (CustomFieldCondition fc : fieldConditions.values()) {
            if (fc.hasShowdownJs()) return true;
        }
        return !helperJs.isEmpty();
    }
}
