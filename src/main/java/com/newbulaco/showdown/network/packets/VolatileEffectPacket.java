package com.newbulaco.showdown.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * syncs volatile battle effects (leech seed, confusion, etc.) to clients.
 * sent when volatile effects are added or removed during battle.
 */
public class VolatileEffectPacket {

    public enum Action {
        ADD,
        REMOVE,
        CLEAR_ALL
    }

    private final UUID battleId;
    private final UUID pokemonId;
    private final boolean isAlly;
    private final Action action;
    private final String effectId;

    public VolatileEffectPacket(UUID battleId, UUID pokemonId, boolean isAlly, Action action, String effectId) {
        this.battleId = battleId;
        this.pokemonId = pokemonId;
        this.isAlly = isAlly;
        this.action = action;
        this.effectId = effectId;
    }

    public static VolatileEffectPacket add(UUID battleId, UUID pokemonId, boolean isAlly, String effectId) {
        return new VolatileEffectPacket(battleId, pokemonId, isAlly, Action.ADD, effectId);
    }

    public static VolatileEffectPacket remove(UUID battleId, UUID pokemonId, boolean isAlly, String effectId) {
        return new VolatileEffectPacket(battleId, pokemonId, isAlly, Action.REMOVE, effectId);
    }

    public static VolatileEffectPacket clearAll(UUID battleId, UUID pokemonId, boolean isAlly) {
        return new VolatileEffectPacket(battleId, pokemonId, isAlly, Action.CLEAR_ALL, "");
    }

    public static void encode(VolatileEffectPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.battleId);
        buf.writeUUID(packet.pokemonId);
        buf.writeBoolean(packet.isAlly);
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.effectId != null ? packet.effectId : "");
    }

    public static VolatileEffectPacket decode(FriendlyByteBuf buf) {
        UUID battleId = buf.readUUID();
        UUID pokemonId = buf.readUUID();
        boolean isAlly = buf.readBoolean();
        Action action = buf.readEnum(Action.class);
        String effectId = buf.readUtf();

        return new VolatileEffectPacket(battleId, pokemonId, isAlly, action,
                effectId.isEmpty() ? null : effectId);
    }

    public static void handle(VolatileEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.newbulaco.showdown.client.battle.ClientVolatileEffectManager.getInstance().handlePacket(packet)
            );
        });
        context.setPacketHandled(true);
    }

    public UUID getBattleId() { return battleId; }
    public UUID getPokemonId() { return pokemonId; }
    public boolean isAlly() { return isAlly; }
    public Action getAction() { return action; }
    public String getEffectId() { return effectId; }
}
