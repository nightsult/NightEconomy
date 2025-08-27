package org.night.nighteconomy.service;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.night.nighteconomy.api.TycoonInfo;
import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.RankingEntry;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.Transaction;
import org.night.nighteconomy.util.ChatUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.*;

public class MultiCurrencyEconomyService {
    private static final Logger LOGGER = LogManager.getLogger();

    private final MultiCurrencyDatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Long> lastRankingUpdate = new ConcurrentHashMap<>();
    // Executor single-thread dedicado para TODO acesso ao DB
    private final ExecutorService dbExecutor;

    // Cache de saldo em memória (read/write-through)
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Double>> balanceCache = new ConcurrentHashMap<>();

    // Cache de DecimalFormat por moeda/config (ThreadLocal porque DecimalFormat não é thread-safe)
    // Chave: currencyId|centavos(2|0)|sepChar
    private final ConcurrentHashMap<String, ThreadLocal<DecimalFormat>> formatterCache = new ConcurrentHashMap<>();

    // Retenção e manutenção (agendamento)
    private volatile double txRetentionDays = 30.0; // reter 30 dias de histórico
    private static final long PRUNE_INITIAL_DELAY_MIN = 5;     // primeira execução em 5 min
    private static final long PRUNE_INTERVAL_MIN = 60;         // a cada 60 min
    private static final long CHECKPOINT_INTERVAL_MIN = 30;    // a cada 30 min
    private static final long MAINTENANCE_INTERVAL_HOURS = 24; // a cada 24 h (VACUUM + ANALYZE)

