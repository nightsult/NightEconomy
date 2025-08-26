package org.night.nighteconomy.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class MultiCurrencyDatabaseManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DATABASE_NAME = "nighteconomy.db";
    private Connection connection;
    private final Path dbPath;

    public MultiCurrencyDatabaseManager(Path dbPath) {
        this.dbPath = dbPath;
        initializeDatabase();
    }

    public void initializeTables() {
        try {
            createTables();
        } catch (SQLException e) {
            LOGGER.error("Erro ao assegurar/criar tabelas: ", e);
        }
    }


    private void initializeDatabase() {
        try {
            // cria diretório do DB
            Files.createDirectories(dbPath.getParent());

            // conecta no SQLite
            String url = "jdbc:sqlite:" + dbPath.toString();
            connection = DriverManager.getConnection(url);

            // PRAGMAs úteis (opcional, melhora performance)
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA journal_mode=WAL;");
                pragma.execute("PRAGMA synchronous=NORMAL;");
                pragma.execute("PRAGMA temp_store=MEMORY;");
                pragma.execute("PRAGMA foreign_keys=ON;");
            }

            // cria tabelas
            createTables();

            LOGGER.info("Banco de dados multi-moeda inicializado em {}", dbPath);
        } catch (SQLException e) {
            LOGGER.error("Erro ao inicializar banco de dados: ", e);
        } catch (Exception e) {
            LOGGER.error("Falha de I/O ao preparar diretório do banco: ", e);
        }
    }

    // Account management
    public boolean createAccount(UUID playerUuid, String currencyId, String username, double defaultBalance) {
        String sql = "INSERT OR IGNORE INTO accounts (uuid, currency_id, username, balance) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, currencyId);
            pstmt.setString(3, username);
            pstmt.setDouble(4, defaultBalance);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar conta para " + username + " na moeda " + currencyId + ": ", e);
            return false;
        }
    }
    
    public double getBalance(UUID playerUuid, String currencyId) {
        String sql = "SELECT balance FROM accounts WHERE uuid = ? AND currency_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, currencyId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao obter saldo para " + playerUuid + " na moeda " + currencyId + ": ", e);
        }
        
        return 0.0;
    }
    
    public boolean setBalance(UUID playerUuid, String currencyId, double amount) {
        String sql = "UPDATE accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ? AND currency_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, currencyId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao definir saldo para " + playerUuid + " na moeda " + currencyId + ": ", e);
            return false;
        }
    }
    
    public boolean addBalance(UUID playerUuid, String currencyId, double amount) {
        double currentBalance = getBalance(playerUuid, currencyId);
        return setBalance(playerUuid, currencyId, currentBalance + amount);
    }
    
    public boolean subtractBalance(UUID playerUuid, String currencyId, double amount) {
        double currentBalance = getBalance(playerUuid, currencyId);
        if (currentBalance >= amount) {
            return setBalance(playerUuid, currencyId, currentBalance - amount);
        }
        return false;
    }
    
    // Payment settings
    public boolean isPaymentEnabled(UUID playerUuid, String currencyId) {
        String sql = "SELECT payment_enabled FROM accounts WHERE uuid = ? AND currency_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, currencyId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("payment_enabled");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao verificar configuração de pagamento: ", e);
        }
        
        return true; // Default to enabled
    }
    
    public boolean setPaymentEnabled(UUID playerUuid, String currencyId, boolean enabled) {
        String sql = "UPDATE accounts SET payment_enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ? AND currency_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, enabled);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, currencyId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao definir configuração de pagamento: ", e);
            return false;
        }
    }
    
    // Transaction management
    public boolean recordTransaction(String currencyId, UUID fromUuid, UUID toUuid, double amount, double fee, String type, String description) {
        String sql = "INSERT INTO transactions (currency_id, from_uuid, to_uuid, amount, fee, type, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currencyId);
            pstmt.setString(2, fromUuid != null ? fromUuid.toString() : null);
            pstmt.setString(3, toUuid != null ? toUuid.toString() : null);
            pstmt.setDouble(4, amount);
            pstmt.setDouble(5, fee);
            pstmt.setString(6, type);
            pstmt.setString(7, description);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao registrar transação: ", e);
            return false;
        }
    }
    
    public List<Transaction> getPlayerTransactions(UUID playerUuid, String currencyId, int limit) {
        String sql = """
            SELECT * FROM transactions 
            WHERE currency_id = ? AND (from_uuid = ? OR to_uuid = ?) 
            ORDER BY timestamp DESC 
            LIMIT ?
        """;
        
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currencyId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, playerUuid.toString());
            pstmt.setInt(4, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();
                    transaction.setId(rs.getLong("id"));
                    transaction.setCurrencyId(rs.getString("currency_id"));
                    transaction.setFromUuid(rs.getString("from_uuid"));
                    transaction.setToUuid(rs.getString("to_uuid"));
                    transaction.setAmount(rs.getDouble("amount"));
                    transaction.setFee(rs.getDouble("fee"));
                    transaction.setType(rs.getString("type"));
                    transaction.setDescription(rs.getString("description"));
                    transaction.setTimestamp(rs.getTimestamp("timestamp"));
                    transactions.add(transaction);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao obter transações do jogador: ", e);
        }
        
        return transactions;
    }

    // NOVO: verificação real de existência da conta
    public boolean hasAccount(UUID playerUuid, String currencyId) {
        String sql = "SELECT 1 FROM accounts WHERE uuid = ? AND currency_id = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, currencyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao verificar existência de conta: ", e);
            return false;
        }
    }

    // OTIMIZADO: atualização do cache de ranking em transação e com um único INSERT...SELECT
    public void updateRankingCache(String currencyId) {
        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            // Limpa cache antigo
            try (PreparedStatement clearStmt = connection.prepareStatement(
                    "DELETE FROM ranking_cache WHERE currency_id = ?")) {
                clearStmt.setString(1, currencyId);
                clearStmt.executeUpdate();
            }

            // Insere novo cache de uma vez usando window function
            // Nota: 'ROW_NUMBER() OVER (ORDER BY balance DESC)' requer SQLite 3.25+ (com suporte a window functions)
            final String insertSql = """
            INSERT INTO ranking_cache (currency_id, uuid, username, balance, position)
            SELECT ? AS currency_id, uuid, username, balance,
                   ROW_NUMBER() OVER (ORDER BY balance DESC) AS position
            FROM accounts
            WHERE currency_id = ? AND balance > 0
            ORDER BY balance DESC
            LIMIT 100
        """;
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setString(1, currencyId);
                insertStmt.setString(2, currencyId);
                insertStmt.executeUpdate();
            }

            connection.commit();
            LOGGER.debug("Cache de ranking atualizado (transação) para moeda: " + currencyId);
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignore) {}
            LOGGER.error("Erro ao atualizar cache de ranking (transação): ", e);
        } finally {
            try { connection.setAutoCommit(oldAutoCommit); } catch (SQLException ignore) {}
        }
    }

    // REFORÇADO: criação de índices após criar tabelas (chame dentro de createTables ou initializeTables)
    private void createIndices() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Para consultas de ranking por moeda (ordenando por balance para construir cache)
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_accounts_currency_balance ON accounts(currency_id, balance DESC)");

            // Para histórico de transações (consultas por moeda+from/ou to ordenadas por timestamp)
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_currency_from_time ON transactions(currency_id, from_uuid, timestamp DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_currency_to_time   ON transactions(currency_id, to_uuid, timestamp DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_currency_time      ON transactions(currency_id, timestamp DESC)");

            // Para leitura do ranking já cacheado por moeda e posição
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ranking_currency_pos  ON ranking_cache(currency_id, position)");
        }
    }

    // CHAME createIndices() no final do createTables()
    private void createTables() throws SQLException {
        String createAccountsTable = """
        CREATE TABLE IF NOT EXISTS accounts (
            uuid TEXT NOT NULL,
            currency_id TEXT NOT NULL,
            username TEXT NOT NULL,
            balance REAL NOT NULL DEFAULT 0.0,
            payment_enabled BOOLEAN NOT NULL DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (uuid, currency_id)
        )
    """;

        String createTransactionsTable = """
        CREATE TABLE IF NOT EXISTS transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            currency_id TEXT NOT NULL,
            from_uuid TEXT,
            to_uuid TEXT,
            amount REAL NOT NULL,
            fee REAL NOT NULL DEFAULT 0.0,
            type TEXT NOT NULL,
            description TEXT,
            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """;

        String createRankingCacheTable = """
        CREATE TABLE IF NOT EXISTS ranking_cache (
            currency_id TEXT NOT NULL,
            uuid TEXT NOT NULL,
            username TEXT NOT NULL,
            balance REAL NOT NULL,
            position INTEGER NOT NULL,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (currency_id, uuid)
        )
    """;

        String createPlayerSettingsTable = """
        CREATE TABLE IF NOT EXISTS player_settings (
            uuid TEXT PRIMARY KEY,
            currency_id TEXT NOT NULL,
            payment_enabled BOOLEAN NOT NULL DEFAULT 1,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createAccountsTable);
            stmt.execute(createTransactionsTable);
            stmt.execute(createRankingCacheTable);
            stmt.execute(createPlayerSettingsTable);
        }

        // Garantir índices
        createIndices();
    }
    
    public List<RankingEntry> getTopPlayers(String currencyId, int limit) {
        String sql = """
            SELECT uuid, username, balance, position 
            FROM ranking_cache 
            WHERE currency_id = ? 
            ORDER BY position ASC 
            LIMIT ?
        """;
        
        List<RankingEntry> ranking = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currencyId);
            pstmt.setInt(2, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    RankingEntry entry = new RankingEntry();
                    entry.setUuid(rs.getString("uuid"));
                    entry.setUsername(rs.getString("username"));
                    entry.setBalance(rs.getDouble("balance"));
                    entry.setPosition(rs.getInt("position"));
                    ranking.add(entry);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao obter ranking: ", e);
        }
        
        return ranking;
    }
    
    public int getPlayerPosition(UUID playerUuid, String currencyId) {
        String sql = "SELECT position FROM ranking_cache WHERE currency_id = ? AND uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currencyId);
            pstmt.setString(2, playerUuid.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("position");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao obter posição do jogador no ranking: ", e);
        }
        
        return -1; // Not in ranking
    }
    
    public String getTopPlayerUuid(String currencyId) {
        String sql = "SELECT uuid FROM ranking_cache WHERE currency_id = ? AND position = 1";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currencyId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao obter top 1 do ranking: ", e);
        }
        
        return null;
    }
    
    // Utility methods
    public boolean resetPlayerBalance(UUID playerUuid, String currencyId, double defaultBalance) {
        return setBalance(playerUuid, currencyId, defaultBalance);
    }
    
    public Map<String, Double> getAllPlayerBalances(UUID playerUuid) {
        String sql = "SELECT currency_id, balance FROM accounts WHERE uuid = ?";
        Map<String, Double> balances = new HashMap<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    balances.put(rs.getString("currency_id"), rs.getDouble("balance"));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao obter todos os saldos do jogador: ", e);
        }
        
        return balances;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Conexão com banco de dados fechada.");
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao fechar conexão com banco de dados: ", e);
        }
    }
    
    // Inner classes for data transfer
    public static class Transaction {
        private long id;
        private String currencyId;
        private String fromUuid;
        private String toUuid;
        private double amount;
        private double fee;
        private String type;
        private String description;
        private Timestamp timestamp;
        
        // Getters and setters
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        
        public String getCurrencyId() { return currencyId; }
        public void setCurrencyId(String currencyId) { this.currencyId = currencyId; }
        
        public String getFromUuid() { return fromUuid; }
        public void setFromUuid(String fromUuid) { this.fromUuid = fromUuid; }
        
        public String getToUuid() { return toUuid; }
        public void setToUuid(String toUuid) { this.toUuid = toUuid; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public double getFee() { return fee; }
        public void setFee(double fee) { this.fee = fee; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Timestamp getTimestamp() { return timestamp; }
        public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    }
    
    public static class RankingEntry {
        private String uuid;
        private String username;
        private double balance;
        private int position;
        
        // Getters and setters
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
        
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
    }
}

