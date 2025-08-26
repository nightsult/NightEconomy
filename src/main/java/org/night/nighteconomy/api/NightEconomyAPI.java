package org.night.nighteconomy.api;

import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.RankingEntry;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.Transaction;
import org.night.nighteconomy.service.MultiCurrencyEconomyService.PaymentResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * API pública do NightEconomy para integração com outros mods
 * 
 * Esta interface fornece acesso completo ao sistema de economia multi-moeda,
 * permitindo que outros mods interajam com contas, transações, rankings e configurações.
 * 
 * @author NightEconomy Team
 * @version 2.0.0
 * @since 1.0.0
 */
public interface NightEconomyAPI {
    
    // ========== CURRENCY MANAGEMENT ==========
    
    /**
     * Obtém todas as moedas disponíveis no sistema
     * 
     * @return Set contendo os IDs de todas as moedas configuradas
     */
    Set<String> getAvailableCurrencies();
    
    /**
     * Verifica se uma moeda existe no sistema
     * 
     * @param currencyId ID da moeda a verificar
     * @return true se a moeda existir, false caso contrário
     */
    boolean currencyExists(String currencyId);
    
    /**
     * Obtém a configuração de uma moeda específica
     * 
     * @param currencyId ID da moeda
     * @return Configuração da moeda ou null se não existir
     */
    CurrencyConfig getCurrencyConfig(String currencyId);
    
    /**
     * Obtém o nome de exibição de uma moeda
     * 
     * @param currencyId ID da moeda
     * @return Nome de exibição da moeda ou null se não existir
     */
    String getCurrencyName(String currencyId);
    
    /**
     * Obtém o valor padrão de uma moeda para novos jogadores
     * 
     * @param currencyId ID da moeda
     * @return Valor padrão da moeda
     */
    double getCurrencyDefaultValue(String currencyId);
    
    // ========== ACCOUNT MANAGEMENT ==========
    
    /**
     * Cria uma conta para um jogador em uma moeda específica
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param username Nome do jogador
     * @return true se a conta foi criada com sucesso, false caso contrário
     */
    boolean createAccount(UUID playerUuid, String currencyId, String username);
    
    /**
     * Verifica se um jogador possui uma conta em uma moeda específica
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @return true se a conta existir, false caso contrário
     */
    boolean hasAccount(UUID playerUuid, String currencyId);
    
    /**
     * Garante que uma conta existe para um jogador em uma moeda específica
     * Cria a conta se ela não existir
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param username Nome do jogador
     */
    void ensureAccountExists(UUID playerUuid, String currencyId, String username);
    
    // ========== BALANCE OPERATIONS ==========
    
    /**
     * Obtém o saldo de um jogador em uma moeda específica
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @return Saldo do jogador na moeda especificada
     */
    double getBalance(UUID playerUuid, String currencyId);
    
    /**
     * Define o saldo de um jogador em uma moeda específica
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param amount Novo saldo
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean setBalance(UUID playerUuid, String currencyId, double amount);
    
    /**
     * Adiciona uma quantia ao saldo de um jogador
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param amount Quantia a adicionar (deve ser positiva)
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean addBalance(UUID playerUuid, String currencyId, double amount);
    
    /**
     * Remove uma quantia do saldo de um jogador
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param amount Quantia a remover (deve ser positiva)
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean subtractBalance(UUID playerUuid, String currencyId, double amount);
    
    /**
     * Verifica se um jogador tem saldo suficiente
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param amount Quantia a verificar
     * @return true se o jogador tem saldo suficiente, false caso contrário
     */
    boolean hasBalance(UUID playerUuid, String currencyId, double amount);
    
    /**
     * Reseta o saldo de um jogador para o valor padrão da moeda
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean resetBalance(UUID playerUuid, String currencyId);
    
    /**
     * Obtém todos os saldos de um jogador em todas as moedas
     * 
     * @param playerUuid UUID do jogador
     * @return Map com currencyId como chave e saldo como valor
     */
    Map<String, Double> getAllPlayerBalances(UUID playerUuid);
    
    // ========== PAYMENT SYSTEM ==========
    
    /**
     * Realiza um pagamento entre dois jogadores
     * 
     * @param fromUuid UUID do remetente
     * @param toUuid UUID do destinatário
     * @param currencyId ID da moeda
     * @param amount Quantia a transferir
     * @return Resultado da operação de pagamento
     */
    PaymentResult payPlayer(UUID fromUuid, UUID toUuid, String currencyId, double amount);
    
    /**
     * Verifica se um jogador aceita pagamentos em uma moeda específica
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @return true se aceita pagamentos, false caso contrário
     */
    boolean isPaymentEnabled(UUID playerUuid, String currencyId);
    
    /**
     * Define se um jogador aceita pagamentos em uma moeda específica
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param enabled true para aceitar pagamentos, false para recusar
     * @return true se a configuração foi alterada com sucesso, false caso contrário
     */
    boolean setPaymentEnabled(UUID playerUuid, String currencyId, boolean enabled);
    
    /**
     * Calcula a taxa de transação para um pagamento
     * 
     * @param currencyId ID da moeda
     * @param amount Quantia do pagamento
     * @return Valor da taxa de transação
     */
    double calculateTransactionFee(String currencyId, double amount);
    
    // ========== TRANSACTION HISTORY ==========
    
    /**
     * Obtém o histórico de transações de um jogador
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param limit Número máximo de transações a retornar (máximo 50)
     * @return Lista de transações do jogador
     */
    List<Transaction> getPlayerTransactions(UUID playerUuid, String currencyId, int limit);
    
