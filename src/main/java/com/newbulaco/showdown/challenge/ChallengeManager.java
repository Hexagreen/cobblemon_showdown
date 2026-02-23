package com.newbulaco.showdown.challenge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChallengeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChallengeManager.class);

    // keyed by challenged player UUID since each player can only have one pending challenge
    private final Map<UUID, Challenge> activeChallenges = new ConcurrentHashMap<>();

    public boolean createChallenge(UUID challenger, UUID challenged, String formatId, Challenge.ItemBet itemBet) {
        if (activeChallenges.containsKey(challenged)) {
            return false;
        }

        Challenge challenge = new Challenge(challenger, challenged, formatId, itemBet);
        activeChallenges.put(challenged, challenge);
        LOGGER.debug("Challenge created: {} -> {} (format: {})", challenger, challenged, formatId);
        return true;
    }

    public Challenge getChallenge(UUID challenged) {
        Challenge challenge = activeChallenges.get(challenged);
        if (challenge != null && challenge.isExpired()) {
            removeChallenge(challenged);
            return null;
        }
        return challenge;
    }

    public Challenge getChallengeFrom(UUID challenger, UUID challenged) {
        Challenge challenge = getChallenge(challenged);
        if (challenge != null && challenge.getChallenger().equals(challenger)) {
            return challenge;
        }
        return null;
    }

    public boolean hasChallenge(UUID challenged) {
        return getChallenge(challenged) != null;
    }

    public boolean hasChallengedPlayer(UUID challenger, UUID challenged) {
        return getChallengeFrom(challenger, challenged) != null;
    }

    public void removeChallenge(UUID challenged) {
        Challenge removed = activeChallenges.remove(challenged);
        if (removed != null) {
            LOGGER.debug("Challenge removed: {} -> {}", removed.getChallenger(), challenged);
        }
    }

    public Challenge acceptChallenge(UUID challenged) {
        Challenge challenge = getChallenge(challenged);
        if (challenge != null) {
            removeChallenge(challenged);
            LOGGER.info("Challenge accepted: {} vs {} (format: {})",
                    challenge.getChallenger(), challenged, challenge.getFormatId());
            return challenge;
        }
        return null;
    }

    public void cleanupExpired() {
        Iterator<Map.Entry<UUID, Challenge>> iterator = activeChallenges.entrySet().iterator();
        int removed = 0;

        while (iterator.hasNext()) {
            Map.Entry<UUID, Challenge> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            LOGGER.debug("Cleaned up {} expired challenge(s)", removed);
        }
    }

    public Collection<Challenge> getAllChallenges() {
        return Collections.unmodifiableCollection(activeChallenges.values());
    }

    public int getActiveChallengeCount() {
        return activeChallenges.size();
    }
}
