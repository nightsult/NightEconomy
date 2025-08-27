package org.night.nighteconomy.api;

import java.util.UUID;

/**
 * Interface para listeners de eventos de economia
 * 
 * Permite que outros mods sejam notificados sobre mudanças no sistema de economia,
 * como transações, mudanças de saldo, alterações de ranking, etc.
 * 
 * @author Night
 * @version 3.1.0
 * @since 3.1.0
 */
public interface EconomyEventListener {
    
    /**
     * Chamado quando o saldo de um jogador é alterado
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param oldBalance Saldo anterior
     * @param newBalance Novo saldo
     * @param reason Motivo da alteração (SET, ADD, REMOVE, PAY, etc.)
     */
    default void onBalanceChange(UUID playerUuid, String currencyId, double oldBalance, double newBalance, String reason) {}
    
    /**
     * Chamado quando uma transação é realizada
     * 
     * @param fromUuid UUID do remetente (pode ser null para transações do sistema)
     * @param toUuid UUID do destinatário (pode ser null para transações do sistema)
     * @param currencyId ID da moeda
     * @param amount Quantia da transação
     * @param fee Taxa da transação
     * @param type Tipo da transação
     * @param description Descrição da transação
     */
    default void onTransaction(UUID fromUuid, UUID toUuid, String currencyId, double amount, double fee, String type, String description) {}
    
    /**
     * Chamado quando um pagamento é realizado entre jogadores
     * 
     * @param fromUuid UUID do remetente
     * @param toUuid UUID do destinatário
     * @param currencyId ID da moeda
     * @param amount Quantia transferida
     * @param fee Taxa da transação
     * @param success true se o pagamento foi bem-sucedido, false caso contrário
     */
    default void onPayment(UUID fromUuid, UUID toUuid, String currencyId, double amount, double fee, boolean success) {}
    
    /**
     * Chamado quando uma conta é criada para um jogador
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param username Nome do jogador
     * @param initialBalance Saldo inicial da conta
     */
    default void onAccountCreated(UUID playerUuid, String currencyId, String username, double initialBalance) {}
    
    /**
     * Chamado quando o ranking de uma moeda é atualizado
     * 
     * @param currencyId ID da moeda
     * @param topPlayerUuid UUID do jogador que está no topo (pode ser null se não houver jogadores)
     * @param totalPlayers Número total de jogadores no ranking
     */
    default void onRankingUpdated(String currencyId, UUID topPlayerUuid, int totalPlayers) {}
    
    /**
     * Chamado quando um jogador se torna o magnata (top 1) de uma moeda
     * 
     * @param playerUuid UUID do novo magnata
     * @param currencyId ID da moeda
     * @param previousMagnataUuid UUID do magnata anterior (pode ser null se não havia magnata)
     */
    default void onMagnataChanged(UUID playerUuid, String currencyId, UUID previousMagnataUuid) {}
    
    /**
     * Chamado quando as configurações de pagamento de um jogador são alteradas
     * 
     * @param playerUuid UUID do jogador
     * @param currencyId ID da moeda
     * @param enabled true se os pagamentos foram habilitados, false se foram desabilitados
     */
    default void onPaymentSettingChanged(UUID playerUuid, String currencyId, boolean enabled) {}
    
    /**
     * Chamado quando uma moeda é recarregada
     * 
     * @param currencyId ID da moeda recarregada
     */
    default void onCurrencyReloaded(String currencyId) {}
    
    /**
     * Chamado quando todas as configurações são recarregadas
     */
    default void onConfigurationsReloaded() {}
    
    /**
     * Chamado quando uma nova moeda é adicionada ao sistema
     * 
     * @param currencyId ID da nova moeda
     * @param currencyName Nome da nova moeda
     */
    default void onCurrencyAdded(String currencyId, String currencyName) {}
    
    /**
     * Chamado quando uma moeda é removida do sistema
     * 
     * @param currencyId ID da moeda removida
     * @param currencyName Nome da moeda removida
     */
    default void onCurrencyRemoved(String currencyId, String currencyName) {}
}

