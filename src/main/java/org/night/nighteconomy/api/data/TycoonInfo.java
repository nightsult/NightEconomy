package org.night.nighteconomy.api.data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Informações sobre o Tycoon atual de uma moeda.
 */
public record TycoonInfo(
        UUID playerId,
        String playerName,
        BigDecimal balance,
        String tag
) { }