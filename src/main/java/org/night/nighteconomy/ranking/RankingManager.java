package org.night.nighteconomy.ranking;

import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.RankingEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RankingManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final MultiCurrencyDatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Map<String, List<RankingEntry>> rankingCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    
    public RankingManager(MultiCurrencyDatabaseManager databaseManager, ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }
    
    public List<RankingEntry> getTopPlayers(String currencyId, int limit) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || !config.isRanking()) {
            return new ArrayList<>();
        }
        
        // Check if cache needs update
        if (shouldUpdateCache(currencyId, config)) {
            updateRankingCache(currencyId);
        }
        
        List<RankingEntry> cached = rankingCache.get(currencyId);
        if (cached == null) {
            return new ArrayList<>();
        }
        
        // Return requested number of entries
        int endIndex = Math.min(limit, cached.size());
        return new ArrayList<>(cached.subList(0, endIndex));
    }
    
    public int getPlayerPosition(UUID playerUuid, String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || !config.isRanking()) {
            return -1;
        }
        
        // Check if cache needs update
        if (shouldUpdateCache(currencyId, config)) {
            updateRankingCache(currencyId);
        }
        
        List<RankingEntry> cached = rankingCache.get(currencyId);
        if (cached == null) {
            return -1;
        }
        
        String uuidString = playerUuid.toString();
        for (RankingEntry entry : cached) {
            if (uuidString.equals(entry.getUuid())) {
                return entry.getPosition();
            }
        }
        
        return -1; // Player not in ranking
    }
    
    public RankingEntry getTopPlayer(String currencyId) {
        List<RankingEntry> topPlayers = getTopPlayers(currencyId, 1);
        return topPlayers.isEmpty() ? null : topPlayers.get(0);
    }
    
    public boolean isPlayerMagnata(UUID playerUuid, String currencyId) {
        RankingEntry topPlayer = getTopPlayer(currencyId);
        return topPlayer != null && playerUuid.toString().equals(topPlayer.getUuid());
    }
    
    public String getMagnataTag(String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        return config != null ? config.getMagnata() : "";
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
            display.add("&cNenhum jogador encontrado no ranking!");
            return display;
        }
        
        for (RankingEntry entry : ranking) {
            String formattedAmount = formatAmount(currencyId, entry.getBalance());
            String line = String.format("&e%d. &f%s &7- &a%s", 
                entry.getPosition(), 
                entry.getUsername(), 
                formattedAmount);
            display.add(line);
        }
        
        return display;
    }
    
    private boolean shouldUpdateCache(String currencyId, CurrencyConfig config) {
        long lastUpdate = lastUpdateTime.getOrDefault(currencyId, 0L);
        long updateInterval = config.getUpdate() * 1000L; // Convert to milliseconds
        long currentTime = System.currentTimeMillis();
        
        return (currentTime - lastUpdate) >= updateInterval;
    }
    
    private void updateRankingCache(String currencyId) {
        try {
            // Update database cache first
            databaseManager.updateRankingCache(currencyId);
            
            // Load into memory cache
            List<RankingEntry> ranking = databaseManager.getTopPlayers(currencyId, 100);
            rankingCache.put(currencyId, ranking);
            lastUpdateTime.put(currencyId, System.currentTimeMillis());
            
            LOGGER.debug("Cache de ranking atualizado para moeda: " + currencyId + " (" + ranking.size() + " entradas)");
            
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar cache de ranking para moeda " + currencyId + ": ", e);
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
        LOGGER.info("Cache de ranking limpo.");
    }
    
    public void clearCache(String currencyId) {
        rankingCache.remove(currencyId);
        lastUpdateTime.remove(currencyId);
        LOGGER.debug("Cache de ranking limpo para moeda: " + currencyId);
    }
    
    private String formatAmount(String currencyId, double amount) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || config.getFormat() == null) {
            return String.format("%.2f", amount);
        }
        
        CurrencyConfig.FormatConfig format = config.getFormat();
        CurrencyConfig.MultiplesConfig multiples = format.getMultiples();
        
        // Handle multiples (K, M, B, etc.)
        if (multiples != null && multiples.isEnabled() && amount >= Math.pow(1000, multiples.getStart())) {
            return formatWithMultiples(amount, multiples);
        }
        
        // Regular formatting
        return String.format("%.2f", amount);
    }
    
    private String formatWithMultiples(double amount, CurrencyConfig.MultiplesConfig multiples) {
        List<String> multiplesList = multiples.getMultiples();
        if (multiplesList == null || multiplesList.isEmpty()) {
            return String.format("%.2f", amount);
        }
        
        int index = 0;
        double value = amount;
        
        while (value >= 1000 && index < multiplesList.size()) {
            value /= 1000;
            index++;
        }
        
        if (index > 0 && index <= multiplesList.size()) {
            String suffix = multiplesList.get(index - 1);
            return String.format("%.2f%s", value, suffix);
        }
        
        return String.format("%.2f", amount);
    }
    
    // Statistics methods
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
        double totalBalance = ranking.stream().mapToDouble(RankingEntry::getBalance).sum();
        double averageBalance = totalBalance / totalPlayers;
        
        stats.put("totalPlayers", totalPlayers);
        stats.put("totalBalance", totalBalance);
        stats.put("averageBalance", averageBalance);
        stats.put("topPlayer", ranking.get(0).getUsername());
        stats.put("topBalance", ranking.get(0).getBalance());
        
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

