package com.newbulaco.showdown.client.battle;

import net.minecraft.ChatFormatting;

import java.util.*;

public class PokemonBattleStatus {
    private final Map<String, Integer> statStages = new HashMap<>();

    private String typeOverride = "";
    private final Set<String> addedTypes = new HashSet<>();

    private final Set<String> activeEffects = new HashSet<>();

    // effects that stack (e.g. stockpile)
    private final Map<String, Integer> countedEffects = new HashMap<>();

    public PokemonBattleStatus() {
    }

    public void boostStat(String stat, int stages) {
        String normalizedStat = BattleStatusTracker.formatStatName(stat);
        int current = statStages.getOrDefault(normalizedStat, 0);
        int newValue = Math.max(-6, Math.min(6, current + stages));
        if (newValue == 0) {
            statStages.remove(normalizedStat);
        } else {
            statStages.put(normalizedStat, newValue);
        }
    }

    public void clearBoosts(boolean negativeOnly) {
        if (negativeOnly) {
            statStages.entrySet().removeIf(entry -> entry.getValue() < 0);
        } else {
            statStages.clear();
        }
    }

    public Map<String, Integer> getStatStages() {
        return Collections.unmodifiableMap(statStages);
    }

    public int getStatStage(String stat) {
        return statStages.getOrDefault(BattleStatusTracker.formatStatName(stat), 0);
    }

    public void setTypeOverride(String type) {
        typeOverride = type;
        addedTypes.clear();
    }

    public void addType(String type) {
        addedTypes.add(type);
    }

    public String getTypeOverride() {
        return typeOverride;
    }

    public Set<String> getAddedTypes() {
        return Collections.unmodifiableSet(addedTypes);
    }

    public void addEffect(String effect) {
        String normalized = effect.toLowerCase();
        if (normalized.equals("stockpile")) {
            countedEffects.put(normalized, countedEffects.getOrDefault(normalized, 0) + 1);
        } else {
            activeEffects.add(normalized);
        }
    }

    public void removeEffect(String effect) {
        String normalized = effect.toLowerCase();
        countedEffects.remove(normalized);
        activeEffects.remove(normalized);
    }

    public Set<String> getActiveEffects() {
        return Collections.unmodifiableSet(activeEffects);
    }

    public Map<String, Integer> getCountedEffects() {
        return Collections.unmodifiableMap(countedEffects);
    }

    public boolean hasEffect(String effect) {
        String normalized = effect.toLowerCase();
        return activeEffects.contains(normalized) || countedEffects.containsKey(normalized);
    }

    public List<StatDisplay> getStatDisplays() {
        List<StatDisplay> displays = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : statStages.entrySet()) {
            int stage = entry.getValue();
            String multiplier = getStatMultiplierString(entry.getKey(), stage);
            ChatFormatting color = stage > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            String sign = stage > 0 ? "+" : "";
            displays.add(new StatDisplay(entry.getKey() + " " + sign + stage, color, multiplier));
        }

        return displays;
    }

    public List<EffectDisplay> getEffectDisplays() {
        List<EffectDisplay> displays = new ArrayList<>();

        if (!typeOverride.isEmpty()) {
            displays.add(new EffectDisplay(typeOverride, ChatFormatting.LIGHT_PURPLE, "Type changed"));
        }

        for (String type : addedTypes) {
            displays.add(new EffectDisplay("+" + type, ChatFormatting.AQUA, "Type added"));
        }

        for (String effect : activeEffects) {
            displays.add(new EffectDisplay(formatEffectName(effect), ChatFormatting.YELLOW, ""));
        }

        for (Map.Entry<String, Integer> entry : countedEffects.entrySet()) {
            displays.add(new EffectDisplay(
                formatEffectName(entry.getKey()) + " x" + entry.getValue(),
                ChatFormatting.YELLOW,
                ""
            ));
        }

        return displays;
    }

    public boolean hasAnyStatus() {
        return !statStages.isEmpty() ||
               !typeOverride.isEmpty() ||
               !addedTypes.isEmpty() ||
               !activeEffects.isEmpty() ||
               !countedEffects.isEmpty();
    }

    // accuracy/evasion use a different multiplier table than other stats
    private String getStatMultiplierString(String stat, int stage) {
        boolean isAccuracyOrEvasion = stat.equals("Acc") || stat.equals("Eva");
        if (isAccuracyOrEvasion) {
            return switch (stage) {
                case -6 -> "0.33x";
                case -5 -> "0.38x";
                case -4 -> "0.43x";
                case -3 -> "0.5x";
                case -2 -> "0.6x";
                case -1 -> "0.75x";
                case 0 -> "1x";
                case 1 -> "1.33x";
                case 2 -> "1.67x";
                case 3 -> "2x";
                case 4 -> "2.33x";
                case 5 -> "2.67x";
                case 6 -> "3x";
                default -> "";
            };
        } else {
            return switch (stage) {
                case -6 -> "0.25x";
                case -5 -> "0.29x";
                case -4 -> "0.33x";
                case -3 -> "0.4x";
                case -2 -> "0.5x";
                case -1 -> "0.67x";
                case 0 -> "1x";
                case 1 -> "1.5x";
                case 2 -> "2x";
                case 3 -> "2.5x";
                case 4 -> "3x";
                case 5 -> "3.5x";
                case 6 -> "4x";
                default -> "";
            };
        }
    }

    private String formatEffectName(String effect) {
        String[] words = effect.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    public record StatDisplay(String text, ChatFormatting color, String tooltip) {}
    public record EffectDisplay(String text, ChatFormatting color, String tooltip) {}
}
