package org.night.nighteconomy.api;

import org.night.nighteconomy.api.data.RankEntry;
import org.night.nighteconomy.api.data.TycoonInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * NightEconomy's public read-only API.
 * Provides access to currencies, balances, tycoon, and rankings.
 */
public interface NightEconomyAPI {

    /**
     * Identifiers of available currencies.
     *
     * @return set of currency IDs (e.g., "coins", "gems")
     */
    Set<String> getAvailableCurrencies();

    /**
     * The player's current balance in a specific currency.
     *
     * @param playerId Player UUID
     * @param currencyId Currency ID
     * @return balance (never null). If the player is not yet registered, returns 0.
     * @throws IllegalArgumentException if the currency does not exist
     */
    BigDecimal getBalance(UUID playerId, String currencyId);

    /**
     * Current Tycoon information for a currency (if any).
     *
     * @param currencyId Currency ID
     * @return TycoonInfo or null if no tycoon exists
     * @throws IllegalArgumentException if the currency does not exist
     */
    TycoonInfo getCurrentTycoon(String currencyId);

    /**
     * Configured Tycoon tag for the currency (for example: {@code "&amp;a[$]"}).
     *
     * @param currencyId Currency ID
     * @return tycoon tag (can be null or empty if not configured)
     * @throws IllegalArgumentException if the currency does not exist
     */
    String getTycoonTag(String currencyId);

    /**
     * Currency ranking, limited to {@code limit} entries.
     *
     * @param currencyId Currency ID
     * @param limit Maximum number of entries (e.g., 10 for top-10)
     * @return List sorted by balance desc, with position starting at 1
     * @throws IllegalArgumentException if the currency does not exist
     */
    List<RankEntry> getTopRanking(String currencyId, int limit);

    boolean tryDebit(UUID playerId, String currencyId, java.math.BigDecimal amount, String reason);

    String formatAmount(String currencyId, java.math.BigDecimal amount);
}