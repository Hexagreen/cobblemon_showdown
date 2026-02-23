package com.newbulaco.showdown.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * syncs field status (weather, terrain, rooms) to clients.
 * sent when field conditions change during battle.
 */
public class FieldStatusPacket {

    public enum FieldType {
        WEATHER,
        TERRAIN,
        ROOM
    }

    public enum Action {
        SET,
        CLEAR
    }

    private final UUID battleId;
    private final FieldType fieldType;
    private final Action action;
    private final String conditionId;
    private final int turns;

    public FieldStatusPacket(UUID battleId, FieldType fieldType, Action action, String conditionId, int turns) {
        this.battleId = battleId;
        this.fieldType = fieldType;
        this.action = action;
        this.conditionId = conditionId;
        this.turns = turns;
    }

    public static FieldStatusPacket set(UUID battleId, FieldType fieldType, String conditionId, int turns) {
        return new FieldStatusPacket(battleId, fieldType, Action.SET, conditionId, turns);
    }

    public static FieldStatusPacket clear(UUID battleId, FieldType fieldType, String conditionId) {
        return new FieldStatusPacket(battleId, fieldType, Action.CLEAR, conditionId, -1);
    }

    public static void encode(FieldStatusPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.battleId);
        buf.writeEnum(packet.fieldType);
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.conditionId != null ? packet.conditionId : "");
        buf.writeInt(packet.turns);
    }

    public static FieldStatusPacket decode(FriendlyByteBuf buf) {
        UUID battleId = buf.readUUID();
        FieldType fieldType = buf.readEnum(FieldType.class);
        Action action = buf.readEnum(Action.class);
        String conditionId = buf.readUtf();
        int turns = buf.readInt();

        return new FieldStatusPacket(battleId, fieldType, action,
                conditionId.isEmpty() ? null : conditionId, turns);
    }

    public static void handle(FieldStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> handleOnClient(packet)
            );
        });
        context.setPacketHandled(true);
    }

    // separate method to avoid client class loading on dedicated server
    private static void handleOnClient(FieldStatusPacket packet) {
        if (packet.action == Action.CLEAR) {
            switch (packet.fieldType) {
                case WEATHER -> com.newbulaco.showdown.client.battle.BattleStatusTracker.setWeather("");
                case TERRAIN -> com.newbulaco.showdown.client.battle.BattleStatusTracker.setTerrain("");
                case ROOM -> com.newbulaco.showdown.client.battle.BattleStatusTracker.removeRoom(packet.conditionId);
            }
        } else {
            switch (packet.fieldType) {
                case WEATHER -> com.newbulaco.showdown.client.battle.BattleStatusTracker.setWeather(packet.conditionId, packet.turns);
                case TERRAIN -> com.newbulaco.showdown.client.battle.BattleStatusTracker.setTerrain(packet.conditionId, packet.turns);
                case ROOM -> com.newbulaco.showdown.client.battle.BattleStatusTracker.setRoom(packet.conditionId, packet.turns);
            }
        }
    }

    public UUID getBattleId() { return battleId; }
    public FieldType getFieldType() { return fieldType; }
    public Action getAction() { return action; }
    public String getConditionId() { return conditionId; }
    public int getTurns() { return turns; }
}
