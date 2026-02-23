package com.newbulaco.showdown.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// tasks are executed on the server thread during tick events
public class TickScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickScheduler.class);

    private static final List<ScheduledTask> tasks = new CopyOnWriteArrayList<>();
    private static long currentTick = 0;

    public static class ScheduledTask {
        private final Runnable task;
        private final long executeAtTick;
        private final String description;
        private boolean cancelled = false;

        public ScheduledTask(Runnable task, long executeAtTick, String description) {
            this.task = task;
            this.executeAtTick = executeAtTick;
            this.description = description;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public String getDescription() {
            return description;
        }
    }

    // delayTicks: 20 ticks = 1 second
    public static ScheduledTask schedule(Runnable task, int delayTicks, String description) {
        long executeAt = currentTick + delayTicks;
        ScheduledTask scheduledTask = new ScheduledTask(task, executeAt, description);
        tasks.add(scheduledTask);

        LOGGER.debug("Scheduled task '{}' to run in {} ticks (at tick {})",
                description, delayTicks, executeAt);

        return scheduledTask;
    }

    public static ScheduledTask scheduleSeconds(Runnable task, int delaySeconds, String description) {
        return schedule(task, delaySeconds * 20, description);
    }

    public static void tick() {
        currentTick++;

        Iterator<ScheduledTask> iterator = tasks.iterator();
        List<ScheduledTask> toRemove = new ArrayList<>();

        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();

            if (task.isCancelled()) {
                toRemove.add(task);
                continue;
            }

            if (currentTick >= task.executeAtTick) {
                try {
                    LOGGER.debug("Executing scheduled task: {}", task.getDescription());
                    task.task.run();
                } catch (Exception e) {
                    LOGGER.error("Error executing scheduled task: {}", task.getDescription(), e);
                }
                toRemove.add(task);
            }
        }

        tasks.removeAll(toRemove);
    }

    public static void cancelAll() {
        int count = tasks.size();
        tasks.clear();
        currentTick = 0;

        if (count > 0) {
            LOGGER.info("Cancelled {} scheduled tasks", count);
        }
    }

    // matches if description contains the pattern
    public static int cancel(String descriptionPattern) {
        int cancelled = 0;
        for (ScheduledTask task : tasks) {
            if (task.getDescription() != null && task.getDescription().contains(descriptionPattern)) {
                task.cancel();
                cancelled++;
            }
        }
        if (cancelled > 0) {
            LOGGER.debug("Cancelled {} tasks matching '{}'", cancelled, descriptionPattern);
        }
        return cancelled;
    }

    public static int getPendingTaskCount() {
        return tasks.size();
    }
}
