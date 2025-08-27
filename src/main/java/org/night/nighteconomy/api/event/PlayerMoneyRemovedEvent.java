package org.night.nighteconomy.api.event;

import net.neoforged.bus.api.Event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento emitido quando dinheiro é removido do jogador via comando "remove".
 */
public final class PlayerMoneyRemovedEvent extends Event {
    private final String currencyId;
    private final UUID playerId;
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String commandName;

    /**
     * @param currencyId  ID da moeda
     * @param playerId    jogador afetado
     * @param amount      quantia removida (positiva)
     * @param newBalance  novo saldo resultante
     * @param commandName nome do comando que originou (ex.: "remove")
     */
    public PlayerMoneyRemovedEvent(String currencyId, UUID playerId, BigDecimal amount, BigDecimal newBalance, String commandName) {
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