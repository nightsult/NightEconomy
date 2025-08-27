package org.night.nighteconomy.api.data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entrada de ranking para uma moeda específica.
 */
public record RankEntry(
        int position,
        UUID playerId,
        String playerName,
        BigDecimal balance
) { }