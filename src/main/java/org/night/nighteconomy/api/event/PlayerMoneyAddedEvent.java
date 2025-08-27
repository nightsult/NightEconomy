package org.night.nighteconomy.api.event;

import net.neoforged.bus.api.Event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento emitido quando dinheiro é adicionado ao jogador via comando "add".
 */
public final class PlayerMoneyAddedEvent extends Event {
    private final String currencyId;
    private final UUID playerId;
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String commandName;

    /**
     * @param currencyId  ID da moeda
     * @param playerId    jogador afetado
     * @param amount      quantia adicionada (positiva)
     * @param newBalance  novo saldo resultante
     * @param commandName nome do comando que originou (ex.: "add")
     */
    public PlayerMoneyAddedEvent(String currencyId, UUID playerId, BigDecimal amount, BigDecimal newBalance, String commandName) {
        this.currencyId = currencyId;
        this.playerId = playerId;
        this.amount = amount;
        this.newBalance = newBalance;
        this.commandName = commandName;
    }

    public String getCurrencyId() { return currencyId; }
    public UUID getPlayerId() { return playerId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getNewBalance() { return newBalance; }
    public String getCommandName() { return commandName; }
}