package org.night.nighteconomy.service;

import org.night.nighteconomy.database.DatabaseManager;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class EconomyService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final DatabaseManager databaseManager;
    
    public EconomyService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    public void createPlayerAccount(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        String username = player.getName().getString();
        
        if (databaseManager.createAccount(playerUuid, username)) {
            LOGGER.info("Conta criada para jogador: " + username);
        }
    }
    
    public double getPlayerBalance(UUID playerUuid) {
        return databaseManager.getBalance(playerUuid);
    }
    
    public double getPlayerBalance(ServerPlayer player) {
        return getPlayerBalance(player.getUUID());
    }
    
    public boolean setPlayerBalance(UUID playerUuid, double amount) {
        if (amount < 0) {
            return false;
        }
        
        return databaseManager.setBalance(playerUuid, amount);
    }
    
    public boolean setPlayerBalance(ServerPlayer player, double amount) {
        return setPlayerBalance(player.getUUID(), amount);
    }
    
    public boolean addMoney(UUID playerUuid, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        boolean success = databaseManager.addBalance(playerUuid, amount);
        if (success) {
            databaseManager.recordTransaction(null, playerUuid, amount, "ADD", "Dinheiro adicionado por admin");
        }
        
        return success;
    }
    
    public boolean addMoney(ServerPlayer player, double amount) {
        return addMoney(player.getUUID(), amount);
    }
    
    public boolean removeMoney(UUID playerUuid, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        boolean success = databaseManager.subtractBalance(playerUuid, amount);
        if (success) {
            databaseManager.recordTransaction(playerUuid, null, amount, "REMOVE", "Dinheiro removido por admin");
        }
        
        return success;
    }
    
    public boolean removeMoney(ServerPlayer player, double amount) {
        return removeMoney(player.getUUID(), amount);
    }
    
    public boolean transferMoney(ServerPlayer from, ServerPlayer to, double amount) {
        return transferMoney(from.getUUID(), to.getUUID(), amount, from.getName().getString(), to.getName().getString());
    }
    
    public boolean transferMoney(UUID fromUuid, UUID toUuid, double amount, String fromName, String toName) {
        if (amount <= 0) {
            return false;
        }
        
        // Verificar se o remetente tem saldo suficiente
        double fromBalance = databaseManager.getBalance(fromUuid);
        if (fromBalance < amount) {
            return false;
        }
        
        // Realizar transferência
        boolean subtractSuccess = databaseManager.subtractBalance(fromUuid, amount);
        if (!subtractSuccess) {
            return false;
        }
        
        boolean addSuccess = databaseManager.addBalance(toUuid, amount);
        if (!addSuccess) {
            // Reverter subtração se a adição falhar
            databaseManager.addBalance(fromUuid, amount);
            return false;
        }
        
        // Registrar transação
        databaseManager.recordTransaction(fromUuid, toUuid, amount, "TRANSFER", 
            String.format("Transferência de %s para %s", fromName, toName));
        
        return true;
    }
    
    public boolean hasEnoughMoney(UUID playerUuid, double amount) {
        return databaseManager.getBalance(playerUuid) >= amount;
    }
    
    public boolean hasEnoughMoney(ServerPlayer player, double amount) {
        return hasEnoughMoney(player.getUUID(), amount);
    }
    
    public String formatMoney(double amount) {
        return String.format("$%.2f", amount);
    }
}

