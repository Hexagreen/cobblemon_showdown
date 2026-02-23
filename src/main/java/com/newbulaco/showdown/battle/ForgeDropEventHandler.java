package com.newbulaco.showdown.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.newbulaco.showdown.CobblemonShowdown;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

// fallback for preventing item drops -- primary prevention is in DropTableMixin
@Mod.EventBusSubscriber(modid = CobblemonShowdown.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeDropEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeDropEventHandler.class);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof PokemonEntity pokemonEntity)) {
            return;
        }

        if (isPokemonInShowdownBattle(pokemonEntity)) {
            event.getDrops().clear();
            event.setCanceled(true);
            LOGGER.debug("Blocked LivingDropsEvent for Showdown Pokemon: {}",
                    pokemonEntity.getDisplayName().getString());
        }
    }

    private static boolean isPokemonInShowdownBattle(PokemonEntity pokemonEntity) {
        BattleManager battleManager = CobblemonShowdown.getBattleManager();
        if (battleManager == null) {
            return false;
        }

        UUID battleId = pokemonEntity.getBattleId();
        boolean isBattling = pokemonEntity.isBattling();
        UUID ownerUuid = pokemonEntity.getOwnerUUID();

        if (battleId != null && battleManager.isCobblemonBattleShowdown(battleId)) {
            return true;
        }

        if (ownerUuid != null) {
            for (ShowdownBattle battle : battleManager.getActiveBattles().values()) {
                if ((battle.getPlayer1() != null && battle.getPlayer1().getUUID().equals(ownerUuid)) ||
                    (battle.getPlayer2() != null && battle.getPlayer2().getUUID().equals(ownerUuid))) {
                    return true;
                }
            }
        }

        if (isBattling) {
            for (ShowdownBattle showdownBattle : battleManager.getActiveBattles().values()) {
                PokemonBattle cobblemonBattle = showdownBattle.getCobblemonBattle();
                if (cobblemonBattle != null) {
                    if (isPokemonEntityInCobblemonBattle(pokemonEntity, cobblemonBattle)) {
                        return true;
                    }
                }
            }
        }

        // catches cases where getBattleId() returns a value we haven't seen before
        if (isBattling && battleId != null) {
            for (ShowdownBattle showdownBattle : battleManager.getActiveBattles().values()) {
                PokemonBattle cobblemonBattle = showdownBattle.getCobblemonBattle();
                if (cobblemonBattle != null && cobblemonBattle.getBattleId().equals(battleId)) {
                    battleManager.registerCobblemonBattleId(battleId);
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isPokemonEntityInCobblemonBattle(PokemonEntity entity, PokemonBattle battle) {
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
}
