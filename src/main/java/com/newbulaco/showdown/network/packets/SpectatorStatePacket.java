package com.newbulaco.showdown.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SpectatorStatePacket {

    public enum SpectatorAction {
        JOINED,
        LEFT,
        BATTLE_UPDATE,
        POKEMON_FAINTED,
        BATTLE_MESSAGE
    }

    private final SpectatorAction action;
    private final UUID battleId;
    private final String player1Name;
    private final String player2Name;
    private final String formatName;
    private final int turnNumber;
    private final int spectatorCount;
    private final String message;

    public static SpectatorStatePacket joined(UUID battleId, String player1, String player2,
                                               String format, int turn, int spectators) {
        return new SpectatorStatePacket(SpectatorAction.JOINED, battleId, player1, player2,
                format, turn, spectators, null);
    }

    public static SpectatorStatePacket left(UUID battleId) {
        return new SpectatorStatePacket(SpectatorAction.LEFT, battleId, null, null,
                null, 0, 0, null);
    }

    public static SpectatorStatePacket battleUpdate(UUID battleId, String player1, String player2,
                                                     int turn, int spectators) {
        return new SpectatorStatePacket(SpectatorAction.BATTLE_UPDATE, battleId, player1, player2,
                null, turn, spectators, null);
    }

    public static SpectatorStatePacket pokemonFainted(UUID battleId, String pokemonName, String ownerName) {
        return new SpectatorStatePacket(SpectatorAction.POKEMON_FAINTED, battleId, ownerName, null,
                null, 0, 0, pokemonName + " fainted!");
    }

    public static SpectatorStatePacket battleMessage(UUID battleId, String message) {
        return new SpectatorStatePacket(SpectatorAction.BATTLE_MESSAGE, battleId, null, null,
                null, 0, 0, message);
    }

    private SpectatorStatePacket(SpectatorAction action, UUID battleId, String player1Name,
                                  String player2Name, String formatName, int turnNumber,
                                  int spectatorCount, String message) {
        this.action = action;
        this.battleId = battleId;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.formatName = formatName;
        this.turnNumber = turnNumber;
        this.spectatorCount = spectatorCount;
        this.message = message;
    }

    public static void encode(SpectatorStatePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUUID(packet.battleId);
        buf.writeUtf(packet.player1Name != null ? packet.player1Name : "");
        buf.writeUtf(packet.player2Name != null ? packet.player2Name : "");
        buf.writeUtf(packet.formatName != null ? packet.formatName : "");
        buf.writeInt(packet.turnNumber);
        buf.writeInt(packet.spectatorCount);
        buf.writeUtf(packet.message != null ? packet.message : "");
    }

    public static SpectatorStatePacket decode(FriendlyByteBuf buf) {
        SpectatorAction action = buf.readEnum(SpectatorAction.class);
        UUID battleId = buf.readUUID();
        String player1 = buf.readUtf();
        String player2 = buf.readUtf();
        String format = buf.readUtf();
        int turn = buf.readInt();
        int spectators = buf.readInt();
        String message = buf.readUtf();

        return new SpectatorStatePacket(action, battleId,
                player1.isEmpty() ? null : player1,
                player2.isEmpty() ? null : player2,
                format.isEmpty() ? null : format,
                turn, spectators,
                message.isEmpty() ? null : message);
    }

    public static void handle(SpectatorStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> handleClient(packet)
            );
        });
        context.setPacketHandled(true);
    }

    private static void handleClient(SpectatorStatePacket packet) {
        com.newbulaco.showdown.client.ClientSpectatorManager.getInstance().handleSpectatorState(packet);
    }

    public SpectatorAction getAction() { return action; }
    public UUID getBattleId() { return battleId; }
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public String getFormatName() { return formatName; }
    public int getTurnNumber() { return turnNumber; }
    public int getSpectatorCount() { return spectatorCount; }
    public String getMessage() { return message; }
}
