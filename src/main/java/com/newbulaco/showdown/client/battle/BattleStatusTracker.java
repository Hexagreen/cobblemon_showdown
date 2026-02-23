package com.newbulaco.showdown.client.battle;

import java.util.*;

public class BattleStatusTracker {
    private static final Map<String, PokemonBattleStatus> pokemonStatusMap = new HashMap<>();

    private static String weather = "";
    private static int weatherTurns = -1;
    private static String terrain = "";
    private static int terrainTurns = -1;
    private static final Map<String, Integer> rooms = new HashMap<>();
    private static final Map<String, Integer> allyHazards = new HashMap<>();
    private static final Map<String, Integer> enemyHazards = new HashMap<>();

    // Wheel of Dharma adapted type tracking (for Mahoraga)
    private static String allyAdaptedType = "";
    private static String enemyAdaptedType = "";

    public static void addPokemon(String pokemonId) {
        if (!pokemonStatusMap.containsKey(pokemonId)) {
            pokemonStatusMap.put(pokemonId, new PokemonBattleStatus());
        }
    }

    public static void removePokemon(String pokemonId) {
        pokemonStatusMap.remove(pokemonId);
    }

    public static PokemonBattleStatus getPokemonStatus(String pokemonId) {
        return pokemonStatusMap.getOrDefault(pokemonId, new PokemonBattleStatus());
    }

    public static void boostStat(String pokemonId, String stat, int stages) {
        if (pokemonStatusMap.containsKey(pokemonId)) {
            pokemonStatusMap.get(pokemonId).boostStat(stat, stages);
        }
    }

    public static void clearBoosts(String pokemonId, boolean negativeOnly) {
        if (pokemonStatusMap.containsKey(pokemonId)) {
            pokemonStatusMap.get(pokemonId).clearBoosts(negativeOnly);
        }
    }

    public static void addEffect(String pokemonId, String effect) {
        if (pokemonStatusMap.containsKey(pokemonId)) {
            pokemonStatusMap.get(pokemonId).addEffect(effect);
        }
    }

    public static void removeEffect(String pokemonId, String effect) {
        if (pokemonStatusMap.containsKey(pokemonId)) {
            pokemonStatusMap.get(pokemonId).removeEffect(effect);
        }
    }

    public static void setTypeOverride(String pokemonId, String type) {
        if (pokemonStatusMap.containsKey(pokemonId)) {
            pokemonStatusMap.get(pokemonId).setTypeOverride(type);
        }
    }

    public static void addType(String pokemonId, String type) {
        if (pokemonStatusMap.containsKey(pokemonId)) {
            pokemonStatusMap.get(pokemonId).addType(type);
        }
    }

    public static void setWeather(String newWeather) {
        weather = newWeather;
        weatherTurns = newWeather.isEmpty() ? -1 : 5;
    }

    public static void setWeather(String newWeather, int turns) {
        weather = newWeather;
        weatherTurns = turns;
    }

    public static String getWeather() {
        return weather;
    }

    public static int getWeatherTurns() {
        return weatherTurns;
    }

    public static void setTerrain(String newTerrain) {
        terrain = newTerrain;
        terrainTurns = newTerrain.isEmpty() ? -1 : 5;
    }

    public static String getTerrain() {
        return terrain;
    }

    public static int getTerrainTurns() {
        return terrainTurns;
    }

    public static void addRoom(String room) {
        rooms.put(room, 5);
    }

    public static void setRoom(String room, int turns) {
        rooms.put(room, turns);
    }

    public static void removeRoom(String room) {
        rooms.remove(room);
    }

    public static Map<String, Integer> getRooms() {
        return Collections.unmodifiableMap(rooms);
    }

    public static void setTerrain(String newTerrain, int turns) {
        terrain = newTerrain;
        terrainTurns = turns;
    }

    public static void addHazard(boolean isAlly, String hazard) {
        Map<String, Integer> targetMap = isAlly ? allyHazards : enemyHazards;
        targetMap.put(hazard, targetMap.getOrDefault(hazard, 0) + 1);
    }

    public static void addAllyHazard(String hazard, int stacks) {
        allyHazards.put(hazard, stacks);
    }

    public static void addEnemyHazard(String hazard, int stacks) {
        enemyHazards.put(hazard, stacks);
    }

    public static void removeHazard(boolean isAlly, String hazard) {
        Map<String, Integer> targetMap = isAlly ? allyHazards : enemyHazards;
        targetMap.remove(hazard);
    }

    public static void removeAllyHazard(String hazard) {
        allyHazards.remove(hazard);
    }

    public static void removeEnemyHazard(String hazard) {
        enemyHazards.remove(hazard);
    }

    public static void clearHazards(boolean isAlly) {
        if (isAlly) {
            allyHazards.clear();
        } else {
            enemyHazards.clear();
        }
    }

    public static Map<String, Integer> getAllyHazards() {
        return Collections.unmodifiableMap(allyHazards);
    }

    public static Map<String, Integer> getEnemyHazards() {
        return Collections.unmodifiableMap(enemyHazards);
    }

    public static void setAllyAdaptedType(String type) {
        allyAdaptedType = type != null ? type : "";
    }

    public static void setEnemyAdaptedType(String type) {
        enemyAdaptedType = type != null ? type : "";
    }

    public static String getAllyAdaptedType() {
        return allyAdaptedType;
    }

    public static String getEnemyAdaptedType() {
        return enemyAdaptedType;
    }

    public static void clearAdaptedTypes() {
        allyAdaptedType = "";
        enemyAdaptedType = "";
    }

    public static void onTurnEnd() {
        if (weatherTurns > 0) {
            weatherTurns--;
            if (weatherTurns == 0) {
                weather = "";
                weatherTurns = -1;
            }
        }

        if (terrainTurns > 0) {
            terrainTurns--;
            if (terrainTurns == 0) {
                terrain = "";
                terrainTurns = -1;
            }
        }

        List<String> expiredRooms = new ArrayList<>();
        rooms.replaceAll((k, v) -> v - 1);
        rooms.forEach((room, turns) -> {
            if (turns <= 0) expiredRooms.add(room);
        });
        expiredRooms.forEach(rooms::remove);
    }

    public static void onBattleStart() {
        clearAll();
    }

    public static void onBattleEnd() {
        clearAll();
    }

    public static void clearAll() {
        pokemonStatusMap.clear();
        weather = "";
        weatherTurns = -1;
        terrain = "";
        terrainTurns = -1;
        rooms.clear();
        allyHazards.clear();
        enemyHazards.clear();
        allyAdaptedType = "";
        enemyAdaptedType = "";
    }

    public static String formatStatName(String stat) {
        return switch (stat.toLowerCase()) {
            case "attack", "atk" -> "Atk";
            case "defence", "defense", "def" -> "Def";
            case "special_attack", "spa", "spatk" -> "SpA";
            case "special_defence", "special_defense", "spd", "spdef" -> "SpD";
            case "speed", "spe" -> "Spe";
            case "accuracy", "acc" -> "Acc";
            case "evasion", "eva" -> "Eva";
            default -> stat;
        };
    }

    public static int parseSeverity(String severity) {
        return switch (severity.toLowerCase()) {
            case "slight" -> 1;
            case "sharp" -> 2;
            case "severe" -> 3;
            case "max" -> 12;
            default -> 1;
        };
    }
}
