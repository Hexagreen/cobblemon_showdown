package com.newbulaco.showdown.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class BattleStatePacket {

    public enum BattleAction {
        STARTED,
        TURN_UPDATE,
        TIMER_UPDATE,
        ENDED
    }

    private final UUID battleId;
    private final BattleAction action;
    private final String player1Name;
    private final String player2Name;
    private final String formatName;

    private final int player1TotalTime;
    private final int player1TurnTime;
    private final int player2TotalTime;
    private final int player2TurnTime;

    private final int turnNumber;

    private final String winnerName;

    public static BattleStatePacket battleStarted(UUID battleId, String player1, String player2, String format) {
        return new BattleStatePacket(battleId, BattleAction.STARTED, player1, player2, format,
                0, 0, 0, 0, 0, null);
    }

    public static BattleStatePacket timerUpdate(UUID battleId, String player1, String player2,
                                                  int p1Total, int p1Turn, int p2Total, int p2Turn) {
        return new BattleStatePacket(battleId, BattleAction.TIMER_UPDATE, player1, player2, null,
                p1Total, p1Turn, p2Total, p2Turn, 0, null);
    }

    public static BattleStatePacket turnUpdate(UUID battleId, String player1, String player2, int turn) {
        return new BattleStatePacket(battleId, BattleAction.TURN_UPDATE, player1, player2, null,
                0, 0, 0, 0, turn, null);
    }

    public static BattleStatePacket battleEnded(UUID battleId, String player1, String player2, String winner) {
        return new BattleStatePacket(battleId, BattleAction.ENDED, player1, player2, null,
                0, 0, 0, 0, 0, winner);
    }

    private BattleStatePacket(UUID battleId, BattleAction action, String player1Name, String player2Name,
                               String formatName, int p1Total, int p1Turn, int p2Total, int p2Turn,
                               int turnNumber, String winnerName) {
        this.battleId = battleId;
        this.action = action;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.formatName = formatName;
        this.player1TotalTime = p1Total;
        this.player1TurnTime = p1Turn;
        this.player2TotalTime = p2Total;
        this.player2TurnTime = p2Turn;
        this.turnNumber = turnNumber;
        this.winnerName = winnerName;
    }

    public static void encode(BattleStatePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.battleId);
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.player1Name != null ? packet.player1Name : "");
        buf.writeUtf(packet.player2Name != null ? packet.player2Name : "");
        buf.writeUtf(packet.formatName != null ? packet.formatName : "");
        buf.writeInt(packet.player1TotalTime);
        buf.writeInt(packet.player1TurnTime);
        buf.writeInt(packet.player2TotalTime);
        buf.writeInt(packet.player2TurnTime);
        buf.writeInt(packet.turnNumber);
        buf.writeUtf(packet.winnerName != null ? packet.winnerName : "");
    }

    public static BattleStatePacket decode(FriendlyByteBuf buf) {
        UUID battleId = buf.readUUID();
        BattleAction action = buf.readEnum(BattleAction.class);
        String player1 = buf.readUtf();
        String player2 = buf.readUtf();
        String format = buf.readUtf();
        int p1Total = buf.readInt();
        int p1Turn = buf.readInt();
        int p2Total = buf.readInt();
        int p2Turn = buf.readInt();
        int turn = buf.readInt();
        String winner = buf.readUtf();

        return new BattleStatePacket(battleId, action,
                player1.isEmpty() ? null : player1,
                player2.isEmpty() ? null : player2,
                format.isEmpty() ? null : format,
                p1Total, p1Turn, p2Total, p2Turn, turn,
                winner.isEmpty() ? null : winner);
    }

    public static void handle(BattleStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.newbulaco.showdown.client.ClientBattleStateManager.getInstance().handleBattleState(packet)
            );
        });
        context.setPacketHandled(true);
    }

    public UUID getBattleId() { return battleId; }
    public BattleAction getAction() { return action; }
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public String getFormatName() { return formatName; }
    public int getPlayer1TotalTime() { return player1TotalTime; }
    public int getPlayer1TurnTime() { return player1TurnTime; }
    public int getPlayer2TotalTime() { return player2TotalTime; }
    public int getPlayer2TurnTime() { return player2TurnTime; }
    public int getTurnNumber() { return turnNumber; }
    public String getWinnerName() { return winnerName; }
}
