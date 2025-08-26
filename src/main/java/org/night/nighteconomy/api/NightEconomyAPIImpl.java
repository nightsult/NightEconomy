package org.night.nighteconomy.api;

import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.RankingEntry;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.Transaction;
import org.night.nighteconomy.placeholder.PlaceholderManager;
import org.night.nighteconomy.ranking.RankingManager;
import org.night.nighteconomy.service.MultiCurrencyEconomyService;
import org.night.nighteconomy.service.MultiCurrencyEconomyService.PaymentResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementação da API pública do NightEconomy
 * 
 * Esta classe fornece acesso completo ao sistema de economia multi-moeda
 * para outros mods que desejam integrar com o NightEconomy.
 * 
 * @author NightEconomy Team
 * @version 2.0.0
 * @since 1.0.0
 */
public class NightEconomyAPIImpl implements NightEconomyAPI {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String API_VERSION = "2.0.0";
    
    private final MultiCurrencyEconomyService economyService;
    private final ConfigManager configManager;
    private final PlaceholderManager placeholderManager;
    private final RankingManager rankingManager;
    private final List<EconomyEventListener> listeners = new CopyOnWriteArrayList<>();
    
    public NightEconomyAPIImpl(MultiCurrencyEconomyService economyService, 
                              ConfigManager configManager,
                              PlaceholderManager placeholderManager,
                              RankingManager rankingManager) {
        this.economyService = economyService;
        this.configManager = configManager;
        this.placeholderManager = placeholderManager;
        this.rankingManager = rankingManager;
    }
    
    // ========== CURRENCY MANAGEMENT ==========
    
    @Override
    public Set<String> getAvailableCurrencies() {
        return economyService.getAvailableCurrencies();
    }
    
    @Override
    public boolean currencyExists(String currencyId) {
        return configManager.getCurrency(currencyId) != null;
    }
    
    @Override
    public CurrencyConfig getCurrencyConfig(String currencyId) {
        return configManager.getCurrency(currencyId);
    }
    
    @Override
    public String getCurrencyName(String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        return config != null ? config.getName() : null;
    }
    
