package com.newbulaco.showdown.client;

import com.newbulaco.showdown.network.packets.BattleStatePacket;
import com.newbulaco.showdown.network.packets.SeriesStatePacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@OnlyIn(Dist.CLIENT)
public class ClientBattleStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientBattleStateManager.class);

    private static ClientBattleStateManager instance;

    @Nullable
    private ActiveBattle currentBattle;

    @Nullable
    private ActiveSeries currentSeries;

    private final List<BattleStateListener> battleListeners = new CopyOnWriteArrayList<>();
    private final List<SeriesStateListener> seriesListeners = new CopyOnWriteArrayList<>();

    private ClientBattleStateManager() {}

    public static ClientBattleStateManager getInstance() {
        if (instance == null) {
            instance = new ClientBattleStateManager();
        }
        return instance;
    }

    public void handleBattleState(BattleStatePacket packet) {
        switch (packet.getAction()) {
            case STARTED -> {
                currentBattle = new ActiveBattle(
                        packet.getBattleId(),
                        packet.getPlayer1Name(),
                        packet.getPlayer2Name(),
                        packet.getFormatName()
                );
                LOGGER.debug("Battle started: {} vs {} ({})",
                        packet.getPlayer1Name(), packet.getPlayer2Name(), packet.getFormatName());
                notifyBattleStarted(currentBattle);
            }
            case TIMER_UPDATE -> {
                if (currentBattle != null && currentBattle.battleId.equals(packet.getBattleId())) {
                    currentBattle.player1TotalTime = packet.getPlayer1TotalTime();
                    currentBattle.player1TurnTime = packet.getPlayer1TurnTime();
                    currentBattle.player2TotalTime = packet.getPlayer2TotalTime();
                    currentBattle.player2TurnTime = packet.getPlayer2TurnTime();
                    notifyTimerUpdated(currentBattle);
                }
            }
            case TURN_UPDATE -> {
                if (currentBattle != null && currentBattle.battleId.equals(packet.getBattleId())) {
                    currentBattle.turnNumber = packet.getTurnNumber();
                    notifyTurnUpdated(currentBattle);
                }
            }
            case ENDED -> {
                if (currentBattle != null && currentBattle.battleId.equals(packet.getBattleId())) {
                    currentBattle.winnerName = packet.getWinnerName();
                    currentBattle.ended = true;
                    notifyBattleEnded(currentBattle);
                    LOGGER.debug("Battle ended: {} won", packet.getWinnerName());
                    currentBattle = null;
                }
            }
        }
    }

    public void handleSeriesState(SeriesStatePacket packet) {
        switch (packet.getAction()) {
            case STARTED -> {
                currentSeries = new ActiveSeries(
                        packet.getPlayer1Name(),
                        packet.getPlayer2Name(),
                        packet.getBestOf()
                );
                LOGGER.debug("Series started: {} vs {} (Best of {})",
                        packet.getPlayer1Name(), packet.getPlayer2Name(), packet.getBestOf());
                notifySeriesStarted(currentSeries);
            }
            case GAME_ENDED -> {
                if (currentSeries != null) {
                    currentSeries.player1Wins = packet.getPlayer1Wins();
                    currentSeries.player2Wins = packet.getPlayer2Wins();
                    currentSeries.currentGame = packet.getCurrentGame();
                    currentSeries.lastGameWinner = packet.getGameWinner();
                    notifyGameEnded(currentSeries);
                    LOGGER.debug("Game {} ended: {} won (Score: {}-{})",
                            packet.getCurrentGame() - 1, packet.getGameWinner(),
                            packet.getPlayer1Wins(), packet.getPlayer2Wins());
                }
            }
            case SERIES_ENDED -> {
                if (currentSeries != null) {
                    currentSeries.player1Wins = packet.getPlayer1Wins();
                    currentSeries.player2Wins = packet.getPlayer2Wins();
                    currentSeries.seriesWinner = packet.getSeriesWinner();
                    currentSeries.ended = true;
                    notifySeriesEnded(currentSeries);
                    LOGGER.debug("Series ended: {} won ({}-{})",
                            packet.getSeriesWinner(), packet.getPlayer1Wins(), packet.getPlayer2Wins());
                    currentSeries = null;
                }
            }
        }
    }

    @Nullable
    public ActiveBattle getCurrentBattle() { return currentBattle; }

    @Nullable
    public ActiveSeries getCurrentSeries() { return currentSeries; }

    public boolean isInBattle() { return currentBattle != null && !currentBattle.ended; }

    public boolean isInSeries() { return currentSeries != null && !currentSeries.ended; }

    public void addBattleListener(BattleStateListener listener) { battleListeners.add(listener); }
    public void removeBattleListener(BattleStateListener listener) { battleListeners.remove(listener); }

    public void addSeriesListener(SeriesStateListener listener) { seriesListeners.add(listener); }
    public void removeSeriesListener(SeriesStateListener listener) { seriesListeners.remove(listener); }

    private void notifyBattleStarted(ActiveBattle battle) {
        for (BattleStateListener l : battleListeners) l.onBattleStarted(battle);
    }
    private void notifyTimerUpdated(ActiveBattle battle) {
        for (BattleStateListener l : battleListeners) l.onTimerUpdated(battle);
    }
    private void notifyTurnUpdated(ActiveBattle battle) {
        for (BattleStateListener l : battleListeners) l.onTurnUpdated(battle);
    }
    private void notifyBattleEnded(ActiveBattle battle) {
        for (BattleStateListener l : battleListeners) l.onBattleEnded(battle);
    }

    private void notifySeriesStarted(ActiveSeries series) {
        for (SeriesStateListener l : seriesListeners) l.onSeriesStarted(series);
    }
    private void notifyGameEnded(ActiveSeries series) {
        for (SeriesStateListener l : seriesListeners) l.onGameEnded(series);
    }
    private void notifySeriesEnded(ActiveSeries series) {
        for (SeriesStateListener l : seriesListeners) l.onSeriesEnded(series);
    }

    public void clearAll() {
        currentBattle = null;
        currentSeries = null;
    }

    public static class ActiveBattle {
        public final UUID battleId;
        public final String player1Name;
        public final String player2Name;
        public final String formatName;
        public int player1TotalTime;
        public int player1TurnTime;
        public int player2TotalTime;
        public int player2TurnTime;
        public int turnNumber;
        public String winnerName;
        public boolean ended;

        ActiveBattle(UUID battleId, String player1Name, String player2Name, String formatName) {
            this.battleId = battleId;
            this.player1Name = player1Name;
            this.player2Name = player2Name;
            this.formatName = formatName;
        }
    }

    public static class ActiveSeries {
        public final String player1Name;
        public final String player2Name;
        public final int bestOf;
        public int player1Wins;
        public int player2Wins;
        public int currentGame = 1;
        public String lastGameWinner;
        public String seriesWinner;
        public boolean ended;

        ActiveSeries(String player1Name, String player2Name, int bestOf) {
            this.player1Name = player1Name;
            this.player2Name = player2Name;
            this.bestOf = bestOf;
        }
    }

    public interface BattleStateListener {
        default void onBattleStarted(ActiveBattle battle) {}
        default void onTimerUpdated(ActiveBattle battle) {}
        default void onTurnUpdated(ActiveBattle battle) {}
        default void onBattleEnded(ActiveBattle battle) {}
    }

    public interface SeriesStateListener {
        default void onSeriesStarted(ActiveSeries series) {}
        default void onGameEnded(ActiveSeries series) {}
        default void onSeriesEnded(ActiveSeries series) {}
    }
}
