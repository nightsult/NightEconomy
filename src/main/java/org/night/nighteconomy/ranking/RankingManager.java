package org.night.nighteconomy.ranking;

import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.RankingEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RankingManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final MultiCurrencyDatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Map<String, List<RankingEntry>> rankingCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ThreadLocal<DecimalFormat>> formatterCache = new ConcurrentHashMap<>();

    public RankingManager(MultiCurrencyDatabaseManager databaseManager, ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    private final java.util.concurrent.ExecutorService updateExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "RankingUpdate");
                t.setDaemon(true);
                return t;
            });

    public List<RankingEntry> getTopPlayers(String currencyId, int limit) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || !config.isRanking()) {
            return new ArrayList<>();
        }

        if (shouldUpdateCache(currencyId, config)) {
            triggerAsyncUpdate(currencyId);
        }

        List<RankingEntry> cached = rankingCache.get(currencyId);
        if (cached == null) {
            return new ArrayList<>();
        }

        int endIndex = Math.min(limit, cached.size());
        return new ArrayList<>(cached.subList(0, endIndex));
    }

    public int getPlayerPosition(UUID playerUuid, String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || !config.isRanking()) {
            return -1;
        }

        if (shouldUpdateCache(currencyId, config)) {
            triggerAsyncUpdate(currencyId);
        }

        List<RankingEntry> cached = rankingCache.get(currencyId);
        if (cached == null) {
            return -1;
        }

        String uuidString = playerUuid.toString();
        for (RankingEntry entry : cached) {
            if (uuidString.equals(entry.uuid)) {
                return entry.position;
            }
        }
        return -1;
    }

    private void triggerAsyncUpdate(String currencyId) {
        updateExecutor.submit(() -> {
            try {
                updateRankingCache(currencyId);
            } catch (Exception e) {
                LOGGER.error("Error updating ranking in background for " + currencyId, e);
            }
        });
    }

    public RankingEntry getTopPlayer(String currencyId) {
        List<RankingEntry> topPlayers = getTopPlayers(currencyId, 1);
        return topPlayers.isEmpty() ? null : topPlayers.get(0);
    }

    public boolean isPlayerTycoon(UUID playerUuid, String currencyId) {
        RankingEntry topPlayer = getTopPlayer(currencyId);
        return topPlayer != null && playerUuid.toString().equals(topPlayer.uuid);
    }

    @Deprecated
    public boolean isPlayerMagnata(UUID playerUuid, String currencyId) {
        return isPlayerTycoon(playerUuid, currencyId);
    }

    public String getTycoonTag(String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        return config != null ? (config.getTycoon() != null ? config.getTycoon() : "") : "";
    }

    @Deprecated
    public String getMagnataTag(String currencyId) {
        return getTycoonTag(currencyId);
    }

    public List<String> formatRankingDisplay(String currencyId, int limit) {
        List<RankingEntry> ranking = getTopPlayers(currencyId, limit);
        List<String> display = new ArrayList<>();

        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null) {
            return display;
        }

        String currencyName = config.getName();
        display.add("&6=== Top " + currencyName + " ===");

        if (ranking.isEmpty()) {
            display.add("&cNo players found in the ranking!");
            return display;
        }

        for (RankingEntry entry : ranking) {
            String formattedAmount = formatAmount(currencyId, entry.balance);
            String line = String.format("&e%d. &f%s &7- &a%s",
                    entry.position,
                    entry.username,
                    formattedAmount);
            display.add(line);
        }

        return display;
    }

    private boolean shouldUpdateCache(String currencyId, CurrencyConfig config) {
        long lastUpdate = lastUpdateTime.getOrDefault(currencyId, 0L);
        long updateInterval = config.getUpdate() * 1000L;
        long currentTime = System.currentTimeMillis();

        return (currentTime - lastUpdate) >= updateInterval;
    }

    private void updateRankingCache(String currencyId) {
        try {
            databaseManager.updateRankingCache(currencyId);

            List<RankingEntry> ranking = databaseManager.getTopPlayers(currencyId, 100);
            rankingCache.put(currencyId, ranking);
            lastUpdateTime.put(currencyId, System.currentTimeMillis());

            LOGGER.debug("Ranking cache updated for currency: " + currencyId + " (" + ranking.size() + " entrysies)");

        } catch (Exception e) {
            LOGGER.error("Error updating ranking cache for currency " + currencyId + ": ", e);
        }
    }

    public void forceUpdate(String currencyId) {
        updateRankingCache(currencyId);
    }

    public void forceUpdateAll() {
        Map<String, CurrencyConfig> currencies = configManager.getCurrencies();
        for (String currencyId : currencies.keySet()) {
            CurrencyConfig config = currencies.get(currencyId);
            if (config.isRanking()) {
                updateRankingCache(currencyId);
            }
        }
    }

    public void clearCache() {
        rankingCache.clear();
        lastUpdateTime.clear();
        LOGGER.info("Clear ranking cache.");
    }

    public void clearCache(String currencyId) {
        rankingCache.remove(currencyId);
        lastUpdateTime.remove(currencyId);
        LOGGER.debug("Cleared ranking cache for currency: " + currencyId);
    }

    private static String formatterKey(String currencyId, boolean showCents, char decimalSep) {
        return currencyId + "|" + (showCents ? "2" : "0") + "|" + decimalSep;
    }

    private DecimalFormat getOrCreateFormatter(String currencyId, boolean showCents, char decimalSep) {
        String key = formatterKey(currencyId, showCents, decimalSep);
        ThreadLocal<DecimalFormat> tl = formatterCache.computeIfAbsent(key, k ->
                ThreadLocal.withInitial(() -> {
                    DecimalFormatSymbols s = new DecimalFormatSymbols();
                    s.setDecimalSeparator(decimalSep);
                    DecimalFormat df = new DecimalFormat(showCents ? "0.00" : "0", s);
                    df.setGroupingUsed(false);
                    return df;
                })
        );
        return tl.get();
    }

    public void invalidateFormatterCacheForCurrency(String currencyId) {
        String prefix = currencyId + "|";
        formatterCache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clearFormatterCache() {
        formatterCache.clear();
    }

    private String formatAmount(String currencyId, double amount) {
        CurrencyConfig cfg = configManager.getCurrency(currencyId);

        boolean showCents = true;
        char sepChar = ',';
        CurrencyConfig.MultiplesConfig multiples = null;

        if (cfg != null && cfg.getFormat() != null) {
            var fmt = cfg.getFormat();

            showCents = (fmt.getCentsEnabled() != null) ? fmt.getCentsEnabled() : true;

            String sep = fmt.getDecimalSeparator();
            if (sep == null || sep.isEmpty()) {
                if (fmt.getSeparator() != null
                        && fmt.getSeparator().getDecimal() != null
                        && !fmt.getSeparator().getDecimal().isEmpty()) {
                    sep = fmt.getSeparator().getDecimal();
                } else {
                    sep = ",";
                }
            }
            sepChar = sep.charAt(0);

            multiples = fmt.getMultiples();
        }

        DecimalFormat df = getOrCreateFormatter(currencyId, showCents, sepChar);

        if (multiples != null && multiples.isEnabled() && amount >= Math.pow(1000, multiples.getStart())) {
            return formatWithMultiples(amount, multiples, df);
        }

        return df.format(amount);
    }

    private String formatWithMultiples(double amount, CurrencyConfig.MultiplesConfig multiples, DecimalFormat df) {
        List<String> multiplesList = multiples.getMultiples();
        if (multiplesList == null || multiplesList.isEmpty()) {
            return df.format(amount);
        }

        int index = 0;
        double value = amount;

        while (value >= 1000 && index < multiplesList.size()) {
            value /= 1000;
            index++;
        }

        if (index > 0 && index <= multiplesList.size()) {
            String suffix = multiplesList.get(index - 1);
            return df.format(value) + suffix;
        }

        return df.format(amount);
    }

    private String formatAmountSimple(String currencyId, double amount) {
        var cfg = configManager.getCurrency(currencyId);

        boolean show = true;
        char sepChar = ',';

        if (cfg != null && cfg.getFormat() != null) {
            var fmt = cfg.getFormat();

            show = (fmt.getCentsEnabled() != null) ? fmt.getCentsEnabled() : true;

            String sep = fmt.getDecimalSeparator();

            if (sep == null || sep.isEmpty()) {
                if (fmt.getSeparator() != null
                        && fmt.getSeparator().getDecimal() != null
                        && !fmt.getSeparator().getDecimal().isEmpty()) {
                    sep = fmt.getSeparator().getDecimal();
                } else {
                    sep = ",";
                }
            }

            sepChar = sep.charAt(0);
        }

        DecimalFormat df = getOrCreateFormatter(currencyId, show, sepChar);
        return df.format(amount);
    }

    public Map<String, Object> getRankingStats(String currencyId) {
        Map<String, Object> stats = new HashMap<>();

        List<RankingEntry> ranking = rankingCache.get(currencyId);
        if (ranking == null || ranking.isEmpty()) {
            stats.put("totalPlayers", 0);
            stats.put("totalBalance", 0.0);
            stats.put("averageBalance", 0.0);
            return stats;
        }

        int totalPlayers = ranking.size();
        double totalBalance = ranking.stream().mapToDouble(e -> e.balance).sum();
        double averageBalance = totalBalance / totalPlayers;

        stats.put("totalPlayers", totalPlayers);
        stats.put("totalBalance", totalBalance);
        stats.put("averageBalance", averageBalance);
        stats.put("topPlayer", ranking.get(0).username);
        stats.put("topBalance", ranking.get(0).balance);

        return stats;
    }

    public boolean isRankingEnabled(String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        return config != null && config.isRanking();
    }

    public long getLastUpdateTime(String currencyId) {
        return lastUpdateTime.getOrDefault(currencyId, 0L);
    }

    public int getCacheSize(String currencyId) {
        List<RankingEntry> cached = rankingCache.get(currencyId);
        return cached != null ? cached.size() : 0;
    }
}