package com.newbulaco.showdown.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * syncs side conditions (safeguard, light screen, reflect, etc.) to clients.
 * side conditions affect an entire side of the battle, not individual pokemon.
 */
public class SideConditionPacket {

    public enum Action {
        ADD,
        REMOVE,
        CLEAR_ALL
    }

    private final UUID battleId;
    private final boolean isAllySide;
    private final Action action;
    private final String conditionId;
    private final int turnsRemaining;

    public SideConditionPacket(UUID battleId, boolean isAllySide, Action action, String conditionId, int turnsRemaining) {
        this.battleId = battleId;
        this.isAllySide = isAllySide;
        this.action = action;
        this.conditionId = conditionId;
        this.turnsRemaining = turnsRemaining;
    }

    public static SideConditionPacket add(UUID battleId, boolean isAllySide, String conditionId, int turnsRemaining) {
        return new SideConditionPacket(battleId, isAllySide, Action.ADD, conditionId, turnsRemaining);
    }

    public static SideConditionPacket remove(UUID battleId, boolean isAllySide, String conditionId) {
        return new SideConditionPacket(battleId, isAllySide, Action.REMOVE, conditionId, 0);
    }

    public static SideConditionPacket clearAll(UUID battleId, boolean isAllySide) {
        return new SideConditionPacket(battleId, isAllySide, Action.CLEAR_ALL, "", 0);
    }

    public static void encode(SideConditionPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.battleId);
        buf.writeBoolean(packet.isAllySide);
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.conditionId != null ? packet.conditionId : "");
        buf.writeInt(packet.turnsRemaining);
    }

    public static SideConditionPacket decode(FriendlyByteBuf buf) {
        UUID battleId = buf.readUUID();
        boolean isAllySide = buf.readBoolean();
        Action action = buf.readEnum(Action.class);
        String conditionId = buf.readUtf();
        int turnsRemaining = buf.readInt();

        return new SideConditionPacket(battleId, isAllySide, action,
                conditionId.isEmpty() ? null : conditionId, turnsRemaining);
    }

    public static void handle(SideConditionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.newbulaco.showdown.client.battle.ClientSideConditionManager.getInstance().handlePacket(packet)
            );
        });
        context.setPacketHandled(true);
    }

    public UUID getBattleId() { return battleId; }
    public boolean isAllySide() { return isAllySide; }
    public Action getAction() { return action; }
    public String getConditionId() { return conditionId; }
    public int getTurnsRemaining() { return turnsRemaining; }
}
