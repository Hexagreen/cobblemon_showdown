package com.newbulaco.showdown.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerHistory {
    private UUID uuid;
    private List<MatchRecord> matches = new ArrayList<>();
    private PlayerStats stats = new PlayerStats();

    public PlayerHistory() {
    }

    public PlayerHistory(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public List<MatchRecord> getMatches() {
        return matches;
    }

    public void setMatches(List<MatchRecord> matches) {
        this.matches = matches != null ? matches : new ArrayList<>();
    }

    public PlayerStats getStats() {
        return stats;
    }

    public void setStats(PlayerStats stats) {
        this.stats = stats != null ? stats : new PlayerStats();
    }

    public void addMatch(UUID opponent, String formatId, boolean won) {
        MatchRecord record = new MatchRecord();
        record.date = Instant.now().toString();
        record.opponent = opponent.toString();
        record.formatId = formatId;
        record.result = won ? "win" : "loss";

        matches.add(record);

        if (won) {
            stats.wins++;
        } else {
            stats.losses++;
        }
    }

    public int getTotalMatches() {
        return stats.wins + stats.losses;
    }

    public double getWinRate() {
        int total = getTotalMatches();
        if (total == 0) return 0.0;
        return (stats.wins * 100.0) / total;
    }

    public static class MatchRecord {
        private String date;
        private String opponent;
        private String formatId;
        private String result;

        public String getDate() {
            return date;
        }

        public String getOpponent() {
            return opponent;
        }

        public UUID getOpponentUuid() {
            try {
                return UUID.fromString(opponent);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public String getFormatId() {
            return formatId;
        }

        public String getResult() {
            return result;
        }

        public boolean isWin() {
            return "win".equals(result);
        }
    }

    public static class PlayerStats {
        private int wins = 0;
        private int losses = 0;

        public int getWins() {
            return wins;
        }

        public void setWins(int wins) {
            this.wins = wins;
        }

        public int getLosses() {
            return losses;
        }

        public void setLosses(int losses) {
            this.losses = losses;
        }
    }
}
