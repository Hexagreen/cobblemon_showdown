package com.newbulaco.showdown.battle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.newbulaco.showdown.CobblemonShowdown;
import com.newbulaco.showdown.network.ShowdownNetwork;
import com.newbulaco.showdown.network.packets.BattleClearPacket;
import kotlin.Unit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class CobblemonBattleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CobblemonBattleListener.class);

    public static void register() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(
                Priority.NORMAL,
                event -> {
                    handleBattleVictory(event.getBattle(), event.getWinners(), event.getLosers());
                    return Unit.INSTANCE;
                }
        );

        CobblemonEvents.BATTLE_FAINTED.subscribe(
                Priority.NORMAL,
                event -> {
                    handleBattleFainted(event.getBattle(), event.getKilled());
                    return Unit.INSTANCE;
                }
        );

        CobblemonEvents.BATTLE_FLED.subscribe(
                Priority.NORMAL,
                event -> {
                    handleBattleFled(event.getBattle(), event.getPlayer());
                    return Unit.INSTANCE;
                }
        );

        // fallback loot drop prevention (primary is in DropTableMixin)
        CobblemonEvents.LOOT_DROPPED.subscribe(
                Priority.HIGHEST,
                event -> {
                    if (event.getEntity() instanceof PokemonEntity pokemonEntity) {
                        if (isPokemonInShowdownBattle(pokemonEntity)) {
                            LOGGER.debug("Blocked LOOT_DROPPED for Showdown Pokemon: {}",
                                    pokemonEntity.getDisplayName().getString());
                            event.cancel();
                        }
                    }
                    return Unit.INSTANCE;
                }
        );

        CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.subscribe(
                Priority.HIGHEST,
                event -> {
                    PokemonEntity pokemonEntity = event.getPokemonEntity();
                    if (isPokemonInShowdownBattle(pokemonEntity)) {
                        LOGGER.debug("Prevented save of Showdown battle Pokemon: {}",
                                pokemonEntity.getDisplayName().getString());
                        event.cancel();
                    }
                    return Unit.INSTANCE;
                }
        );

        // only blocks ORIGINAL Pokemon, not clones used in battle
        CobblemonEvents.HELD_ITEM_UPDATED.subscribe(
                Priority.HIGHEST,
                event -> {
                    Pokemon pokemon = event.getPokemon();
                    if (isPokemonInShowdownBattleByPokemon(pokemon)) {
                        LOGGER.debug("Blocked held item change for original Pokemon in battle: {} (UUID: {})",
                                pokemon.getSpecies().getName(), pokemon.getUuid());
                        event.cancel();
                    }
                    return Unit.INSTANCE;
                }
        );

        CobblemonEvents.HELD_ITEM_PRE.subscribe(
                Priority.HIGHEST,
                event -> {
                    Pokemon pokemon = event.getPokemon();
                    if (isPokemonInShowdownBattleByPokemon(pokemon)) {
                        LOGGER.debug("Blocked held item swap for original Pokemon in battle: {} (UUID: {})",
                                pokemon.getSpecies().getName(), pokemon.getUuid());
                        event.cancel();
                    }
                    return Unit.INSTANCE;
                }
        );

        LOGGER.info("Registered Cobblemon battle event listeners");
    }

    private static boolean isPokemonInShowdownBattle(PokemonEntity pokemonEntity) {
        BattleManager battleManager = CobblemonShowdown.getBattleManager();
        if (battleManager == null) {
            return false;
        }

        UUID battleId = pokemonEntity.getBattleId();
        boolean isBattling = pokemonEntity.isBattling();
        UUID ownerUuid = pokemonEntity.getOwnerUUID();

        Pokemon pokemon = pokemonEntity.getPokemon();
        if (pokemon != null && battleManager.isPokemonInShowdownBattle(pokemon.getUuid())) {
            LOGGER.debug("Pokemon {} matched by UUID tracking", pokemonEntity.getDisplayName().getString());
            return true;
        }

        if (battleId != null && battleManager.isCobblemonBattleShowdown(battleId)) {
            LOGGER.debug("Pokemon {} matched by persistent battle ID: {}",
                    pokemonEntity.getDisplayName().getString(), battleId);
            return true;
        }

        if (ownerUuid != null) {
            for (ShowdownBattle battle : battleManager.getActiveBattles().values()) {
                if ((battle.getPlayer1() != null && battle.getPlayer1().getUUID().equals(ownerUuid)) ||
                    (battle.getPlayer2() != null && battle.getPlayer2().getUUID().equals(ownerUuid))) {
                    LOGGER.debug("Pokemon {} matched by owner UUID: {}",
                            pokemonEntity.getDisplayName().getString(), ownerUuid);
                    return true;
                }
            }
        }

        if (isBattling) {
            for (ShowdownBattle showdownBattle : battleManager.getActiveBattles().values()) {
                PokemonBattle cobblemonBattle = showdownBattle.getCobblemonBattle();
                if (cobblemonBattle != null) {
                    if (isPokemonEntityInCobblemonBattle(pokemonEntity, cobblemonBattle)) {
                        LOGGER.debug("Pokemon {} matched by direct Cobblemon battle check",
                                pokemonEntity.getDisplayName().getString());
                        return true;
                    }
                }
            }
        }

        if (isBattling && battleId != null) {
            for (ShowdownBattle showdownBattle : battleManager.getActiveBattles().values()) {
                PokemonBattle cobblemonBattle = showdownBattle.getCobblemonBattle();
                if (cobblemonBattle != null && cobblemonBattle.getBattleId().equals(battleId)) {
                    battleManager.registerCobblemonBattleId(battleId);
                    LOGGER.debug("Pokemon {} matched by active battle comparison",
                            pokemonEntity.getDisplayName().getString());
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isPokemonEntityInCobblemonBattle(PokemonEntity entity,
            PokemonBattle battle) {
        Pokemon pokemon = entity.getPokemon();
        if (pokemon == null) {
            return false;
        }

        UUID pokemonUuid = pokemon.getUuid();

        for (BattleActor actor : battle.getActors()) {
            for (BattlePokemon battlePokemon : actor.getPokemonList()) {
                if (battlePokemon.getEffectedPokemon().getUuid().equals(pokemonUuid)) {
                    return true;
                }
                if (battlePokemon.getOriginalPokemon().getUuid().equals(pokemonUuid)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Only uses UUID-based tracking -- we intentionally avoid owner-based fallback because
     * it incorrectly blocks cloned Pokemon (which should consume items normally in battle)
     * and blocked item restoration when battles were still active.
     */
    private static boolean isPokemonInShowdownBattleByPokemon(Pokemon pokemon) {
        BattleManager battleManager = CobblemonShowdown.getBattleManager();
        if (battleManager == null) {
            return false;
        }

        // cloned Pokemon (used in battle) have different UUIDs and should NOT be blocked
        return battleManager.isPokemonInShowdownBattle(pokemon.getUuid());
    }

    private static void handleBattleVictory(PokemonBattle battle,
                                             List<? extends BattleActor> winners,
                                             List<? extends BattleActor> losers) {
        BattleManager battleManager = CobblemonShowdown.getBattleManager();
        if (battleManager == null) {
            return;
        }

        UUID cobblemonBattleId = battle.getBattleId();
        ShowdownBattle showdownBattle = findShowdownBattle(battleManager, cobblemonBattleId);

        if (showdownBattle == null) {
            return;
        }

        LOGGER.info("Showdown battle {} ended via Cobblemon event", showdownBattle.getBattleId());

        UUID winnerUuid = null;
        for (BattleActor winner : winners) {
            if (winner instanceof PlayerBattleActor) {
                winnerUuid = ((PlayerBattleActor) winner).getUuid();
                break;
            }
        }

        sendBattleClearPackets(showdownBattle, cobblemonBattleId);

        battleManager.onBattleEnd(showdownBattle.getBattleId(), winnerUuid);
    }

    private static void handleBattleFled(PokemonBattle battle, PlayerBattleActor fleeingPlayer) {
        BattleManager battleManager = CobblemonShowdown.getBattleManager();
        if (battleManager == null) {
            return;
        }

        UUID cobblemonBattleId = battle.getBattleId();
        ShowdownBattle showdownBattle = findShowdownBattle(battleManager, cobblemonBattleId);

        if (showdownBattle == null) {
            UUID fleeingUuid = fleeingPlayer.getUuid();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(fleeingUuid);
                if (player != null) {
                    ShowdownNetwork.sendToPlayer(new BattleClearPacket(cobblemonBattleId), player);
                }
            }
            return;
        }

        LOGGER.info("Showdown battle {} fled by player", showdownBattle.getBattleId());

        sendBattleClearPackets(showdownBattle, cobblemonBattleId);

        UUID fleeingUuid = fleeingPlayer.getUuid();
        UUID winnerUuid = showdownBattle.getPlayer1().getUUID().equals(fleeingUuid)
                ? showdownBattle.getPlayer2().getUUID()
                : showdownBattle.getPlayer1().getUUID();

        battleManager.onBattleEnd(showdownBattle.getBattleId(), winnerUuid);
    }

    private static void sendBattleClearPackets(ShowdownBattle showdownBattle, UUID cobblemonBattleId) {
        BattleClearPacket clearPacket = new BattleClearPacket(cobblemonBattleId);

        if (showdownBattle.getPlayer1() != null) {
            ShowdownNetwork.sendToPlayer(clearPacket, showdownBattle.getPlayer1());
        }
        if (showdownBattle.getPlayer2() != null) {
            ShowdownNetwork.sendToPlayer(clearPacket, showdownBattle.getPlayer2());
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (UUID spectatorUuid : showdownBattle.getSpectatorManager().getSpectators()) {
                ServerPlayer spectator = server.getPlayerList().getPlayer(spectatorUuid);
                if (spectator != null) {
                    ShowdownNetwork.sendToPlayer(clearPacket, spectator);
                }
            }
        }

        LOGGER.debug("Sent battle clear packets for battle {}", cobblemonBattleId);
    }

    private static void handleBattleFainted(PokemonBattle battle,
                                             BattlePokemon killed) {
        BattleManager battleManager = CobblemonShowdown.getBattleManager();
        if (battleManager == null) {
            return;
        }

        UUID cobblemonBattleId = battle.getBattleId();
        ShowdownBattle showdownBattle = findShowdownBattle(battleManager, cobblemonBattleId);

        if (showdownBattle == null) {
            return;
        }

        String killedName = killed.getEffectedPokemon().getSpecies().getName();
        String ownerName = "Unknown";

        BattleActor actor = killed.getActor();
        if (actor instanceof PlayerBattleActor) {
            UUID ownerUuid = ((PlayerBattleActor) actor).getUuid();
            if (showdownBattle.getPlayer1().getUUID().equals(ownerUuid)) {
                ownerName = showdownBattle.getPlayer1().getName().getString();
            } else if (showdownBattle.getPlayer2().getUUID().equals(ownerUuid)) {
                ownerName = showdownBattle.getPlayer2().getName().getString();
            }
        }

        LOGGER.debug("In battle {}: {}'s {} fainted", showdownBattle.getBattleId(), ownerName, killedName);

        showdownBattle.getSpectatorManager().notifyPokemonFainted(killedName, ownerName);
        VolatileEffectTracker.getInstance().forcePartyStatusUpdate(battle);
    }

    private static ShowdownBattle findShowdownBattle(BattleManager battleManager, UUID cobblemonBattleId) {
        for (ShowdownBattle battle : battleManager.getActiveBattles().values()) {
            if (battle.getCobblemonBattle() != null &&
                    battle.getCobblemonBattle().getBattleId().equals(cobblemonBattleId)) {
                return battle;
            }
        }
        return null;
    }
}
