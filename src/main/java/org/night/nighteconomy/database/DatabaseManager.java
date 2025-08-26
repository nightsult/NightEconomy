package org.night.nighteconomy.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DATABASE_NAME = "economy.db";
    private Connection connection;
    
    public DatabaseManager() {
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try {
            // Criar diretório se não existir
            File dbDir = new File("config/economymod");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            // Conectar ao banco de dados
            String url = "jdbc:sqlite:config/economymod/" + DATABASE_NAME;
            connection = DriverManager.getConnection(url);
            
            // Criar tabelas
            createTables();
            
            LOGGER.info("Banco de dados inicializado com sucesso!");
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao inicializar banco de dados: ", e);
        }
    }
    
    private void createTables() throws SQLException {
        String createAccountsTable = """
            CREATE TABLE IF NOT EXISTS accounts (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                balance REAL NOT NULL DEFAULT 0.0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                from_uuid TEXT,
                to_uuid TEXT,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                description TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (from_uuid) REFERENCES accounts(uuid),
                FOREIGN KEY (to_uuid) REFERENCES accounts(uuid)
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createAccountsTable);
            stmt.execute(createTransactionsTable);
            LOGGER.info("Tabelas criadas com sucesso!");
        }
    }
    
    public boolean createAccount(UUID playerUuid, String username) {
        String sql = "INSERT OR IGNORE INTO accounts (uuid, username, balance) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, username);
            pstmt.setDouble(3, 0.0);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar conta para " + username + ": ", e);
            return false;
        }
    }
    
    public double getBalance(UUID playerUuid) {
        String sql = "SELECT balance FROM accounts WHERE uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao obter saldo para " + playerUuid + ": ", e);
        }
        
        return 0.0;
    }
    
    public boolean setBalance(UUID playerUuid, double amount) {
        String sql = "UPDATE accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, playerUuid.toString());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao definir saldo para " + playerUuid + ": ", e);
            return false;
        }
    }
    
    public boolean addBalance(UUID playerUuid, double amount) {
        double currentBalance = getBalance(playerUuid);
        return setBalance(playerUuid, currentBalance + amount);
    }
    
    public boolean subtractBalance(UUID playerUuid, double amount) {
        double currentBalance = getBalance(playerUuid);
        if (currentBalance >= amount) {
            return setBalance(playerUuid, currentBalance - amount);
        }
        return false;
    }
    
    public boolean recordTransaction(UUID fromUuid, UUID toUuid, double amount, String type, String description) {
        String sql = "INSERT INTO transactions (from_uuid, to_uuid, amount, type, description) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, fromUuid != null ? fromUuid.toString() : null);
            pstmt.setString(2, toUuid != null ? toUuid.toString() : null);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, type);
            pstmt.setString(5, description);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.error("Erro ao registrar transação: ", e);
            return false;
        }
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
}