    public MultiCurrencyEconomyService(MultiCurrencyDatabaseManager databaseManager,
                                       ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;

        // Single-threaded executor dedicado ao DB
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NE-DB");
            t.setDaemon(true);
            return t;
        });

        // Pool para agendamentos
        this.scheduler = Executors.newScheduledThreadPool(2);

        // Inicia os schedulers
        startRankingUpdateScheduler();
        startMaintenanceSchedulers();

        LOGGER.info("Economy service started. Transaction retention={} days. Schedulers initialized.", txRetentionDays);
    }

    private void startRankingUpdateScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateAllRankings();
            } catch (Exception e) {
                LOGGER.error("Erro ao atualizar rankings: ", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
        LOGGER.debug("Ranking update scheduler started with fixed rate 30s");
    }

    private void startMaintenanceSchedulers() {
        // Retenção de transações
        scheduler.scheduleAtFixedRate(() -> {
            try {
                dbExecutor.submit(() -> {
                    try {
                        int removed = databaseManager.pruneOldTransactions(txRetentionDays);
                        if (removed > 0) {
                            LOGGER.info("Pruned {} transactions older than {} days", removed, txRetentionDays);
                        } else {
                            LOGGER.debug("Pruning executed. No old transactions to remove (retention={} days)", txRetentionDays);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Erro no pruning de transactions: ", e);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Erro ao agendar pruning de transactions: ", e);
            }
        }, PRUNE_INITIAL_DELAY_MIN, PRUNE_INTERVAL_MIN, TimeUnit.MINUTES);
        LOGGER.debug("Transaction pruning scheduler started: initialDelay={}min, period={}min", PRUNE_INITIAL_DELAY_MIN, PRUNE_INTERVAL_MIN);

        // WAL checkpoint (TRUNCATE)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                dbExecutor.submit(() -> {
                    try {
                        boolean ok = databaseManager.walCheckpointTruncate();
                        LOGGER.debug("WAL checkpoint TRUNCATE executed, ok={}", ok);
                    } catch (Exception e) {
                        LOGGER.error("Erro no WAL checkpoint: ", e);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Erro ao agendar WAL checkpoint: ", e);
            }
        }, CHECKPOINT_INTERVAL_MIN, CHECKPOINT_INTERVAL_MIN, TimeUnit.MINUTES);
        LOGGER.debug("WAL checkpoint scheduler started: period={}min", CHECKPOINT_INTERVAL_MIN);

        // VACUUM + ANALYZE diários
        scheduler.scheduleAtFixedRate(() -> {
            try {
                dbExecutor.submit(() -> {
                    try {
                        boolean a = databaseManager.analyze();
                        boolean v = databaseManager.vacuum();
                        LOGGER.info("Maintenance executed: ANALYZE={}, VACUUM={}", a, v);
                    } catch (Exception e) {
                        LOGGER.error("Erro na manutenção (VACUUM/ANALYZE): ", e);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Erro ao agendar manutenção (VACUUM/ANALYZE): ", e);
            }
        }, MAINTENANCE_INTERVAL_HOURS, MAINTENANCE_INTERVAL_HOURS, TimeUnit.HOURS);
        LOGGER.debug("Maintenance scheduler started: period={}h", MAINTENANCE_INTERVAL_HOURS);
    }

    // Atualiza rankings das moedas de acordo com o intervalo; a execução de DB é delegada ao dbExecutor
    private void updateAllRankings() {
        Map<String, CurrencyConfig> currencies = configManager.getCurrencies();
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<String, CurrencyConfig> entry : currencies.entrySet()) {
            String currencyId = entry.getKey();
            CurrencyConfig config = entry.getValue();

            if (!config.isRanking()) continue;

            long lastUpdate = lastRankingUpdate.getOrDefault(currencyId, 0L);
            long updateInterval = config.getUpdate() * 1000L;

            if (currentTime - lastUpdate >= updateInterval) {
                final String cid = currencyId;
                dbExecutor.submit(() -> {
                    try {
                        // Atualiza cache de ranking
                        databaseManager.updateRankingCache(cid);

                        // Checa tycoon atual (top1) e compara com último salvo
                        RankingEntry top = databaseManager.getTopPlayerInfo(cid);
                        if (top != null && top.uuid != null) {
                            String prev = databaseManager.getLastTycoonUuid(cid);

                            // Sempre persistimos o atual; broadcast somente se havia anterior e mudou
                            boolean changed = (prev == null) ? false : !prev.equals(top.uuid);
                            databaseManager.upsertLastTycoon(cid, top.uuid, top.username != null ? top.username : "");

                            if (changed) {
                                broadcastNewTycoon(cid, top.username != null ? top.username : top.uuid);
                                LOGGER.info("New tycoon for currency {} -> {} (uuid={})", cid, top.username, top.uuid);
                            }
                        }

                        lastRankingUpdate.put(cid, System.currentTimeMillis());
                        LOGGER.debug("Ranking atualizado para moeda: {}", cid);
                    } catch (Exception e) {
                        LOGGER.error("Erro ao atualizar ranking para {}: ", cid, e);
                    }
                });
            }
        }
    }

    private void broadcastNewTycoon(String currencyId, String playerName) {
        try {
            CurrencyConfig cfg = configManager.getCurrency(currencyId);
            String template = (cfg != null && cfg.getTycoonBroadcast() != null && !cfg.getTycoonBroadcast().isEmpty())
                    ? cfg.getTycoonBroadcast()
                    : "&aPlayer &e%player% &ais the new server tycoon!";

            String msg = template.replace("%player%", playerName);
            String colored = ChatUtil.translateColors(msg);

            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> {
                    server.getPlayerList().broadcastSystemMessage(Component.literal(colored), false);
                });
            } else {
                LOGGER.warn("Servidor indisponível para broadcast do Tycoon: {}", colored);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao enviar broadcast de Tycoon: ", e);
        }
    }

    // --------------- API pública para manutenção/retention ---------------
    public void setTransactionRetentionDays(double days) {
        if (days < 0) days = 0;
        this.txRetentionDays = days;
        LOGGER.info("Transaction retention updated to {} days", days);
    }

    public double getTransactionRetentionDays() {
        return txRetentionDays;
    }

    public void pruneOldTransactionsNow() {
        pruneOldTransactionsNowAsync().join();
    }

    public CompletableFuture<Integer> pruneOldTransactionsNowAsync() {
        double days = this.txRetentionDays;
        return CompletableFuture.supplyAsync(() -> databaseManager.pruneOldTransactions(days), dbExecutor);
    }

    public void runMaintenanceNow(boolean doVacuum, boolean doAnalyze, boolean doCheckpoint) {
        runMaintenanceNowAsync(doVacuum, doAnalyze, doCheckpoint).join();
    }

    public CompletableFuture<Void> runMaintenanceNowAsync(boolean doVacuum, boolean doAnalyze, boolean doCheckpoint) {
        return CompletableFuture.runAsync(() -> {
            if (doAnalyze) {
                boolean a = databaseManager.analyze();
                LOGGER.info("Manual ANALYZE executed: {}", a);
            }
            if (doVacuum) {
                boolean v = databaseManager.vacuum();
                LOGGER.info("Manual VACUUM executed: {}", v);
            }
            if (doCheckpoint) {
                boolean c = databaseManager.walCheckpointTruncate();
                LOGGER.info("Manual WAL checkpoint TRUNCATE executed: {}", c);
            }
        }, dbExecutor);
    }

    // --------------- Cache helpers (saldo) ---------------
    private ConcurrentHashMap<String, Double> userCache(UUID playerUuid) {
        return balanceCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
    }

    private Double getCachedBalance(UUID playerUuid, String currencyId) {
        var map = balanceCache.get(playerUuid);
        return map != null ? map.get(currencyId) : null;
    }

    private void putCachedBalance(UUID playerUuid, String currencyId, double value) {
        userCache(playerUuid).put(currencyId, value);
    }

    public void invalidateCachedBalance(UUID playerUuid, String currencyId) {
        var map = balanceCache.get(playerUuid);
        if (map != null) map.remove(currencyId);
    }

    public void clearPlayerCache(UUID playerUuid) {
        balanceCache.remove(playerUuid);
    }

    public void clearAllCache() {
        balanceCache.clear();
    }

    // --------------- Cache helpers (formatters) ---------------
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
                    df.setGroupingUsed(false); // sem separador de milhar
                    return df;
                })
        );
        return tl.get();
    }

    public void clearFormatterCache() {
        formatterCache.clear();
    }

    public void invalidateFormatterCacheForCurrency(String currencyId) {
        String prefix = currencyId + "|";
        formatterCache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    // --------------- Accounts ---------------
    public boolean createAccount(UUID playerUuid, String currencyId, String username) {
        return createAccountAsync(playerUuid, currencyId, username).join();
    }

    public CompletableFuture<Boolean> createAccountAsync(UUID playerUuid, String currencyId, String username) {
        return CompletableFuture.supplyAsync(() -> {
            CurrencyConfig cfg = configManager.getCurrency(currencyId);
            if (cfg == null) return false;
            boolean created = databaseManager.createAccount(playerUuid, currencyId, username, cfg.getDefaultValue());
            if (created) {
                putCachedBalance(playerUuid, currencyId, cfg.getDefaultValue());
            }
            return created;
        }, dbExecutor);
    }

    public void ensureAccountExists(UUID playerUuid, String currencyId, String username) {
        ensureAccountExistsAsync(playerUuid, currencyId, username).join();
    }

    public CompletableFuture<Void> ensureAccountExistsAsync(UUID playerUuid, String currencyId, String username) {
        return CompletableFuture.runAsync(() -> {
            if (!databaseManager.hasAccount(playerUuid, currencyId)) {
                CurrencyConfig cfg = configManager.getCurrency(currencyId);
                double initial = (cfg != null) ? cfg.getDefaultValue() : 0.0;
                boolean created = databaseManager.createAccount(playerUuid, currencyId, username, initial);
                if (created) {
                    putCachedBalance(playerUuid, currencyId, initial);
                }
            } else {
                Double cached = getCachedBalance(playerUuid, currencyId);
                if (cached == null) {
                    double current = databaseManager.getBalance(playerUuid, currencyId);
                    putCachedBalance(playerUuid, currencyId, current);
                }
            }
        }, dbExecutor);
    }

    // --------------- Transactions (history) ---------------
    public CompletableFuture<List<Transaction>> getPlayerTransactionsAsync(UUID playerUuid, String currencyId, int limit) {
        int lim = Math.min(limit, 50);
        return CompletableFuture.supplyAsync(
                () -> databaseManager.getPlayerTransactions(playerUuid, currencyId, lim),
                dbExecutor
        );
    }

    public List<Transaction> getPlayerTransactions(UUID playerUuid, String currencyId, int limit) {
        return getPlayerTransactionsAsync(playerUuid, currencyId, limit).join();
    }

    // --------------- Balances (read-through/write-through) ---------------
    public double getBalance(UUID playerUuid, String currencyId) {
        return getBalanceAsync(playerUuid, currencyId).join();
    }

    public CompletableFuture<Double> getBalanceAsync(UUID playerUuid, String currencyId) {
        Double cached = getCachedBalance(playerUuid, currencyId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> {
            double dbValue = databaseManager.getBalance(playerUuid, currencyId);
            putCachedBalance(playerUuid, currencyId, dbValue);
            return dbValue;
        }, dbExecutor);
    }

    public boolean setBalance(UUID playerUuid, String currencyId, double amount) {
        return setBalanceAsync(playerUuid, currencyId, amount).join();
    }

    public CompletableFuture<Boolean> setBalanceAsync(UUID playerUuid, String currencyId, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            boolean ok = databaseManager.setBalance(playerUuid, currencyId, amount);
            if (ok) putCachedBalance(playerUuid, currencyId, amount);
            else invalidateCachedBalance(playerUuid, currencyId);
            return ok;
        }, dbExecutor);
    }

    public boolean addBalance(UUID playerUuid, String currencyId, double amount) {
        return addBalanceAsync(playerUuid, currencyId, amount).join();
    }

    public CompletableFuture<Boolean> addBalanceAsync(UUID playerUuid, String currencyId, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            boolean ok = databaseManager.addBalance(playerUuid, currencyId, amount);
            if (ok) {
                double newVal = databaseManager.getBalance(playerUuid, currencyId);
                putCachedBalance(playerUuid, currencyId, newVal);
            } else {
                invalidateCachedBalance(playerUuid, currencyId);
            }
            return ok;
        }, dbExecutor);
    }

    public boolean subtractBalance(UUID playerUuid, String currencyId, double amount) {
        return subtractBalanceAsync(playerUuid, currencyId, amount).join();
    }

    public CompletableFuture<Boolean> subtractBalanceAsync(UUID playerUuid, String currencyId, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            boolean ok = databaseManager.subtractBalance(playerUuid, currencyId, amount);
            if (ok) {
                double newVal = databaseManager.getBalance(playerUuid, currencyId);
                putCachedBalance(playerUuid, currencyId, newVal);
            } else {
                invalidateCachedBalance(playerUuid, currencyId);
            }
            return ok;
        }, dbExecutor);
    }

    public boolean resetBalance(UUID playerUuid, String currencyId) {
        return resetBalanceAsync(playerUuid, currencyId).join();
    }

    public CompletableFuture<Boolean> resetBalanceAsync(UUID playerUuid, String currencyId) {
        return CompletableFuture.supplyAsync(() -> {
            CurrencyConfig config = configManager.getCurrency(currencyId);
            if (config == null) return false;
            boolean success = databaseManager.resetPlayerBalance(playerUuid, currencyId, config.getDefaultValue());
            if (success) {
                databaseManager.recordTransaction(currencyId, null, playerUuid, config.getDefaultValue(), 0.0, "RESET", "Saldo resetado por administrador");
                putCachedBalance(playerUuid, currencyId, config.getDefaultValue());
            } else {
                invalidateCachedBalance(playerUuid, currencyId);
            }
            return success;
        }, dbExecutor);
    }

    // --------------- Payment toggle ---------------
    public boolean isPaymentEnabled(UUID playerUuid, String currencyId) {
        return isPaymentEnabledAsync(playerUuid, currencyId).join();
    }

    public CompletableFuture<Boolean> isPaymentEnabledAsync(UUID playerUuid, String currencyId) {
        return CompletableFuture.supplyAsync(
                () -> databaseManager.isPaymentEnabled(playerUuid, currencyId),
                dbExecutor
        );
    }

    public boolean setPaymentEnabled(UUID playerUuid, String currencyId, boolean enabled) {
        return setPaymentEnabledAsync(playerUuid, currencyId, enabled).join();
    }

    public CompletableFuture<Boolean> setPaymentEnabledAsync(UUID playerUuid, String currencyId, boolean enabled) {
        return CompletableFuture.supplyAsync(
                () -> databaseManager.setPaymentEnabled(playerUuid, currencyId, enabled),
                dbExecutor
        );
    }

    // --------------- Ranking ---------------
    public List<RankingEntry> getTopPlayers(String currencyId, int limit) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null || !config.isRanking()) {
            return new ArrayList<>();
        }
        return getTopPlayersAsync(currencyId, Math.min(limit, 100)).join();
    }

    public CompletableFuture<List<RankingEntry>> getTopPlayersAsync(String currencyId, int limit) {
        int lim = Math.min(limit, 100);
        return CompletableFuture.supplyAsync(
                () -> databaseManager.getTopPlayers(currencyId, lim),
                dbExecutor
        );
    }

    public int getPlayerPosition(UUID playerUuid, String currencyId) {
        return getPlayerPositionAsync(playerUuid, currencyId).join();
    }

    public CompletableFuture<Integer> getPlayerPositionAsync(UUID playerUuid, String currencyId) {
        return CompletableFuture.supplyAsync(() -> {
            CurrencyConfig config = configManager.getCurrency(currencyId);
            if (config == null || !config.isRanking()) return -1;
            return databaseManager.getPlayerPosition(playerUuid, currencyId);
        }, dbExecutor);
    }

    public boolean isPlayerTycoon(UUID playerUuid, String currencyId) {
        return isPlayerTycoonAsync(playerUuid, currencyId).join();
    }

    public CompletableFuture<Boolean> isPlayerTycoonAsync(UUID playerUuid, String currencyId) {
        return CompletableFuture.supplyAsync(() -> {
            String topPlayerUuid = databaseManager.getTopPlayerUuid(currencyId);
            return topPlayerUuid != null && topPlayerUuid.equals(playerUuid.toString());
        }, dbExecutor);
    }

    // Backward-compat method name
    public boolean isPlayerMagnata(UUID playerUuid, String currencyId) {
        return isPlayerTycoon(playerUuid, currencyId);
    }

    public void forceRankingUpdate(String currencyId) {
        forceRankingUpdateAsync(currencyId).join();
    }

    public CompletableFuture<Void> forceRankingUpdateAsync(String currencyId) {
        return CompletableFuture.runAsync(() -> {
            databaseManager.updateRankingCache(currencyId);

            RankingEntry top = databaseManager.getTopPlayerInfo(currencyId);
            if (top != null && top.uuid != null) {
                String prev = databaseManager.getLastTycoonUuid(currencyId);
                boolean changed = (prev == null) ? false : !prev.equals(top.uuid);
                databaseManager.upsertLastTycoon(currencyId, top.uuid, top.username != null ? top.username : "");
                if (changed) {
                    broadcastNewTycoon(currencyId, top.username != null ? top.username : top.uuid);
                }
            }

            lastRankingUpdate.put(currencyId, System.currentTimeMillis());
        }, dbExecutor);
    }

    public TycoonInfo getCurrentTycoonInfo(String currencyId) {
        var top = databaseManager.getTopPlayerInfo(currencyId);
        if (top == null || top.uuid == null) return null;
        try {
            return new TycoonInfo(java.util.UUID.fromString(top.uuid), top.username);
        } catch (IllegalArgumentException ex) {
            // uuid inválido no cache (não deveria acontecer)
            return new TycoonInfo(null, top.username);
        }
    }

    /**
     * Último Tycoon persistido (currency_state). Null se não houver registro.
     */
    public TycoonInfo getLastKnownTycoonInfo(String currencyId) {
        var rec = databaseManager.getLastTycoonInfo(currencyId);
        if (rec == null || rec.uuid == null) return null;
        try {
            return new TycoonInfo(java.util.UUID.fromString(rec.uuid), rec.username);
        } catch (IllegalArgumentException ex) {
            return new TycoonInfo(null, rec.username);
        }
    }

    /**
     * Tag do Tycoon configurada na moeda (string de cores com &).
     */
    public String getTycoonTag(String currencyId) {
        var cfg = configManager.getCurrency(currencyId);
        return cfg != null && cfg.getTycoon() != null ? cfg.getTycoon() : "";
    }

    // Backward-compat getter name
    public String getMagnataTag(String currencyId) {
        return getTycoonTag(currencyId);
    }

    public String formatAmount(String currencyId, double amount) {
        CurrencyConfig config = configManager.getCurrency(currencyId);

        boolean showCents = true;
        char sepChar = ',';

        if (config != null && config.getFormat() != null) {
            CurrencyConfig.FormatConfig fmt = config.getFormat();
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
        }

        DecimalFormat df = getOrCreateFormatter(currencyId, showCents, sepChar);
        return df.format(amount);
    }

    // --------------- Utilities ---------------
    public Map<String, Double> getAllPlayerBalances(UUID playerUuid) {
        return getAllPlayerBalancesAsync(playerUuid).join();
    }

    public CompletableFuture<Map<String, Double>> getAllPlayerBalancesAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Double> balances = databaseManager.getAllPlayerBalances(playerUuid);
            if (balances != null) {
                for (Map.Entry<String, Double> e : balances.entrySet()) {
                    putCachedBalance(playerUuid, e.getKey(), e.getValue());
                }
            }
            return balances;
        }, dbExecutor);
    }

    public Set<String> getAvailableCurrencies() {
        return configManager.getCurrencies().keySet();
    }

    public CurrencyConfig getCurrencyConfig(String currencyId) {
        return configManager.getCurrency(currencyId);
    }

    // --------------- Shutdown ---------------
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

        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            databaseManager.close();
        } catch (Exception ignore) {}
        LOGGER.info("Economy service shutdown complete.");
    }

    // --------------- Payment (atomic in DB) ---------------
    public PaymentResult payPlayer(UUID senderUuid, UUID receiverUuid, String currencyId, double amount) {
        return payPlayerAsync(senderUuid, receiverUuid, currencyId, amount).join();
    }

    public CompletableFuture<PaymentResult> payPlayerAsync(UUID senderUuid,
                                                           UUID receiverUuid,
                                                           String currencyId,
                                                           double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount <= 0) return new PaymentResult(false, "Quantia inválida");

            CurrencyConfig cfg = configManager.getCurrency(currencyId);
            if (cfg == null) return new PaymentResult(false, "Moeda inexistente");

            double fee = 0.0;
            if (cfg.getPayment() != null) {
                fee = Math.max(0.0, cfg.getPayment().getFee());
            }

            MultiCurrencyDatabaseManager.PayTxResult res =
                    databaseManager.payAtomic(senderUuid, receiverUuid, currencyId, amount, fee);

            switch (res.status) {
                case RECEIVER_BLOCKED:
                    return new PaymentResult(false, "Jogador não aceita pagamentos");
                case INSUFFICIENT_FUNDS:
                    return new PaymentResult(false, "Saldo insuficiente");
                case SENDER_NOT_FOUND:
                case RECEIVER_NOT_FOUND:
                    return new PaymentResult(false, "Conta inexistente");
                case ERROR:
                    return new PaymentResult(false, "Erro ao processar pagamento");
                case OK:
                default:
                    // Atualiza cache de ambos após commit
                    double senderNew = databaseManager.getBalance(senderUuid, currencyId);
                    putCachedBalance(senderUuid, currencyId, senderNew);

                    double receiverNew = databaseManager.getBalance(receiverUuid, currencyId);
                    putCachedBalance(receiverUuid, currencyId, receiverNew);

                    return new PaymentResult(true, "OK", fee);
            }
        }, dbExecutor);
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