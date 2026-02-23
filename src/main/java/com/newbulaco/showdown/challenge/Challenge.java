package com.newbulaco.showdown.challenge;

import java.util.UUID;

public class Challenge {
    private final UUID challenger;
    private final UUID challenged;
    private final String formatId;
    private final long createdTime;
    // challenges expire after 60 seconds if not accepted/denied
    private final long expiryTime;
    private final ItemBet itemBet;

    public Challenge(UUID challenger, UUID challenged, String formatId, ItemBet itemBet) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.formatId = formatId;
        this.createdTime = System.currentTimeMillis();
        this.expiryTime = createdTime + 60000;
        this.itemBet = itemBet;
    }

    public UUID getChallenger() {
        return challenger;
    }

    public UUID getChallenged() {
        return challenged;
    }

    public String getFormatId() {
        return formatId;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public long getRemainingSeconds() {
        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public ItemBet getItemBet() {
        return itemBet;
    }

    public boolean hasItemBet() {
        return itemBet != null;
    }

    public static class ItemBet {
        private final String itemId;
        private final int amount;

        public ItemBet(String itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }

        public String getItemId() {
            return itemId;
        }

        public int getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return amount + "x " + itemId;
        }
    }
}
