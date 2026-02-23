package com.newbulaco.showdown.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * syncs stat stage changes to clients.
 * sent when stat boosts/drops occur during battle.
 */
public class StatChangePacket {

    private final UUID battleId;
    private final UUID pokemonId;
    private final boolean isAlly;
    // maps stat abbreviation (atk, def, spa, spd, spe, accuracy, evasion) to stage (-6 to +6)
    private final Map<String, Integer> statStages;

    public StatChangePacket(UUID battleId, UUID pokemonId, boolean isAlly, Map<String, Integer> statStages) {
        this.battleId = battleId;
        this.pokemonId = pokemonId;
        this.isAlly = isAlly;
        this.statStages = statStages;
    }

    public static void encode(StatChangePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.battleId);
        buf.writeUUID(packet.pokemonId);
        buf.writeBoolean(packet.isAlly);
        buf.writeInt(packet.statStages.size());
        for (Map.Entry<String, Integer> entry : packet.statStages.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public static StatChangePacket decode(FriendlyByteBuf buf) {
        UUID battleId = buf.readUUID();
        UUID pokemonId = buf.readUUID();
        boolean isAlly = buf.readBoolean();
        int size = buf.readInt();
        Map<String, Integer> statStages = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String stat = buf.readUtf();
            int stages = buf.readInt();
            statStages.put(stat, stages);
        }
        return new StatChangePacket(battleId, pokemonId, isAlly, statStages);
    }

    public static void handle(StatChangePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.newbulaco.showdown.client.battle.ClientStatChangeManager.getInstance().handlePacket(packet)
            );
        });
        context.setPacketHandled(true);
    }

    public UUID getBattleId() { return battleId; }
    public UUID getPokemonId() { return pokemonId; }
    public boolean isAlly() { return isAlly; }
    public Map<String, Integer> getStatStages() { return statStages; }
}
