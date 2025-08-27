package org.night.nighteconomy.api.data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Rating input for a specific currency.
 */
public record RankEntry(
        int position,
        UUID playerId,
        String playerName,
        BigDecimal balance
) { }