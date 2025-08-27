package org.night.nighteconomy.api.data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Information about the current Tycoon of a coin.
 */
public record TycoonInfo(
        UUID playerId,
        String playerName,
        BigDecimal balance,
        String tag
) { }