package com.newbulaco.showdown.client.battle;

import com.newbulaco.showdown.network.packets.StatChangePacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientStatChangeManager {

    private static ClientStatChangeManager instance;

    // battle ID -> pokemon ID -> stat -> stages
    private final Map<UUID, Map<UUID, Map<String, Integer>>> battleStats = new ConcurrentHashMap<>();

    // battle ID -> pokemon ID -> isAlly flag
    private final Map<UUID, Map<UUID, Boolean>> pokemonSides = new ConcurrentHashMap<>();

    private UUID currentBattleId = null;

    private ClientStatChangeManager() {}

    public static ClientStatChangeManager getInstance() {
        if (instance == null) {
            instance = new ClientStatChangeManager();
        }
        return instance;
    }

    public void handlePacket(StatChangePacket packet) {
        UUID battleId = packet.getBattleId();
        UUID pokemonId = packet.getPokemonId();

        currentBattleId = battleId;

        battleStats.computeIfAbsent(battleId, k -> new ConcurrentHashMap<>());
        pokemonSides.computeIfAbsent(battleId, k -> new ConcurrentHashMap<>());

        Map<UUID, Map<String, Integer>> pokemonStats = battleStats.get(battleId);
        Map<UUID, Boolean> sides = pokemonSides.get(battleId);

        sides.put(pokemonId, packet.isAlly());
        pokemonStats.put(pokemonId, new ConcurrentHashMap<>(packet.getStatStages()));
    }

    public Map<String, Integer> getAllyStatStages() {
        if (currentBattleId == null) return Collections.emptyMap();

        Map<UUID, Map<String, Integer>> pokemonStats = battleStats.get(currentBattleId);
        Map<UUID, Boolean> sides = pokemonSides.get(currentBattleId);

        if (pokemonStats == null || sides == null) return Collections.emptyMap();

        for (Map.Entry<UUID, Boolean> entry : sides.entrySet()) {
            if (entry.getValue()) {
                Map<String, Integer> stats = pokemonStats.get(entry.getKey());
                if (stats != null) {
                    return new HashMap<>(stats);
                }
            }
        }
        return Collections.emptyMap();
    }

    public Map<String, Integer> getOpponentStatStages() {
        if (currentBattleId == null) return Collections.emptyMap();

        Map<UUID, Map<String, Integer>> pokemonStats = battleStats.get(currentBattleId);
        Map<UUID, Boolean> sides = pokemonSides.get(currentBattleId);

        if (pokemonStats == null || sides == null) return Collections.emptyMap();

        for (Map.Entry<UUID, Boolean> entry : sides.entrySet()) {
            if (!entry.getValue()) {
                Map<String, Integer> stats = pokemonStats.get(entry.getKey());
                if (stats != null) {
                    return new HashMap<>(stats);
                }
            }
        }
        return Collections.emptyMap();
    }

    public void clearBattle(UUID battleId) {
        battleStats.remove(battleId);
        pokemonSides.remove(battleId);
        if (battleId.equals(currentBattleId)) {
            currentBattleId = null;
        }
    }

    public void clearAll() {
        battleStats.clear();
        pokemonSides.clear();
        currentBattleId = null;
    }

    public static String getStatDisplayName(String statAbbr) {
        if (statAbbr == null) return "";
        return switch (statAbbr.toLowerCase()) {
            case "atk" -> "Atk";
            case "def" -> "Def";
            case "spa" -> "SpA";
            case "spd" -> "SpD";
            case "spe" -> "Spe";
            case "accuracy" -> "Acc";
            case "evasion" -> "Eva";
            default -> statAbbr;
        };
    }
}
