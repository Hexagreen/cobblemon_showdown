package com.newbulaco.showdown.client;

import com.newbulaco.showdown.network.packets.SpectatorStatePacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@OnlyIn(Dist.CLIENT)
public class ClientSpectatorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSpectatorManager.class);

    private static ClientSpectatorManager instance;

    @Nullable
    private SpectatedBattle spectatedBattle;

    private final List<String> battleMessages = new ArrayList<>();
    private static final int MAX_MESSAGES = 50;

    private final List<SpectatorListener> listeners = new CopyOnWriteArrayList<>();

    private ClientSpectatorManager() {}

    public static ClientSpectatorManager getInstance() {
        if (instance == null) {
            instance = new ClientSpectatorManager();
        }
        return instance;
    }

    public void handleSpectatorState(SpectatorStatePacket packet) {
        switch (packet.getAction()) {
            case JOINED -> {
                spectatedBattle = new SpectatedBattle(
                        packet.getBattleId(),
                        packet.getPlayer1Name(),
                        packet.getPlayer2Name(),
                        packet.getFormatName(),
                        packet.getTurnNumber(),
                        packet.getSpectatorCount()
                );
                battleMessages.clear();
                LOGGER.debug("Now spectating: {} vs {}", packet.getPlayer1Name(), packet.getPlayer2Name());
                notifySpectatingStarted(spectatedBattle);
            }
            case LEFT -> {
                if (spectatedBattle != null && spectatedBattle.battleId.equals(packet.getBattleId())) {
                    notifySpectatingEnded(spectatedBattle);
                    spectatedBattle = null;
                    battleMessages.clear();
                    LOGGER.debug("Stopped spectating battle");
                }
            }
            case BATTLE_UPDATE -> {
                if (spectatedBattle != null && spectatedBattle.battleId.equals(packet.getBattleId())) {
                    spectatedBattle.turnNumber = packet.getTurnNumber();
                    spectatedBattle.spectatorCount = packet.getSpectatorCount();
                    notifyBattleUpdated(spectatedBattle);
                }
            }
            case POKEMON_FAINTED -> {
                if (spectatedBattle != null && spectatedBattle.battleId.equals(packet.getBattleId())) {
                    addMessage(packet.getMessage());
                    notifyPokemonFainted(spectatedBattle, packet.getMessage());
                }
            }
            case BATTLE_MESSAGE -> {
                if (spectatedBattle != null && spectatedBattle.battleId.equals(packet.getBattleId())) {
                    addMessage(packet.getMessage());
                    notifyBattleMessage(spectatedBattle, packet.getMessage());
                }
            }
        }
    }

    private void addMessage(String message) {
        if (message != null) {
            battleMessages.add(message);
            while (battleMessages.size() > MAX_MESSAGES) {
                battleMessages.remove(0);
            }
        }
    }

    public boolean isSpectating() {
        return spectatedBattle != null;
    }

    @Nullable
    public SpectatedBattle getSpectatedBattle() {
        return spectatedBattle;
    }

    public List<String> getBattleMessages() {
        return new ArrayList<>(battleMessages);
    }

    public void clear() {
        spectatedBattle = null;
        battleMessages.clear();
    }

    public void addListener(SpectatorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SpectatorListener listener) {
        listeners.remove(listener);
    }

    private void notifySpectatingStarted(SpectatedBattle battle) {
        for (SpectatorListener l : listeners) l.onSpectatingStarted(battle);
    }

    private void notifySpectatingEnded(SpectatedBattle battle) {
        for (SpectatorListener l : listeners) l.onSpectatingEnded(battle);
    }

    private void notifyBattleUpdated(SpectatedBattle battle) {
        for (SpectatorListener l : listeners) l.onBattleUpdated(battle);
    }

    private void notifyPokemonFainted(SpectatedBattle battle, String message) {
        for (SpectatorListener l : listeners) l.onPokemonFainted(battle, message);
    }

    private void notifyBattleMessage(SpectatedBattle battle, String message) {
        for (SpectatorListener l : listeners) l.onBattleMessage(battle, message);
    }

    public static class SpectatedBattle {
        public final UUID battleId;
        public final String player1Name;
        public final String player2Name;
        public final String formatName;
        public int turnNumber;
        public int spectatorCount;

        SpectatedBattle(UUID battleId, String player1, String player2, String format, int turn, int spectators) {
            this.battleId = battleId;
            this.player1Name = player1;
            this.player2Name = player2;
            this.formatName = format;
            this.turnNumber = turn;
            this.spectatorCount = spectators;
        }
    }

    public interface SpectatorListener {
        default void onSpectatingStarted(SpectatedBattle battle) {}
        default void onSpectatingEnded(SpectatedBattle battle) {}
        default void onBattleUpdated(SpectatedBattle battle) {}
        default void onPokemonFainted(SpectatedBattle battle, String message) {}
        default void onBattleMessage(SpectatedBattle battle, String message) {}
    }
}
