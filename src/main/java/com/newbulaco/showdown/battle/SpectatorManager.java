package com.newbulaco.showdown.battle;

import com.newbulaco.showdown.network.ShowdownNetwork;
import com.newbulaco.showdown.network.packets.SpectatorStatePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpectatorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpectatorManager.class);

    private final ShowdownBattle battle;
    private final Set<UUID> spectators = new HashSet<>();
    private boolean spectatingEnabled = true;
    private boolean notifyBattlersOfSpectators = true;

    public SpectatorManager(ShowdownBattle battle) {
        this.battle = battle;
    }

    public boolean addSpectator(ServerPlayer player) {
        if (!spectatingEnabled) {
            player.sendSystemMessage(Component.literal("§cSpectating is disabled for this battle"));
            return false;
        }

        if (battle.isParticipant(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cYou cannot spectate your own battle"));
            return false;
        }

        if (battle.isCompleted()) {
            player.sendSystemMessage(Component.literal("§cThis battle has already ended"));
            return false;
        }

        if (spectators.add(player.getUUID())) {
            LOGGER.info("Player {} is now spectating battle {}",
                    player.getName().getString(), battle.getBattleId());

            player.sendSystemMessage(Component.literal(String.format(
                    "§aYou are now spectating %s vs %s",
                    battle.getPlayer1().getName().getString(),
                    battle.getPlayer2().getName().getString()
            )));

            if (notifyBattlersOfSpectators) {
                notifyBattlers(String.format("§7%s is now spectating", player.getName().getString()));
            }

            sendSpectatorJoinedPacket(player);

            return true;
        }

        return false;
    }

    public boolean removeSpectator(ServerPlayer player) {
        if (spectators.remove(player.getUUID())) {
            LOGGER.info("Player {} stopped spectating battle {}",
                    player.getName().getString(), battle.getBattleId());

            player.sendSystemMessage(Component.literal("§7You stopped spectating the battle"));

            if (notifyBattlersOfSpectators) {
                notifyBattlers(String.format("§7%s stopped spectating", player.getName().getString()));
            }

            SpectatorStatePacket leftPacket = SpectatorStatePacket.left(battle.getBattleId());
            ShowdownNetwork.sendToPlayer(leftPacket, player);

            return true;
        }

        return false;
    }

    public boolean removeSpectator(UUID playerUuid) {
        boolean removed = spectators.remove(playerUuid);
        if (removed) {
            LOGGER.info("Removed spectator {} from battle {} (disconnected)",
                    playerUuid, battle.getBattleId());
        }
        return removed;
    }

    public void clearSpectators() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            spectators.clear();
            return;
        }

        for (UUID spectatorUuid : new HashSet<>(spectators)) {
            ServerPlayer spectator = server.getPlayerList().getPlayer(spectatorUuid);
            if (spectator != null) {
                spectator.sendSystemMessage(Component.literal("§7The battle you were watching has ended"));

                SpectatorStatePacket leftPacket = SpectatorStatePacket.left(battle.getBattleId());
                ShowdownNetwork.sendToPlayer(leftPacket, spectator);
            }
        }

        spectators.clear();
        LOGGER.info("Cleared all spectators from battle {}", battle.getBattleId());
    }

    public boolean isSpectating(UUID playerUuid) {
        return spectators.contains(playerUuid);
    }

    public int getSpectatorCount() {
        return spectators.size();
    }

    public Set<UUID> getSpectators() {
        return new HashSet<>(spectators);
    }

    public void setSpectatingEnabled(boolean enabled) {
        this.spectatingEnabled = enabled;

        if (!enabled) {
            clearSpectators();
        }
    }

    public boolean isSpectatingEnabled() {
        return spectatingEnabled;
    }

    public void setNotifyBattlersOfSpectators(boolean notify) {
        this.notifyBattlersOfSpectators = notify;
    }

    private void notifyBattlers(String message) {
        if (battle.getPlayer1() != null) {
            battle.getPlayer1().sendSystemMessage(Component.literal(message));
        }
        if (battle.getPlayer2() != null) {
            battle.getPlayer2().sendSystemMessage(Component.literal(message));
        }
    }

    public void broadcastToSpectators(String message) {
        if (spectators.isEmpty()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        SpectatorStatePacket messagePacket = SpectatorStatePacket.battleMessage(
                battle.getBattleId(), message);

        for (UUID spectatorUuid : spectators) {
            ServerPlayer spectator = server.getPlayerList().getPlayer(spectatorUuid);
            if (spectator != null) {
                spectator.sendSystemMessage(Component.literal("§7[Battle] §f" + message));
                ShowdownNetwork.sendToPlayer(messagePacket, spectator);
            }
        }

        LOGGER.debug("Broadcast to {} spectators: {}", spectators.size(), message);
    }

    public void notifyPokemonFainted(String pokemonName, String ownerName) {
        if (spectators.isEmpty()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        SpectatorStatePacket faintPacket = SpectatorStatePacket.pokemonFainted(
                battle.getBattleId(), pokemonName, ownerName);

        for (UUID spectatorUuid : spectators) {
            ServerPlayer spectator = server.getPlayerList().getPlayer(spectatorUuid);
            if (spectator != null) {
                spectator.sendSystemMessage(Component.literal(
                        "§7[Battle] §f" + ownerName + "'s " + pokemonName + " fainted!"));
                ShowdownNetwork.sendToPlayer(faintPacket, spectator);
            }
        }
    }

    private void sendSpectatorJoinedPacket(ServerPlayer spectator) {
        int turnNumber = 0;
        if (battle.getCobblemonBattle() != null) {
            turnNumber = battle.getCobblemonBattle().getTurn();
        }

        SpectatorStatePacket joinedPacket = SpectatorStatePacket.joined(
                battle.getBattleId(),
                battle.getPlayer1().getName().getString(),
                battle.getPlayer2().getName().getString(),
                battle.getFormat().getName(),
                turnNumber,
                getSpectatorCount()
        );

        ShowdownNetwork.sendToPlayer(joinedPacket, spectator);
    }

    public void updateSpectators() {
        if (spectators.isEmpty() || battle.getCobblemonBattle() == null) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        int turnNumber = battle.getCobblemonBattle().getTurn();

        SpectatorStatePacket updatePacket = SpectatorStatePacket.battleUpdate(
                battle.getBattleId(),
                battle.getPlayer1().getName().getString(),
                battle.getPlayer2().getName().getString(),
                turnNumber,
                getSpectatorCount()
        );

        for (UUID spectatorUuid : spectators) {
            ServerPlayer spectator = server.getPlayerList().getPlayer(spectatorUuid);
            if (spectator != null) {
                ShowdownNetwork.sendToPlayer(updatePacket, spectator);
            }
        }

        LOGGER.debug("Updated {} spectators with battle state", spectators.size());
    }
}
