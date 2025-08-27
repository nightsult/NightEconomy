package org.night.nighteconomy.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

public class MultiCurrencyDatabaseManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;

    private int busyTimeoutMs = 10_000;
    private int walAutocheckpointPages = 1000;
    private long mmapSizeBytes = 268_435_456L;

    private PreparedStatement psHasAccount;
    private PreparedStatement psCreateAccount;
    private PreparedStatement psGetBalance;
    private PreparedStatement psSetBalance;
    private PreparedStatement psAddBalance;
    private PreparedStatement psSubBalanceNoCheck;
    private PreparedStatement psResetBalance;
    private PreparedStatement psIsPaymentEnabled;
    private PreparedStatement psSetPaymentEnabled;
    private PreparedStatement psRecordTransaction;
    private PreparedStatement psGetPlayerTransactions;
    private PreparedStatement psGetAllPlayerBalances;
    private PreparedStatement psGetTopPlayers;
    private PreparedStatement psGetPlayerPosition;
    private PreparedStatement psGetTopPlayerUuid;
    private PreparedStatement psGetTopPlayerInfo;
    private PreparedStatement psDeleteRankingCache;
    private PreparedStatement psInsertRankingCache;
    private PreparedStatement psDeleteOldTransactionsDays;

    private PreparedStatement psGetLastTycoon;
    private PreparedStatement psGetLastTycoonInfo;
    private PreparedStatement psUpsertLastTycoon;

    public MultiCurrencyDatabaseManager(Connection conn) throws SQLException {
        this.conn = conn;
        this.conn.setAutoCommit(true);

        applyPragmas();
        logEnvironment();

        ensureSchema();

        prepareStatements();
    }

    private void applyPragmas() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            try (ResultSet rs = st.executeQuery("PRAGMA journal_mode = WAL")) {
                if (rs.next()) {
                    String mode = rs.getString(1);
                    LOGGER.info("SQLite journal_mode set to {}", mode);
                }
            }

            st.execute("PRAGMA busy_timeout = " + busyTimeoutMs);

            st.execute("PRAGMA wal_autocheckpoint = " + walAutocheckpointPages);

            if (mmapSizeBytes >= 0) {
                st.execute("PRAGMA mmap_size = " + mmapSizeBytes);
            }
        } catch (SQLException e) {
            LOGGER.warn("Failed to apply PRAGMAs: {}", e.getMessage());
            throw e;
        }

        logPragmasStatus();
    }

    private void logEnvironment() {
        try (Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("select sqlite_version()")) {
                if (rs.next()) {
                    LOGGER.info("SQLite version: {}", rs.getString(1));
                }
            }

            try (ResultSet rs = st.executeQuery("PRAGMA compile_options")) {
                List<String> options = new ArrayList<>();
                while (rs.next()) {
                    options.add(rs.getString(1));
                }
                if (!options.isEmpty()) {
                    LOGGER.debug("SQLite compile options: {}", String.join(", ", options));
                }
            }
        } catch (SQLException e) {
            LOGGER.debug("Unable to query SQLite environment: {}", e.getMessage());
        }
    }

    private String getSinglePragma(String pragma) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA " + pragma)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException ignored) { }
        return null;
    }

    private void logPragmasStatus() {
        String journalMode = getSinglePragma("journal_mode");
        String timeout = getSinglePragma("busy_timeout");
        String autoCheckpoint = getSinglePragma("wal_autocheckpoint");
        String mmap = getSinglePragma("mmap_size");
        String synchronous = getSinglePragma("synchronous");

        LOGGER.info("SQLite PRAGMA status -> journal_mode={}, busy_timeout(ms)={}, wal_autocheckpoint={}, mmap_size(bytes)={}, synchronous={}",
                journalMode, timeout, autoCheckpoint, mmap, synchronous);
    }

    public void setBusyTimeoutMs(int busyTimeoutMs) {
        if (busyTimeoutMs < 0) busyTimeoutMs = 0;
        this.busyTimeoutMs = busyTimeoutMs;
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA busy_timeout = " + busyTimeoutMs);
            LOGGER.info("Updated PRAGMA busy_timeout to {} ms", busyTimeoutMs);
        } catch (SQLException e) {
            LOGGER.warn("Failed to update busy_timeout: {}", e.getMessage());
        }
    }

    public void setWalAutocheckpointPages(int walAutocheckpointPages) {
        if (walAutocheckpointPages < 0) walAutocheckpointPages = 0;
        this.walAutocheckpointPages = walAutocheckpointPages;
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA wal_autocheckpoint = " + walAutocheckpointPages);
            LOGGER.info("Updated PRAGMA wal_autocheckpoint to {} pages", walAutocheckpointPages);
        } catch (SQLException e) {
            LOGGER.warn("Failed to update wal_autocheckpoint: {}", e.getMessage());
        }
    }

    public void setMmapSizeBytes(long mmapSizeBytes) {
        if (mmapSizeBytes < 0) mmapSizeBytes = 0;
        this.mmapSizeBytes = mmapSizeBytes;
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA mmap_size = " + mmapSizeBytes);
            String actual = getSinglePragma("mmap_size");
            LOGGER.info("Updated PRAGMA mmap_size request={} bytes, actual={}", mmapSizeBytes, actual);
        } catch (SQLException e) {
            LOGGER.warn("Failed to update mmap_size: {}", e.getMessage());
        }
    }

    public int getBusyTimeoutMs() { return busyTimeoutMs; }
    public int getWalAutocheckpointPages() { return walAutocheckpointPages; }
    public long getMmapSizeBytes() { return mmapSizeBytes; }

    private void ensureSchema() throws SQLException {
        int userVersion = 0;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            if (rs.next()) userVersion = rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.warn("Could not read PRAGMA user_version: {}", e.getMessage());
        }

        try (Statement st = conn.createStatement()) {
            if (userVersion < 1) {
                LOGGER.info("Initializing NightEconomy schema (v1)...");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS accounts (
                          uuid TEXT NOT NULL,
                          currency_id TEXT NOT NULL,
                          username TEXT NOT NULL,
                          balance REAL NOT NULL DEFAULT 0,
                          payment_enabled INTEGER NOT NULL DEFAULT 1,
                          PRIMARY KEY (uuid, currency_id)
                        )
                        """);

                st.execute("""
                        CREATE TABLE IF NOT EXISTS transactions (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          currency_id TEXT NOT NULL,
                          sender_uuid TEXT,
                          receiver_uuid TEXT,
                          amount REAL NOT NULL,
                          fee REAL NOT NULL DEFAULT 0,
                          type TEXT NOT NULL,
                          description TEXT,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                st.execute("CREATE INDEX IF NOT EXISTS idx_tx_sender_currency_date ON transactions(sender_uuid, currency_id, created_at DESC)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_tx_receiver_currency_date ON transactions(receiver_uuid, currency_id, created_at DESC)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_tx_currency_date ON transactions(currency_id, created_at DESC)");

                st.execute("""
                        CREATE TABLE IF NOT EXISTS ranking_cache (
                          currency_id TEXT NOT NULL,
                          uuid TEXT NOT NULL,
                          username TEXT NOT NULL,
                          balance REAL NOT NULL,
                          position INTEGER NOT NULL,
                          PRIMARY KEY (currency_id, position)
                        )
                        """);
                st.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_rank_currency_uuid ON ranking_cache(currency_id, uuid)");

                st.execute("CREATE INDEX IF NOT EXISTS idx_accounts_currency_uuid ON accounts(currency_id, uuid)");

                st.execute("PRAGMA user_version = 1");
                LOGGER.info("NightEconomy schema initialized (v1).");
                userVersion = 1;
            }

            if (userVersion < 2) {
                LOGGER.info("Applying schema migration to v2 (currency_state)...");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS currency_state (
                          currency_id TEXT PRIMARY KEY,
                          tycoon_uuid TEXT,
                          tycoon_username TEXT,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                st.execute("PRAGMA user_version = 2");
                LOGGER.info("Schema migration to v2 completed.");
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to initialize/migrate schema: ", e);
            throw e;
        }
    }

    private void prepareStatements() throws SQLException {
        psHasAccount = conn.prepareStatement(
                "SELECT 1 FROM accounts WHERE uuid=? AND currency_id=?"
        );
        psCreateAccount = conn.prepareStatement(
                "INSERT OR IGNORE INTO accounts (uuid, currency_id, username, balance, payment_enabled) VALUES (?,?,?,?,1)"
        );
        psGetBalance = conn.prepareStatement(
                "SELECT balance FROM accounts WHERE uuid=? AND currency_id=?"
        );
        psSetBalance = conn.prepareStatement(
                "UPDATE accounts SET balance=? WHERE uuid=? AND currency_id=?"
        );
        psAddBalance = conn.prepareStatement(
                "UPDATE accounts SET balance = balance + ? WHERE uuid=? AND currency_id=?"
        );
        psSubBalanceNoCheck = conn.prepareStatement(
                "UPDATE accounts SET balance = balance - ? WHERE uuid=? AND currency_id=?"
        );
        psResetBalance = conn.prepareStatement(
                "UPDATE accounts SET balance=? WHERE uuid=? AND currency_id=?"
        );
        psIsPaymentEnabled = conn.prepareStatement(
                "SELECT payment_enabled FROM accounts WHERE uuid=? AND currency_id=?"
        );
        psSetPaymentEnabled = conn.prepareStatement(
                "UPDATE accounts SET payment_enabled=? WHERE uuid=? AND currency_id=?"
        );
        psRecordTransaction = conn.prepareStatement(
                "INSERT INTO transactions (currency_id, sender_uuid, receiver_uuid, amount, fee, type, description, created_at) " +
                        "VALUES (?,?,?,?,?,?,?,CURRENT_TIMESTAMP)"
        );
        psGetPlayerTransactions = conn.prepareStatement(
                "SELECT currency_id, sender_uuid, receiver_uuid, amount, fee, type, description, created_at " +
                        "FROM transactions WHERE (sender_uuid=? OR receiver_uuid=?) AND currency_id=? " +
                        "ORDER BY created_at DESC LIMIT ?"
        );
        psGetAllPlayerBalances = conn.prepareStatement(
                "SELECT currency_id, balance FROM accounts WHERE uuid=?"
        );
        psGetTopPlayers = conn.prepareStatement(
                "SELECT uuid, username, balance, position FROM ranking_cache " +
                        "WHERE currency_id=? ORDER BY position ASC LIMIT ?"
        );
        psGetPlayerPosition = conn.prepareStatement(
                "SELECT position FROM ranking_cache WHERE currency_id=? AND uuid=?"
        );
        psGetTopPlayerUuid = conn.prepareStatement(
                "SELECT uuid FROM ranking_cache WHERE currency_id=? ORDER BY position ASC LIMIT 1"
        );
        psGetTopPlayerInfo = conn.prepareStatement(
                "SELECT uuid, username FROM ranking_cache WHERE currency_id=? ORDER BY position ASC LIMIT 1"
        );
        psDeleteRankingCache = conn.prepareStatement(
                "DELETE FROM ranking_cache WHERE currency_id=?"
        );
        psInsertRankingCache = conn.prepareStatement(
                "INSERT INTO ranking_cache (currency_id, uuid, username, balance, position) " +
                        "SELECT ?, a.uuid, a.username, a.balance, " +
                        "ROW_NUMBER() OVER (ORDER BY a.balance DESC, a.uuid ASC) as pos " +
                        "FROM accounts a WHERE a.currency_id=?"
        );

        psDeleteOldTransactionsDays = conn.prepareStatement(
                "DELETE FROM transactions WHERE julianday(created_at) < julianday('now') - ?"
        );

        psGetLastTycoon = conn.prepareStatement(
                "SELECT tycoon_uuid FROM currency_state WHERE currency_id=?"
        );
        psGetLastTycoonInfo = conn.prepareStatement(
                "SELECT tycoon_uuid, tycoon_username FROM currency_state WHERE currency_id=?"
        );
        psUpsertLastTycoon = conn.prepareStatement(
                "INSERT INTO currency_state (currency_id, tycoon_uuid, tycoon_username, updated_at) " +
                        "VALUES (?,?,?,CURRENT_TIMESTAMP) " +
                        "ON CONFLICT(currency_id) DO UPDATE SET " +
                        "tycoon_uuid=excluded.tycoon_uuid, " +
                        "tycoon_username=excluded.tycoon_username, " +
                        "updated_at=CURRENT_TIMESTAMP"
        );
    }

    public static class Transaction {
        public final String currencyId;
        public final String senderUuid;
        public final String receiverUuid;
        public final double amount;
        public final double fee;
        public final String type;
        public final String description;
        public final Timestamp createdAt;

        public Transaction(String currencyId, String senderUuid, String receiverUuid,
                           double amount, double fee, String type, String description, Timestamp createdAt) {
            this.currencyId = currencyId;
            this.senderUuid = senderUuid;
            this.receiverUuid = receiverUuid;
            this.amount = amount;
            this.fee = fee;
            this.type = type;
            this.description = description;
            this.createdAt = createdAt;
        }

        public String getCurrencyId() { return currencyId; }
        public String getSenderUuid() { return senderUuid; }
        public String getReceiverUuid() { return receiverUuid; }
        public double getAmount() { return amount; }
        public double getFee() { return fee; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public Timestamp getCreatedAt() { return createdAt; }
    }

    public static class RankingEntry {
        public final String uuid;
        public final String username;
        public final double balance;
        public final int position;

        public RankingEntry(String uuid, String username, double balance, int position) {
            this.uuid = uuid;
            this.username = username;
            this.balance = balance;
            this.position = position;
        }

        public String getUuid() { return uuid; }
        public String getUsername() { return username; }
        public double getBalance() { return balance; }
        public int getPosition() { return position; }
    }

    public static class TycoonStateRecord {
        public final String uuid;
        public final String username;

        public TycoonStateRecord(String uuid, String username) {
            this.uuid = uuid;
            this.username = username;
        }
    }

    public static class PayTxResult {
        public enum Status { OK, RECEIVER_BLOCKED, INSUFFICIENT_FUNDS, SENDER_NOT_FOUND, RECEIVER_NOT_FOUND, ERROR }
        public final Status status;

        public PayTxResult(Status status) {
            this.status = status;
        }
    }

    public boolean hasAccount(UUID playerUuid, String currencyId) {
        try {
            psHasAccount.clearParameters();
            psHasAccount.setString(1, playerUuid.toString());
            psHasAccount.setString(2, currencyId);
            try (ResultSet rs = psHasAccount.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.error("hasAccount error", e);
            return false;
        }
    }

    public boolean createAccount(UUID playerUuid, String currencyId, String username, double defaultValue) {
        try {
            psCreateAccount.clearParameters();
            psCreateAccount.setString(1, playerUuid.toString());
            psCreateAccount.setString(2, currencyId);
            psCreateAccount.setString(3, username);
            psCreateAccount.setDouble(4, defaultValue);
            int rows = psCreateAccount.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.error("createAccount error", e);
            return false;
        }
    }

    public double getBalance(UUID playerUuid, String currencyId) {
        try {
            psGetBalance.clearParameters();
            psGetBalance.setString(1, playerUuid.toString());
            psGetBalance.setString(2, currencyId);
            try (ResultSet rs = psGetBalance.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            LOGGER.error("getBalance error", e);
        }
        return 0.0;
    }

    public boolean setBalance(UUID playerUuid, String currencyId, double amount) {
        try {
            psSetBalance.clearParameters();
            psSetBalance.setDouble(1, amount);
            psSetBalance.setString(2, playerUuid.toString());
            psSetBalance.setString(3, currencyId);
            return psSetBalance.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("setBalance error", e);
            return false;
        }
    }

    public boolean addBalance(UUID playerUuid, String currencyId, double amount) {
        try {
            psAddBalance.clearParameters();
            psAddBalance.setDouble(1, amount);
            psAddBalance.setString(2, playerUuid.toString());
            psAddBalance.setString(3, currencyId);
            return psAddBalance.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("addBalance error", e);
            return false;
        }
    }

    public boolean subtractBalance(UUID playerUuid, String currencyId, double amount) {
        try {
            psSubBalanceNoCheck.clearParameters();
            psSubBalanceNoCheck.setDouble(1, amount);
            psSubBalanceNoCheck.setString(2, playerUuid.toString());
            psSubBalanceNoCheck.setString(3, currencyId);
            return psSubBalanceNoCheck.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("subtractBalance error", e);
            return false;
        }
    }

    public boolean resetPlayerBalance(UUID playerUuid, String currencyId, double defaultValue) {
        try {
            psResetBalance.clearParameters();
            psResetBalance.setDouble(1, defaultValue);
            psResetBalance.setString(2, playerUuid.toString());
            psResetBalance.setString(3, currencyId);
            return psResetBalance.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("resetPlayerBalance error", e);
            return false;
        }
    }

    public boolean isPaymentEnabled(UUID playerUuid, String currencyId) {
        try {
            psIsPaymentEnabled.clearParameters();
            psIsPaymentEnabled.setString(1, playerUuid.toString());
            psIsPaymentEnabled.setString(2, currencyId);
            try (ResultSet rs = psIsPaymentEnabled.executeQuery()) {
                if (rs.next()) return rs.getInt(1) != 0;
            }
        } catch (SQLException e) {
            LOGGER.error("isPaymentEnabled error", e);
        }
        return true;
    }

    public boolean setPaymentEnabled(UUID playerUuid, String currencyId, boolean enabled) {
        try {
            psSetPaymentEnabled.clearParameters();
            psSetPaymentEnabled.setInt(1, enabled ? 1 : 0);
            psSetPaymentEnabled.setString(2, playerUuid.toString());
            psSetPaymentEnabled.setString(3, currencyId);
            return psSetPaymentEnabled.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("setPaymentEnabled error", e);
            return false;
        }
    }

    public void recordTransaction(String currencyId, UUID sender, UUID receiver, double amount, double fee, String type, String description) {
        try {
            psRecordTransaction.clearParameters();
            psRecordTransaction.setString(1, currencyId);
            psRecordTransaction.setString(2, sender != null ? sender.toString() : null);
            psRecordTransaction.setString(3, receiver != null ? receiver.toString() : null);
            psRecordTransaction.setDouble(4, amount);
            psRecordTransaction.setDouble(5, fee);
            psRecordTransaction.setString(6, type);
            psRecordTransaction.setString(7, description);
            psRecordTransaction.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("recordTransaction error", e);
        }
    }

    public List<Transaction> getPlayerTransactions(UUID playerUuid, String currencyId, int limit) {
        List<Transaction> list = new ArrayList<>();
        try {
            psGetPlayerTransactions.clearParameters();
            String uid = playerUuid.toString();
            psGetPlayerTransactions.setString(1, uid);
            psGetPlayerTransactions.setString(2, uid);
            psGetPlayerTransactions.setString(3, currencyId);
            psGetPlayerTransactions.setInt(4, Math.max(1, limit));
            try (ResultSet rs = psGetPlayerTransactions.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(
                            rs.getString(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getDouble(4),
                            rs.getDouble(5),
                            rs.getString(6),
                            rs.getString(7),
                            rs.getTimestamp(8)
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("getPlayerTransactions error", e);
        }
        return list;
    }

    public Map<String, Double> getAllPlayerBalances(UUID playerUuid) {
        Map<String, Double> out = new HashMap<>();
        try {
            psGetAllPlayerBalances.clearParameters();
            psGetAllPlayerBalances.setString(1, playerUuid.toString());
            try (ResultSet rs = psGetAllPlayerBalances.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getDouble(2));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("getAllPlayerBalances error", e);
        }
        return out;
    }

    public List<RankingEntry> getTopPlayers(String currencyId, int limit) {
        List<RankingEntry> list = new ArrayList<>();
        try {
            psGetTopPlayers.clearParameters();
            psGetTopPlayers.setString(1, currencyId);
            psGetTopPlayers.setInt(2, Math.max(1, limit));
            try (ResultSet rs = psGetTopPlayers.executeQuery()) {
                while (rs.next()) {
                    list.add(new RankingEntry(
                            rs.getString(1),
                            rs.getString(2),
                            rs.getDouble(3),
                            rs.getInt(4)
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("getTopPlayers error", e);
        }
        return list;
    }

    public int getPlayerPosition(UUID playerUuid, String currencyId) {
        try {
            psGetPlayerPosition.clearParameters();
            psGetPlayerPosition.setString(1, currencyId);
            psGetPlayerPosition.setString(2, playerUuid.toString());
            try (ResultSet rs = psGetPlayerPosition.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("getPlayerPosition error", e);
        }
        return -1;
    }

    public String getTopPlayerUuid(String currencyId) {
        try {
            psGetTopPlayerUuid.clearParameters();
            psGetTopPlayerUuid.setString(1, currencyId);
            try (ResultSet rs = psGetTopPlayerUuid.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.error("getTopPlayerUuid error", e);
        }
        return null;
    }

    public RankingEntry getTopPlayerInfo(String currencyId) {
        try {
            psGetTopPlayerInfo.clearParameters();
            psGetTopPlayerInfo.setString(1, currencyId);
            try (ResultSet rs = psGetTopPlayerInfo.executeQuery()) {
                if (rs.next()) {
                    String uuid = rs.getString(1);
                    String username = rs.getString(2);
                    return new RankingEntry(uuid, username, 0.0, 1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("getTopPlayerInfo error", e);
        }
        return null;
    }

    public void updateRankingCache(String currencyId) {
        try {
            conn.setAutoCommit(false);
            try {
                psDeleteRankingCache.clearParameters();
                psDeleteRankingCache.setString(1, currencyId);
                psDeleteRankingCache.executeUpdate();

                psInsertRankingCache.clearParameters();
                psInsertRankingCache.setString(1, currencyId);
                psInsertRankingCache.setString(2, currencyId);
                psInsertRankingCache.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.error("updateRankingCache error", e);
        }
    }

    public String getLastTycoonUuid(String currencyId) {
        try {
            psGetLastTycoon.clearParameters();
            psGetLastTycoon.setString(1, currencyId);
            try (ResultSet rs = psGetLastTycoon.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.error("getLastTycoonUuid error", e);
        }
        return null;
    }

    public TycoonStateRecord getLastTycoonInfo(String currencyId) {
        try {
            psGetLastTycoonInfo.clearParameters();
            psGetLastTycoonInfo.setString(1, currencyId);
            try (ResultSet rs = psGetLastTycoonInfo.executeQuery()) {
                if (rs.next()) {
                    return new TycoonStateRecord(rs.getString(1), rs.getString(2));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("getLastTycoonInfo error", e);
        }
        return null;
    }

    public boolean upsertLastTycoon(String currencyId, String tycoonUuid, String tycoonUsername) {
        try {
            psUpsertLastTycoon.clearParameters();
            psUpsertLastTycoon.setString(1, currencyId);
            psUpsertLastTycoon.setString(2, tycoonUuid);
            psUpsertLastTycoon.setString(3, tycoonUsername);
            return psUpsertLastTycoon.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("upsertLastTycoon error", e);
            return false;
        }
    }

    public int pruneOldTransactions(double retentionDays) {
        try {
            psDeleteOldTransactionsDays.clearParameters();
            psDeleteOldTransactionsDays.setDouble(1, Math.max(0.0, retentionDays));
            int rows = psDeleteOldTransactionsDays.executeUpdate();
            LOGGER.debug("Transactions pruning: {} rows older than {} days removed", rows, retentionDays);
            return rows;
        } catch (SQLException e) {
            LOGGER.error("pruneOldTransactions error", e);
            return 0;
        }
    }

    public boolean walCheckpointTruncate() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA wal_checkpoint(TRUNCATE)")) {
            LOGGER.debug("WAL checkpoint (TRUNCATE) executed");
            return true;
        } catch (SQLException e) {
            LOGGER.error("walCheckpointTruncate error", e);
            return false;
        }
    }

    public boolean analyze() {
        try (Statement st = conn.createStatement()) {
            st.execute("ANALYZE");
            LOGGER.debug("ANALYZE executed");
            return true;
        } catch (SQLException e) {
            LOGGER.error("ANALYZE error", e);
            return false;
        }
    }

    public boolean vacuum() {
        try {
            conn.setAutoCommit(true);
            try (Statement st = conn.createStatement()) {
                st.execute("VACUUM");
                LOGGER.debug("VACUUM executed");
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("VACUUM error", e);
            return false;
        }
    }

    public PayTxResult payAtomic(UUID senderUuid, UUID receiverUuid, String currencyId, double amount, double fee) {
        try {
            conn.setAutoCommit(false);
            try {
                if (!isPaymentEnabled(receiverUuid, currencyId)) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return new PayTxResult(PayTxResult.Status.RECEIVER_BLOCKED);
                }

                if (!hasAccount(senderUuid, currencyId)) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return new PayTxResult(PayTxResult.Status.SENDER_NOT_FOUND);
                }
                if (!hasAccount(receiverUuid, currencyId)) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return new PayTxResult(PayTxResult.Status.RECEIVER_NOT_FOUND);
                }

                double totalDebit = amount + Math.max(0.0, fee);
                double senderBal = getBalance(senderUuid, currencyId);
                if (senderBal < totalDebit) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return new PayTxResult(PayTxResult.Status.INSUFFICIENT_FUNDS);
                }

                if (!subtractBalance(senderUuid, currencyId, totalDebit)) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return new PayTxResult(PayTxResult.Status.INSUFFICIENT_FUNDS);
                }
                if (!addBalance(receiverUuid, currencyId, amount)) {
                    addBalance(senderUuid, currencyId, totalDebit);
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return new PayTxResult(PayTxResult.Status.ERROR);
                }

                recordTransaction(currencyId, senderUuid, receiverUuid, amount, fee, "PAY", "Player payment");

                conn.commit();
                conn.setAutoCommit(true);
                return new PayTxResult(PayTxResult.Status.OK);
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                LOGGER.error("payAtomic SQL error", ex);
                return new PayTxResult(PayTxResult.Status.ERROR);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
            }
        } catch (SQLException e) {
            LOGGER.error("payAtomic (autocommit) error", e);
            return new PayTxResult(PayTxResult.Status.ERROR);
        }
    }


    public void close() {
        List<AutoCloseable> closables = Arrays.asList(
                psHasAccount, psCreateAccount, psGetBalance, psSetBalance, psAddBalance,
                psSubBalanceNoCheck, psResetBalance, psIsPaymentEnabled, psSetPaymentEnabled,
                psRecordTransaction, psGetPlayerTransactions, psGetAllPlayerBalances,
                psGetTopPlayers, psGetPlayerPosition, psGetTopPlayerUuid, psGetTopPlayerInfo,
                psDeleteRankingCache, psInsertRankingCache, psDeleteOldTransactionsDays,
                psGetLastTycoon, psGetLastTycoonInfo, psUpsertLastTycoon
        );
        for (AutoCloseable c : closables) {
            if (c != null) {
                try { c.close(); } catch (Exception e) { /* ignore */ }
            }
        }
        try { conn.close(); } catch (SQLException e) { LOGGER.warn("Error closing connection", e); }
    }
}