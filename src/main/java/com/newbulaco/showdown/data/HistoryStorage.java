package com.newbulaco.showdown.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HistoryStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path historyDir;
    private final Map<UUID, PlayerHistory> cache = new ConcurrentHashMap<>();

    public HistoryStorage() {
        this.historyDir = FMLPaths.CONFIGDIR.get()
                .resolve("cobblemon_showdown")
                .resolve("history");

        try {
            Files.createDirectories(historyDir);
            LOGGER.info("History directory: {}", historyDir.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create history directory", e);
        }
    }

    public PlayerHistory getHistory(UUID playerUuid) {
        PlayerHistory cached = cache.get(playerUuid);
        if (cached != null) {
            return cached;
        }

        File file = getHistoryFile(playerUuid);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                PlayerHistory history = GSON.fromJson(reader, PlayerHistory.class);
                if (history != null) {
                    history.setUuid(playerUuid);
                    cache.put(playerUuid, history);
                    LOGGER.debug("Loaded history for player {}", playerUuid);
                    return history;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load history for player {}", playerUuid, e);
            }
        }

        PlayerHistory newHistory = new PlayerHistory(playerUuid);
        cache.put(playerUuid, newHistory);
        return newHistory;
    }

    public void saveHistory(UUID playerUuid) {
        PlayerHistory history = cache.get(playerUuid);
        if (history == null) {
            LOGGER.warn("Attempted to save history for uncached player {}", playerUuid);
            return;
        }

        File file = getHistoryFile(playerUuid);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(history, writer);
            LOGGER.debug("Saved history for player {}", playerUuid);
        } catch (IOException e) {
            LOGGER.error("Failed to save history for player {}", playerUuid, e);
        }
    }

    public void saveAll() {
        int saved = 0;
        for (UUID uuid : cache.keySet()) {
            saveHistory(uuid);
            saved++;
        }
        if (saved > 0) {
            LOGGER.info("Saved {} player historie(s)", saved);
        }
    }

    // saves before unloading to prevent data loss
    public void unloadHistory(UUID playerUuid) {
        if (cache.containsKey(playerUuid)) {
            saveHistory(playerUuid);
            cache.remove(playerUuid);
            LOGGER.debug("Unloaded history for player {}", playerUuid);
        }
    }

    private File getHistoryFile(UUID playerUuid) {
        return historyDir.resolve(playerUuid.toString() + ".json").toFile();
    }

    public int getCachedCount() {
        return cache.size();
    }

    public boolean hasHistory(UUID playerUuid) {
        if (cache.containsKey(playerUuid)) {
            return true;
        }
        return getHistoryFile(playerUuid).exists();
    }
}
