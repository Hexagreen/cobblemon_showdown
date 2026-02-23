package com.newbulaco.showdown.client.battle;

import com.newbulaco.showdown.network.packets.PartyStatusPacket;
import com.newbulaco.showdown.network.packets.PartyStatusPacket.SlotInfo;
import com.newbulaco.showdown.network.packets.PartyStatusPacket.SlotStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientPartyStatusManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobblemonShowdown");

    private static ClientPartyStatusManager instance;

    // battle ID -> isAlly -> list of slot info
    private final Map<UUID, Map<Boolean, List<SlotInfo>>> battlePartyStatus = new ConcurrentHashMap<>();

    private UUID currentBattleId = null;

    private ClientPartyStatusManager() {}

    public static ClientPartyStatusManager getInstance() {
        if (instance == null) {
            instance = new ClientPartyStatusManager();
        }
        return instance;
    }

    public void handlePacket(PartyStatusPacket packet) {
        UUID battleId = packet.getBattleId();
        boolean isAlly = packet.isAlly();
        List<SlotInfo> slots = packet.getSlots();

        LOGGER.debug("PartyStatusPacket received: battleId={}, isAlly={}, slots={}",
                battleId, isAlly, slots.size());

        currentBattleId = battleId;

        battlePartyStatus.computeIfAbsent(battleId, k -> new ConcurrentHashMap<>());

        Map<Boolean, List<SlotInfo>> partyStatus = battlePartyStatus.get(battleId);
        partyStatus.put(isAlly, new ArrayList<>(slots));
    }

    public List<SlotInfo> getAllyPartyStatus() {
        if (currentBattleId == null) return Collections.emptyList();

        Map<Boolean, List<SlotInfo>> partyStatus = battlePartyStatus.get(currentBattleId);
        if (partyStatus == null) return Collections.emptyList();

        List<SlotInfo> allyStatus = partyStatus.get(true);
        return allyStatus != null ? new ArrayList<>(allyStatus) : Collections.emptyList();
    }

    public List<SlotInfo> getOpponentPartyStatus() {
        if (currentBattleId == null) return Collections.emptyList();

        Map<Boolean, List<SlotInfo>> partyStatus = battlePartyStatus.get(currentBattleId);
        if (partyStatus == null) return Collections.emptyList();

        List<SlotInfo> opponentStatus = partyStatus.get(false);
        return opponentStatus != null ? new ArrayList<>(opponentStatus) : Collections.emptyList();
    }

    public PartyStatusCounts getAllyStatusCounts() {
        return getStatusCounts(getAllyPartyStatus());
    }

    public PartyStatusCounts getOpponentStatusCounts() {
        return getStatusCounts(getOpponentPartyStatus());
    }

    private PartyStatusCounts getStatusCounts(List<SlotInfo> slots) {
        int total = 0;
        int healthy = 0;
        int damaged = 0;
        int fainted = 0;

        for (SlotInfo slot : slots) {
            if (slot.getStatus() != SlotStatus.EMPTY) {
                total++;
                switch (slot.getStatus()) {
                    case HEALTHY -> healthy++;
                    case DAMAGED -> damaged++;
                    case FAINTED -> fainted++;
                }
            }
        }

        return new PartyStatusCounts(total, healthy, damaged, fainted);
    }

    public void clearBattle(UUID battleId) {
        battlePartyStatus.remove(battleId);
        if (battleId.equals(currentBattleId)) {
            currentBattleId = null;
        }
    }

    public void clearAll() {
        battlePartyStatus.clear();
        currentBattleId = null;
    }

    public static int getStatusColor(SlotStatus status) {
        return switch (status) {
            case HEALTHY -> 0xFF55FF55;
            case DAMAGED -> 0xFFFFFF55;
            case FAINTED -> 0xFF555555;
            case EMPTY -> 0x00000000;
        };
    }

    public record PartyStatusCounts(int total, int healthy, int damaged, int fainted) {
        public int alive() {
            return healthy + damaged;
        }
    }
}
