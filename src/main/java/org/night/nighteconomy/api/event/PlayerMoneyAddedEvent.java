package org.night.nighteconomy.api.event;

import net.neoforged.bus.api.Event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event emitted when money is added to the player via the "add" command.
 */
public final class PlayerMoneyAddedEvent extends Event {
    private final String currencyId;
    private final UUID playerId;
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String commandName;

    /**
     * @param currencyId  Currency ID
     * @param playerId    affected player
     * @param amount      added (positive)
     * @param newBalance  new resulting balance
     * @param commandName name of the command that originated (e.g., "add")
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