    /**
     * Registra uma transação externa no sistema
     * Útil para outros mods que querem registrar suas próprias transações
     * 
     * @param currencyId ID da moeda
     * @param fromUuid UUID do remetente (pode ser null para transações do sistema)
     * @param toUuid UUID do destinatário (pode ser null para transações do sistema)
     * @param amount Quantia da transação
     * @param type Tipo da transação
     * @param description Descrição da transação
     * @return true se a transação foi registrada com sucesso, false caso contrário
     */
    boolean recordExternalTransaction(String currencyId, UUID fromUuid, UUID toUuid, 
                                    double amount, String type, String description);
    
    // ========== RANKING SYSTEM ==========
    
    /**
     * Verifica se o ranking está habilitado para uma moeda
     * 
     * @param currencyId ID da moeda
     * @return true se o ranking estiver habilitado, false caso contrário
     */
    boolean isRankingEnabled(String currencyId);
    
    /**
     * Obtém o ranking dos melhores jogadores em uma moeda
     * 
     * @param currencyId ID da moeda
     * @param limit Número máximo de jogadores a retornar (máximo 100)
     * @return Lista de entradas do ranking ordenada por posição
     */
    List<RankingEntry> getTopPlayers(String currencyId, int limit);
    
    /**
     * Obtém a posição de um jogador no ranking
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @return Posição do jogador no ranking ou -1 se não estiver no ranking
     */
    int getPlayerPosition(UUID playerUuid, String currencyId);
    
    /**
     * Verifica se um jogador é o magnata (top 1) de uma moeda
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @return true se o jogador for o magnata, false caso contrário
     */
    boolean isPlayerMagnata(UUID playerUuid, String currencyId);
    
    /**
     * Obtém a tag de magnata de uma moeda
     * 
     * @param currencyId ID da moeda
     * @return Tag de magnata configurada para a moeda
     */
    String getMagnataTag(String currencyId);
    
    /**
     * Força uma atualização do ranking de uma moeda
     * 
     * @param currencyId ID da moeda
     */
    void forceRankingUpdate(String currencyId);
    
    // ========== FORMATTING ==========
    
    /**
     * Formata uma quantia de acordo com as configurações da moeda
     * 
     * @param currencyId ID da moeda
     * @param amount Quantia a formatar
     * @return Quantia formatada como string
     */
    String formatAmount(String currencyId, double amount);
    
    /**
     * Formata uma quantia sem usar abreviações (K, M, B, etc.)
     * 
     * @param currencyId ID da moeda
     * @param amount Quantia a formatar
     * @return Quantia formatada como string sem abreviações
     */
    String formatAmountRaw(String currencyId, double amount);
    
    // ========== PLACEHOLDERS ==========
    
    /**
     * Processa um placeholder específico para um jogador
     * 
     * @param playerUuid UUID do jogador
     * @param placeholder Placeholder a processar (ex: "nighteconomy_money_balance")
     * @return Valor do placeholder ou o placeholder original se não for reconhecido
     */
    String processPlaceholder(UUID playerUuid, String placeholder);
    
    /**
     * Processa múltiplos placeholders em um texto
     * 
     * @param playerUuid UUID do jogador
     * @param text Texto contendo placeholders
     * @return Texto com placeholders substituídos pelos valores correspondentes
     */
    String processPlaceholders(UUID playerUuid, String text);
    
    /**
     * Obtém todos os placeholders disponíveis para um jogador
     * 
     * @param playerUuid UUID do jogador
     * @return Map com placeholder como chave e valor como valor
     */
    Map<String, String> getAllPlaceholders(UUID playerUuid);
    
    /**
     * Obtém a lista de placeholders disponíveis
     * 
     * @return Array com todos os placeholders disponíveis
     */
    String[] getAvailablePlaceholders();
    
    // ========== CONFIGURATION ==========
    
    /**
     * Recarrega todas as configurações do mod
     */
    void reloadConfigurations();
    
    /**
     * Recarrega a configuração de uma moeda específica
     * 
     * @param currencyId ID da moeda a recarregar
     */
    void reloadCurrency(String currencyId);
    
    /**
     * Obtém uma mensagem configurada
     * 
     * @param currencyId ID da moeda (pode ser null para mensagens globais)
     * @param messageKey Chave da mensagem
     * @return Mensagem configurada ou a chave se não encontrada
     */
    String getMessage(String currencyId, String messageKey);
    
    /**
     * Obtém uma mensagem global configurada
     * 
     * @param messageKey Chave da mensagem
     * @return Mensagem configurada ou a chave se não encontrada
     */
    String getGlobalMessage(String messageKey);
    
    // ========== EVENTS ==========
    
    /**
     * Registra um listener para eventos de economia
     * 
     * @param listener Listener a registrar
     */
    void registerEconomyListener(EconomyEventListener listener);
    
    /**
     * Remove um listener de eventos de economia
     * 
     * @param listener Listener a remover
     */
    void unregisterEconomyListener(EconomyEventListener listener);
    
    // ========== UTILITY ==========
    
    /**
     * Obtém a versão da API
     * 
     * @return Versão da API
     */
    String getAPIVersion();
    
    /**
     * Verifica se o LuckPerms está disponível
     * 
     * @return true se o LuckPerms estiver disponível, false caso contrário
     */
    boolean isLuckPermsAvailable();
    
    /**
     * Obtém estatísticas de uma moeda
     * 
     * @param currencyId ID da moeda
     * @return Map com estatísticas da moeda
     */
    Map<String, Object> getCurrencyStatistics(String currencyId);
}

