package io.josemmo.bukkit.plugin.addon.imgui.limits;

import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HourlyLimitService {
    private final Map<UUID, Counter> counters = new ConcurrentHashMap<UUID, Counter>();

    public LimitResult tryConsume(UUID playerId, int amount, AddonSettings settings) {
        if (!settings.isHourlyLimitEnabled()) {
            return LimitResult.allowed(-1, -1, -1);
        }

        long currentHour = System.currentTimeMillis() / 3600000L;
        int limit = settings.getHourlyLimitMaxItems();
        Counter counter = counters.get(playerId);
        if (counter == null) {
            counter = new Counter(currentHour, 0);
            counters.put(playerId, counter);
        }

        synchronized (counter) {
            if (counter.epochHour != currentHour) {
                counter.epochHour = currentHour;
                counter.claimed = 0;
            }

            if (counter.claimed + amount > limit) {
                return LimitResult.denied(limit, counter.claimed, Math.max(0, limit - counter.claimed));
            }

            counter.claimed += amount;
            return LimitResult.allowed(limit, counter.claimed, Math.max(0, limit - counter.claimed));
        }
    }

    public void rollback(UUID playerId, int amount) {
        Counter counter = counters.get(playerId);
        if (counter == null) {
            return;
        }
        synchronized (counter) {
            counter.claimed = Math.max(0, counter.claimed - amount);
        }
    }

    private static class Counter {
        long epochHour;
        int claimed;

        Counter(long epochHour, int claimed) {
            this.epochHour = epochHour;
            this.claimed = claimed;
        }
    }

    public static class LimitResult {
        private final boolean allowed;
        private final int limit;
        private final int used;
        private final int remaining;

        private LimitResult(boolean allowed, int limit, int used, int remaining) {
            this.allowed = allowed;
            this.limit = limit;
            this.used = used;
            this.remaining = remaining;
        }

        public static LimitResult allowed(int limit, int used, int remaining) {
            return new LimitResult(true, limit, used, remaining);
        }

        public static LimitResult denied(int limit, int used, int remaining) {
            return new LimitResult(false, limit, used, remaining);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public int getLimit() {
            return limit;
        }

        public int getUsed() {
            return used;
        }

        public int getRemaining() {
            return remaining;
        }
    }
}