    @Override
    public double getCurrencyDefaultValue(String currencyId) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        return config != null ? config.getDefaultValue() : 0.0;
    }
    
    // ========== ACCOUNT MANAGEMENT ==========
    
    @Override
    public boolean createAccount(UUID playerUuid, String currencyId, String username) {
        boolean success = economyService.createAccount(playerUuid, currencyId, username);
        if (success) {
            CurrencyConfig config = configManager.getCurrency(currencyId);
            double initialBalance = config != null ? config.getDefaultValue() : 0.0;
            fireAccountCreatedEvent(playerUuid, currencyId, username, initialBalance);
        }
        return success;
    }
    
    @Override
    public boolean hasAccount(UUID playerUuid, String currencyId) {
        return economyService.getBalance(playerUuid, currencyId) >= 0;
    }
    
    @Override
    public void ensureAccountExists(UUID playerUuid, String currencyId, String username) {
        economyService.ensureAccountExists(playerUuid, currencyId, username);
    }
    
    // ========== BALANCE OPERATIONS ==========
    
    @Override
    public double getBalance(UUID playerUuid, String currencyId) {
        return economyService.getBalance(playerUuid, currencyId);
    }
    
    @Override
    public boolean setBalance(UUID playerUuid, String currencyId, double amount) {
        if (amount < 0) return false;
        
        double oldBalance = getBalance(playerUuid, currencyId);
        boolean success = economyService.setBalance(playerUuid, currencyId, amount);
        
        if (success) {
            fireBalanceChangeEvent(playerUuid, currencyId, oldBalance, amount, "SET");
        }
        
        return success;
    }
    
    @Override
    public boolean addBalance(UUID playerUuid, String currencyId, double amount) {
        if (amount <= 0) return false;
        
        double oldBalance = getBalance(playerUuid, currencyId);
        boolean success = economyService.addBalance(playerUuid, currencyId, amount);
        
        if (success) {
            fireBalanceChangeEvent(playerUuid, currencyId, oldBalance, oldBalance + amount, "ADD");
        }
        
        return success;
    }
    
    @Override
    public boolean subtractBalance(UUID playerUuid, String currencyId, double amount) {
        if (amount <= 0) return false;
        
        double oldBalance = getBalance(playerUuid, currencyId);
        boolean success = economyService.subtractBalance(playerUuid, currencyId, amount);
        
        if (success) {
            fireBalanceChangeEvent(playerUuid, currencyId, oldBalance, oldBalance - amount, "REMOVE");
        }
        
        return success;
    }
    
    @Override
    public boolean hasBalance(UUID playerUuid, String currencyId, double amount) {
        return getBalance(playerUuid, currencyId) >= amount;
    }
    
    @Override
    public boolean resetBalance(UUID playerUuid, String currencyId) {
        double oldBalance = getBalance(playerUuid, currencyId);
        boolean success = economyService.resetBalance(playerUuid, currencyId);
        
        if (success) {
            double newBalance = getCurrencyDefaultValue(currencyId);
            fireBalanceChangeEvent(playerUuid, currencyId, oldBalance, newBalance, "RESET");
        }
        
        return success;
    }
    
    @Override
    public Map<String, Double> getAllPlayerBalances(UUID playerUuid) {
        return economyService.getAllPlayerBalances(playerUuid);
    }
    
    // ========== PAYMENT SYSTEM ==========
    
    @Override
    public PaymentResult payPlayer(UUID fromUuid, UUID toUuid, String currencyId, double amount) {
        PaymentResult result = economyService.payPlayer(fromUuid, toUuid, currencyId, amount);
        
        firePaymentEvent(fromUuid, toUuid, currencyId, amount, result.getFee(), result.isSuccess());
        
        if (result.isSuccess()) {
            fireTransactionEvent(fromUuid, toUuid, currencyId, amount, result.getFee(), "PAY", "Pagamento entre jogadores");
        }
        
        return result;
    }
    
    @Override
    public boolean isPaymentEnabled(UUID playerUuid, String currencyId) {
        return economyService.isPaymentEnabled(playerUuid, currencyId);
    }
    
    @Override
    public boolean setPaymentEnabled(UUID playerUuid, String currencyId, boolean enabled) {
        boolean success = economyService.setPaymentEnabled(playerUuid, currencyId, enabled);
        
        if (success) {
            firePaymentSettingChangedEvent(playerUuid, currencyId, enabled);
        }
        
        return success;
    }
    
    @Override
    public double calculateTransactionFee(String currencyId, double amount) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config != null && config.getPayment() != null) {
            return amount * config.getPayment().getFee();
        }
        return 0.0;
    }
    
    // ========== TRANSACTION HISTORY ==========
    
    @Override
    public List<Transaction> getPlayerTransactions(UUID playerUuid, String currencyId, int limit) {
        return economyService.getPlayerTransactions(playerUuid, currencyId, Math.min(limit, 50));
    }
    
    @Override
    public boolean recordExternalTransaction(String currencyId, UUID fromUuid, UUID toUuid, 
                                           double amount, String type, String description) {
        // This would need to be implemented in the database manager
        // For now, we'll fire the event
        fireTransactionEvent(fromUuid, toUuid, currencyId, amount, 0.0, type, description);
        return true;
    }
    
    // ========== RANKING SYSTEM ==========
    
    @Override
    public boolean isRankingEnabled(String currencyId) {
        return rankingManager.isRankingEnabled(currencyId);
    }
    
    @Override
    public List<RankingEntry> getTopPlayers(String currencyId, int limit) {
        return economyService.getTopPlayers(currencyId, Math.min(limit, 100));
    }
    
    @Override
    public int getPlayerPosition(UUID playerUuid, String currencyId) {
        return economyService.getPlayerPosition(playerUuid, currencyId);
    }
    
    @Override
    public boolean isPlayerMagnata(UUID playerUuid, String currencyId) {
        return economyService.isPlayerMagnata(playerUuid, currencyId);
    }
    
    @Override
    public String getMagnataTag(String currencyId) {
        return economyService.getMagnataTag(currencyId);
    }
    
    @Override
    public void forceRankingUpdate(String currencyId) {
        economyService.forceRankingUpdate(currencyId);
        
        // Fire ranking updated event
        List<RankingEntry> topPlayers = getTopPlayers(currencyId, 1);
        UUID topPlayerUuid = topPlayers.isEmpty() ? null : UUID.fromString(topPlayers.get(0).getUuid());
        int totalPlayers = getTopPlayers(currencyId, 100).size();
        
        fireRankingUpdatedEvent(currencyId, topPlayerUuid, totalPlayers);
    }
    
    // ========== FORMATTING ==========
    
    @Override
    public String formatAmount(String currencyId, double amount) {
        return economyService.formatAmount(currencyId, amount);
    }
    
    @Override
    public String formatAmountRaw(String currencyId, double amount) {
        // Format without multiples/abbreviations
        return String.format("%.2f", amount);
    }
    
    // ========== PLACEHOLDERS ==========
    
    @Override
    public String processPlaceholder(UUID playerUuid, String placeholder) {
        return placeholderManager.processPlaceholder(playerUuid, placeholder);
    }
    
    @Override
    public String processPlaceholders(UUID playerUuid, String text) {
        return placeholderManager.processPlaceholders(playerUuid, text);
    }
    
    @Override
    public Map<String, String> getAllPlaceholders(UUID playerUuid) {
        return placeholderManager.getAllPlaceholders(playerUuid);
    }
    
    @Override
    public String[] getAvailablePlaceholders() {
        return placeholderManager.getAvailablePlaceholders();
    }
    
    // ========== CONFIGURATION ==========
    
    @Override
    public void reloadConfigurations() {
        configManager.reloadConfigurations();
        fireConfigurationsReloadedEvent();
    }
    
    @Override
    public void reloadCurrency(String currencyId) {
        configManager.reloadCurrency(currencyId);
        fireCurrencyReloadedEvent(currencyId);
    }
    
    @Override
    public String getMessage(String currencyId, String messageKey) {
        if (currencyId != null) {
            CurrencyConfig config = configManager.getCurrency(currencyId);
            if (config != null && config.getMessages() != null && config.getMessages().containsKey(messageKey)) {
                return config.getMessages().get(messageKey);
            }
        }
        return getGlobalMessage(messageKey);
    }
    
    @Override
    public String getGlobalMessage(String messageKey) {
        return configManager.getGlobalMessage(messageKey);
    }
    
    // ========== EVENTS ==========
    
    @Override
    public void registerEconomyListener(EconomyEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            LOGGER.debug("Listener de economia registrado: " + listener.getClass().getSimpleName());
        }
    }
    
    @Override
    public void unregisterEconomyListener(EconomyEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            LOGGER.debug("Listener de economia removido: " + listener.getClass().getSimpleName());
        }
    }
    
    // ========== UTILITY ==========
    
    @Override
    public String getAPIVersion() {
        return API_VERSION;
    }
    
    @Override
    public boolean isLuckPermsAvailable() {
        return placeholderManager.isLuckPermsAvailable();
    }
    
    @Override
    public Map<String, Object> getCurrencyStatistics(String currencyId) {
        Map<String, Object> stats = new HashMap<>();
        
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null) {
            return stats;
        }
        
        stats.put("id", config.getId());
        stats.put("name", config.getName());
        stats.put("defaultValue", config.getDefaultValue());
        stats.put("rankingEnabled", config.isRanking());
        stats.put("updateInterval", config.getUpdate());
        
        if (config.isRanking()) {
            List<RankingEntry> topPlayers = getTopPlayers(currencyId, 100);
            stats.put("totalPlayers", topPlayers.size());
            
            if (!topPlayers.isEmpty()) {
                stats.put("topPlayer", topPlayers.get(0).getUsername());
                stats.put("topBalance", topPlayers.get(0).getBalance());
                
                double totalBalance = topPlayers.stream().mapToDouble(RankingEntry::getBalance).sum();
                stats.put("totalBalance", totalBalance);
                stats.put("averageBalance", totalBalance / topPlayers.size());
            }
        }
        
        return stats;
    }
    
    // ========== EVENT FIRING METHODS ==========
    
    private void fireBalanceChangeEvent(UUID playerUuid, String currencyId, double oldBalance, double newBalance, String reason) {
        for (EconomyEventListener listener : listeners) {
            try {
                listener.onBalanceChange(playerUuid, currencyId, oldBalance, newBalance, reason);
            } catch (Exception e) {
                LOGGER.error("Erro ao executar listener de mudança de saldo: ", e);
            }
        }
    }
    
    private void fireTransactionEvent(UUID fromUuid, UUID toUuid, String currencyId, double amount, double fee, String type, String description) {
        for (EconomyEventListener listener : listeners) {
            try {
                listener.onTransaction(fromUuid, toUuid, currencyId, amount, fee, type, description);
            } catch (Exception e) {
                LOGGER.error("Erro ao executar listener de transação: ", e);
            }
        }
    }
    
    private void firePaymentEvent(UUID fromUuid, UUID toUuid, String currencyId, double amount, double fee, boolean success) {
        for (EconomyEventListener listener : listeners) {
            try {
                listener.onPayment(fromUuid, toUuid, currencyId, amount, fee, success);
            } catch (Exception e) {
                LOGGER.error("Erro ao executar listener de pagamento: ", e);
            }
        }
    }
    
    private void fireAccountCreatedEvent(UUID playerUuid, String currencyId, String username, double initialBalance) {
        for (EconomyEventListener listener : listeners) {
            try {
                listener.onAccountCreated(playerUuid, currencyId, username, initialBalance);
            } catch (Exception e) {
                LOGGER.error("Erro ao executar listener de criação de conta: ", e);
            }
        }
    }
    
    private void fireRankingUpdatedEvent(String currencyId, UUID topPlayerUuid, int totalPlayers) {
        for (EconomyEventListener listener : listeners) {
            try {
                listener.onRankingUpdated(currencyId, topPlayerUuid, totalPlayers);
            } catch (Exception e) {
                LOGGER.error("Erro ao executar listener de atualização de ranking: ", e);
            }
        }
    }
    
    private void firePaymentSettingChangedEvent(UUID playerUuid, String currencyId, boolean enabled) {
        for (EconomyEventListener listener : listeners) {
            try {
                listener.onPaymentSettingChanged(playerUuid, currencyId, enabled);
            } catch (Exception e) {
                LOGGER.error("Erro ao executar listener de configuração de pagamento: ", e);
            }
        }
    }
    
    private void fireCurrencyReloadedEvent(String currencyId) {
        for (EconomyEventListener listener : listeners) {
            try {
                listener.onCurrencyReloaded(currencyId);
            } catch (Exception e) {
                LOGGER.error("Erro ao executar listener de recarga de moeda: ", e);
            }
        }
    }
    
    private void fireConfigurationsReloadedEvent() {
        for (EconomyEventListener listener : listeners) {
            try {
                listener.onConfigurationsReloaded();
            } catch (Exception e) {
                LOGGER.error("Erro ao executar listener de recarga de configurações: ", e);
            }
        }
    }
}

