package com.newbulaco.showdown.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * syncs party/team status to clients during battle.
 * shows how many pokemon each side has and their health status.
 */
public class PartyStatusPacket {

    public enum SlotStatus {
        EMPTY,
        HEALTHY,
        DAMAGED,
        FAINTED
    }

    public static class SlotInfo {
        private final SlotStatus status;
        private final int healthPercent;

        public SlotInfo(SlotStatus status, int healthPercent) {
            this.status = status;
            this.healthPercent = healthPercent;
        }

        public SlotStatus getStatus() { return status; }
        public int getHealthPercent() { return healthPercent; }

        public static SlotInfo fromHealth(int currentHp, int maxHp) {
            if (maxHp <= 0) {
                return new SlotInfo(SlotStatus.EMPTY, 0);
            }

            int percent = (int) ((currentHp * 100.0) / maxHp);

            if (currentHp <= 0) {
                return new SlotInfo(SlotStatus.FAINTED, 0);
            } else if (percent > 50) {
                return new SlotInfo(SlotStatus.HEALTHY, percent);
            } else {
                return new SlotInfo(SlotStatus.DAMAGED, percent);
            }
        }

        public static SlotInfo empty() {
            return new SlotInfo(SlotStatus.EMPTY, 0);
        }
    }

    private final UUID battleId;
    private final boolean isAlly;
    private final List<SlotInfo> slots;

    public PartyStatusPacket(UUID battleId, boolean isAlly, List<SlotInfo> slots) {
        this.battleId = battleId;
        this.isAlly = isAlly;
        this.slots = new ArrayList<>(slots);
    }

    public static void encode(PartyStatusPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.battleId);
        buf.writeBoolean(packet.isAlly);
        buf.writeInt(packet.slots.size());

        for (SlotInfo slot : packet.slots) {
            buf.writeEnum(slot.status);
            buf.writeInt(slot.healthPercent);
        }
    }

    public static PartyStatusPacket decode(FriendlyByteBuf buf) {
        UUID battleId = buf.readUUID();
        boolean isAlly = buf.readBoolean();
        int slotCount = buf.readInt();

        List<SlotInfo> slots = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) {
            SlotStatus status = buf.readEnum(SlotStatus.class);
            int healthPercent = buf.readInt();
            slots.add(new SlotInfo(status, healthPercent));
        }

        return new PartyStatusPacket(battleId, isAlly, slots);
    }

    public static void handle(PartyStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.newbulaco.showdown.client.battle.ClientPartyStatusManager.getInstance().handlePacket(packet)
            );
        });
        context.setPacketHandled(true);
    }

    public UUID getBattleId() { return battleId; }
    public boolean isAlly() { return isAlly; }
    public List<SlotInfo> getSlots() { return slots; }
}
