package com.newbulaco.showdown.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SeriesStatePacket {

    public enum SeriesAction {
        STARTED,
        GAME_ENDED,
        SERIES_ENDED
    }

    private final SeriesAction action;
    private final String player1Name;
    private final String player2Name;
    private final int player1Wins;
    private final int player2Wins;
    private final int bestOf;
    private final int currentGame;
    private final String gameWinner;
    private final String seriesWinner;

    public static SeriesStatePacket seriesStarted(String player1, String player2, int bestOf) {
        return new SeriesStatePacket(SeriesAction.STARTED, player1, player2, 0, 0, bestOf, 1, null, null);
    }

    public static SeriesStatePacket gameEnded(String player1, String player2, int p1Wins, int p2Wins,
                                               int bestOf, int currentGame, String gameWinner) {
        return new SeriesStatePacket(SeriesAction.GAME_ENDED, player1, player2, p1Wins, p2Wins,
                bestOf, currentGame, gameWinner, null);
    }

    public static SeriesStatePacket seriesEnded(String player1, String player2, int p1Wins, int p2Wins,
                                                 int bestOf, String seriesWinner) {
        return new SeriesStatePacket(SeriesAction.SERIES_ENDED, player1, player2, p1Wins, p2Wins,
                bestOf, 0, null, seriesWinner);
    }

    private SeriesStatePacket(SeriesAction action, String player1Name, String player2Name,
                               int player1Wins, int player2Wins, int bestOf, int currentGame,
                               String gameWinner, String seriesWinner) {
        this.action = action;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.player1Wins = player1Wins;
        this.player2Wins = player2Wins;
        this.bestOf = bestOf;
        this.currentGame = currentGame;
        this.gameWinner = gameWinner;
        this.seriesWinner = seriesWinner;
    }

    public static void encode(SeriesStatePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.player1Name != null ? packet.player1Name : "");
        buf.writeUtf(packet.player2Name != null ? packet.player2Name : "");
        buf.writeInt(packet.player1Wins);
        buf.writeInt(packet.player2Wins);
        buf.writeInt(packet.bestOf);
        buf.writeInt(packet.currentGame);
        buf.writeUtf(packet.gameWinner != null ? packet.gameWinner : "");
        buf.writeUtf(packet.seriesWinner != null ? packet.seriesWinner : "");
    }

    public static SeriesStatePacket decode(FriendlyByteBuf buf) {
        SeriesAction action = buf.readEnum(SeriesAction.class);
        String player1 = buf.readUtf();
        String player2 = buf.readUtf();
        int p1Wins = buf.readInt();
        int p2Wins = buf.readInt();
        int bestOf = buf.readInt();
        int currentGame = buf.readInt();
        String gameWinner = buf.readUtf();
        String seriesWinner = buf.readUtf();

        return new SeriesStatePacket(action,
                player1.isEmpty() ? null : player1,
                player2.isEmpty() ? null : player2,
                p1Wins, p2Wins, bestOf, currentGame,
                gameWinner.isEmpty() ? null : gameWinner,
                seriesWinner.isEmpty() ? null : seriesWinner);
    }

    public static void handle(SeriesStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.newbulaco.showdown.client.ClientBattleStateManager.getInstance().handleSeriesState(packet)
            );
        });
        context.setPacketHandled(true);
    }

    public SeriesAction getAction() { return action; }
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public int getPlayer1Wins() { return player1Wins; }
    public int getPlayer2Wins() { return player2Wins; }
    public int getBestOf() { return bestOf; }
    public int getCurrentGame() { return currentGame; }
    public String getGameWinner() { return gameWinner; }
    public String getSeriesWinner() { return seriesWinner; }
}
