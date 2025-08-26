package org.night.nighteconomy.service;

import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.RankingEntry;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MultiCurrencyEconomyService {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final MultiCurrencyDatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Long> lastRankingUpdate = new ConcurrentHashMap<>();
    
    public MultiCurrencyEconomyService(MultiCurrencyDatabaseManager databaseManager, ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Start ranking update scheduler
        startRankingUpdateScheduler();
    }
    
    private void startRankingUpdateScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateAllRankings();
            } catch (Exception e) {
                LOGGER.error("Erro ao atualizar rankings: ", e);
            }
        }, 30, 30, TimeUnit.SECONDS); // Check every 30 seconds
    }
    
    private void updateAllRankings() {
        Map<String, CurrencyConfig> currencies = configManager.getCurrencies();
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, CurrencyConfig> entry : currencies.entrySet()) {
            String currencyId = entry.getKey();
            CurrencyConfig config = entry.getValue();
            
            if (!config.isRanking()) {
                continue; // Skip currencies with ranking disabled
            }
            
            long lastUpdate = lastRankingUpdate.getOrDefault(currencyId, 0L);
            long updateInterval = config.getUpdate() * 1000L; // Convert to milliseconds
            
            if (currentTime - lastUpdate >= updateInterval) {
                databaseManager.updateRankingCache(currencyId);
                lastRankingUpdate.put(currencyId, currentTime);
                LOGGER.debug("Ranking atualizado para moeda: " + currencyId);
            }
        }
    }
    
    // Account management
    public boolean createAccount(UUID playerUuid, String currencyId, String username) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null) {
            LOGGER.warn("Tentativa de criar conta para moeda inexistente: " + currencyId);
            return false;
        }
        
        return databaseManager.createAccount(playerUuid, currencyId, username, config.getDefaultValue());
    }

    public void ensureAccountExists(UUID playerUuid, String currencyId, String username) {
        if (!databaseManager.hasAccount(playerUuid, currencyId)) {
            createAccount(playerUuid, currencyId, username);
        }
    }

    public java.util.concurrent.CompletableFuture<List<Transaction>> getPlayerTransactionsAsync(UUID playerUuid, String currencyId, int limit) {
        return java.util.concurrent.CompletableFuture.supplyAsync(
                () -> databaseManager.getPlayerTransactions(playerUuid, currencyId, Math.min(limit, 50)),
                scheduler
        );
    }

    public java.util.concurrent.CompletableFuture<List<RankingEntry>> getTopPlayersAsync(String currencyId, int limit) {
        // Apenas lê o cache; atualização é feita pelo scheduler interno
        return java.util.concurrent.CompletableFuture.supplyAsync(
                () -> databaseManager.getTopPlayers(currencyId, Math.min(limit, 100)),
                scheduler
        );
    }

    public double getBalance(UUID playerUuid, String currencyId) {
        return databaseManager.getBalance(playerUuid, currencyId);
    }
    
    public boolean setBalance(UUID playerUuid, String currencyId, double amount) {
        if (amount < 0) {
            return false;
        }
        
        boolean success = databaseManager.setBalance(playerUuid, currencyId, amount);
        if (success) {
            databaseManager.recordTransaction(currencyId, null, playerUuid, amount, 0.0, "SET", "Saldo definido por administrador");
        }
        return success;
    }
    
    public boolean addBalance(UUID playerUuid, String currencyId, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        boolean success = databaseManager.addBalance(playerUuid, currencyId, amount);
        if (success) {
            databaseManager.recordTransaction(currencyId, null, playerUuid, amount, 0.0, "ADD", "Dinheiro adicionado por administrador");
        }
        return success;
    }
    
    public boolean subtractBalance(UUID playerUuid, String currencyId, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        double currentBalance = getBalance(playerUuid, currencyId);
        if (currentBalance < amount) {
            return false;
        }
        
        boolean success = databaseManager.subtractBalance(playerUuid, currencyId, amount);
        if (success) {
            databaseManager.recordTransaction(currencyId, playerUuid, null, amount, 0.0, "REMOVE", "Dinheiro removido por administrador");
        }
        return success;
    }
    
    public boolean resetBalance(UUID playerUuid, String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null) {
            return false;
        }
        
        boolean success = databaseManager.resetPlayerBalance(playerUuid, currencyId, config.getDefaultValue());
        if (success) {
            databaseManager.recordTransaction(currencyId, null, playerUuid, config.getDefaultValue(), 0.0, "RESET", "Saldo resetado por administrador");
        }
        return success;
    }
    
    // Payment system
    public PaymentResult payPlayer(UUID fromUuid, UUID toUuid, String currencyId, double amount) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null) {
            return new PaymentResult(false, "Moeda não encontrada");
        }
        
        if (amount <= 0) {
            return new PaymentResult(false, "Quantia inválida");
        }
        
        // Check if sender has enough balance
        double senderBalance = getBalance(fromUuid, currencyId);
        double fee = config.getPayment() != null ? amount * config.getPayment().getFee() : 0.0;
        double totalCost = amount + fee;
        
        if (senderBalance < totalCost) {
            return new PaymentResult(false, "Saldo insuficiente");
        }
        
        // Check if receiver accepts payments
        if (!databaseManager.isPaymentEnabled(toUuid, currencyId)) {
            return new PaymentResult(false, "Jogador não aceita pagamentos");
        }
        
        // Perform the transaction
        boolean senderDeducted = databaseManager.subtractBalance(fromUuid, currencyId, totalCost);
        if (!senderDeducted) {
            return new PaymentResult(false, "Erro ao debitar do remetente");
        }
        
        boolean receiverCredited = databaseManager.addBalance(toUuid, currencyId, amount);
        if (!receiverCredited) {
            // Rollback sender deduction
            databaseManager.addBalance(fromUuid, currencyId, totalCost);
            return new PaymentResult(false, "Erro ao creditar ao destinatário");
        }
        
        // Record transactions
        databaseManager.recordTransaction(currencyId, fromUuid, toUuid, amount, fee, "PAY_SEND", "Pagamento enviado");
        databaseManager.recordTransaction(currencyId, fromUuid, toUuid, amount, 0.0, "PAY_RECEIVE", "Pagamento recebido");
        
        if (fee > 0) {
            databaseManager.recordTransaction(currencyId, fromUuid, null, fee, 0.0, "FEE", "Taxa de transação");
        }
        
        return new PaymentResult(true, "Pagamento realizado com sucesso", fee);
    }
    
    // Payment settings
    public boolean isPaymentEnabled(UUID playerUuid, String currencyId) {
        return databaseManager.isPaymentEnabled(playerUuid, currencyId);
    }
    
    public boolean setPaymentEnabled(UUID playerUuid, String currencyId, boolean enabled) {
        return databaseManager.setPaymentEnabled(playerUuid, currencyId, enabled);
    }
    
    // Transaction history
    public List<Transaction> getPlayerTransactions(UUID playerUuid, String currencyId, int limit) {
        return databaseManager.getPlayerTransactions(playerUuid, currencyId, Math.min(limit, 50)); // Max 50 transactions
    }
    
    // Ranking system
    public List<RankingEntry> getTopPlayers(String currencyId, int limit) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || !config.isRanking()) {
            return new ArrayList<>();
        }
        
        return databaseManager.getTopPlayers(currencyId, Math.min(limit, 100)); // Max 100 entries
    }
    
    public int getPlayerPosition(UUID playerUuid, String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || !config.isRanking()) {
            return -1;
        }
        
        return databaseManager.getPlayerPosition(playerUuid, currencyId);
    }
    
    public boolean isPlayerMagnata(UUID playerUuid, String currencyId) {
        String topPlayerUuid = databaseManager.getTopPlayerUuid(currencyId);
        return topPlayerUuid != null && topPlayerUuid.equals(playerUuid.toString());
    }
    
    public String getMagnataTag(String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        return config != null ? config.getMagnata() : "";
    }
    
    // Currency formatting
    public String formatAmount(String currencyId, double amount) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || config.getFormat() == null) {
            return String.format("%.2f", amount);
        }
        
        CurrencyConfig.FormatConfig format = config.getFormat();
        CurrencyConfig.SeparatorConfig separator = format.getSeparator();
        CurrencyConfig.MultiplesConfig multiples = format.getMultiples();
        
        // Handle multiples (K, M, B, etc.)
        if (multiples != null && multiples.isEnabled() && amount >= multiples.getStart()) {
            return formatWithMultiples(amount, multiples, separator);
        }
        
        // Regular formatting
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        if (separator != null) {
            symbols.setDecimalSeparator(separator.getDecimal().charAt(0));
            symbols.setGroupingSeparator(separator.getGroup().charAt(0));
        }
        
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(amount);
    }
    
    private String formatWithMultiples(double amount, CurrencyConfig.MultiplesConfig multiples, CurrencyConfig.SeparatorConfig separator) {
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
    
    // Utility methods
    public Map<String, Double> getAllPlayerBalances(UUID playerUuid) {
        return databaseManager.getAllPlayerBalances(playerUuid);
    }
    
    public Set<String> getAvailableCurrencies() {
        return configManager.getCurrencies().keySet();
    }
    
    public CurrencyConfig getCurrencyConfig(String currencyId) {
        return configManager.getCurrency(currencyId);
    }
    
    public void forceRankingUpdate(String currencyId) {
        databaseManager.updateRankingCache(currencyId);
        lastRankingUpdate.put(currencyId, System.currentTimeMillis());
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Result classes
    public static class PaymentResult {
        private final boolean success;
        private final String message;
        private final double fee;
        
        public PaymentResult(boolean success, String message) {
            this(success, message, 0.0);
        }
        
        public PaymentResult(boolean success, String message, double fee) {
            this.success = success;
            this.message = message;
            this.fee = fee;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public double getFee() { return fee; }
    }
}

