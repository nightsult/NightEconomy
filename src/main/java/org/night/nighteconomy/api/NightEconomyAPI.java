package org.night.nighteconomy.api;

import org.night.nighteconomy.api.data.RankEntry;
import org.night.nighteconomy.api.data.TycoonInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * API pública somente-leitura do NightEconomy.
 * Fornece acesso a moedas, saldos, tycoon e ranking.
 */
public interface NightEconomyAPI {

    /**
     * Identificadores das moedas disponíveis.
     *
     * @return conjunto de IDs de moeda (ex.: "coins", "gems")
     */
    Set<String> getAvailableCurrencies();

    /**
     * Saldo atual do jogador em uma moeda específica.
     *
     * @param playerId   UUID do jogador
     * @param currencyId ID da moeda
     * @return saldo (nunca nulo). Se o jogador ainda não tem registro, retorna 0.
     * @throws IllegalArgumentException se a moeda não existir
     */
    BigDecimal getBalance(UUID playerId, String currencyId);

    /**
     * Informações do Tycoon atual de uma moeda (se houver).
     *
     * @param currencyId ID da moeda
     * @return TycoonInfo ou null se não houver tycoon
     * @throws IllegalArgumentException se a moeda não existir
     */
    TycoonInfo getCurrentTycoon(String currencyId);

    /**
     * Tag configurada do Tycoon para a moeda (por exemplo: {@code "&amp;a[$]"}).
     *
     * @param currencyId ID da moeda
     * @return tag do tycoon (pode ser null ou vazio se não configurada)
     * @throws IllegalArgumentException se a moeda não existir
     */
    String getTycoonTag(String currencyId);

    /**
     * Ranking da moeda, limitado a {@code limit} entradas.
     *
     * @param currencyId ID da moeda
     * @param limit      máximo de entradas (ex.: 10 para top-10)
     * @return lista ordenada por saldo desc, com posição iniciando em 1
     * @throws IllegalArgumentException se a moeda não existir
     */
    List<RankEntry> getTopRanking(String currencyId, int limit);
}