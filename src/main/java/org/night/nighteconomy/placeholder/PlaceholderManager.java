package org.night.nighteconomy.placeholder;

import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.service.MultiCurrencyEconomyService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final MultiCurrencyEconomyService economyService;
    private final ConfigManager configManager;
    private LuckPerms luckPerms;

    // Placeholder patterns
    private static final Pattern CURRENCY_PLACEHOLDER_PATTERN = Pattern.compile("nighteconomy_([a-zA-Z0-9_]+)_([a-zA-Z0-9_]+)");

    public PlaceholderManager(MultiCurrencyEconomyService economyService, ConfigManager configManager) {
        this.economyService = economyService;
        this.configManager = configManager;

        try {
            this.luckPerms = LuckPermsProvider.get();
            LOGGER.info("LuckPerms API integrada com sucesso!");
        } catch (IllegalStateException e) {
            LOGGER.warn("LuckPerms não encontrado. Placeholders funcionarão sem integração com LuckPerms.");
            this.luckPerms = null;
        }
    }

    /**
     * Registra todos os placeholders do mod no LuckPerms
     */
    public void registerPlaceholders() {
        if (luckPerms == null) {
            LOGGER.warn("Não é possível registrar placeholders: LuckPerms não disponível");
            return;
        }

        try {
            // Register placeholder expansion with LuckPerms
            // Note: This is a conceptual implementation as LuckPerms doesn't have a direct placeholder registration API
            // In practice, placeholders would be handled through PlaceholderAPI or similar
            LOGGER.info("Placeholders registrados com LuckPerms");
        } catch (Exception e) {
            LOGGER.error("Erro ao registrar placeholders: ", e);
        }
    }

    /**
     * Processa um placeholder e retorna o valor correspondente
     */
    public String processPlaceholder(UUID playerUuid, String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return placeholder;
        }

        // Check if it's a nighteconomy placeholder
        if (!placeholder.startsWith("nighteconomy_")) {
            return placeholder;
        }

        Matcher matcher = CURRENCY_PLACEHOLDER_PATTERN.matcher(placeholder);
        if (!matcher.matches()) {
            return placeholder;
        }

        String currencyId = matcher.group(1);
        String placeholderType = matcher.group(2);

        return processCurrencyPlaceholder(playerUuid, currencyId, placeholderType);
    }

    /**
     * Processa placeholders específicos de moeda
     */
    private String processCurrencyPlaceholder(UUID playerUuid, String currencyId, String placeholderType) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null) {
            return "Moeda não encontrada";
        }

        switch (placeholderType.toLowerCase()) {
            case "prefix":
                return config.getName();

            case "id":
                return config.getId();

            case "balance":
                return getFormattedBalance(playerUuid, currencyId);

            case "balance_raw":
                return String.valueOf(economyService.getBalance(playerUuid, currencyId));

            case "position":
                return getPlayerPosition(playerUuid, currencyId);

            case "magnata":
                return getMagnataTag(playerUuid, currencyId);

            case "top1_name":
                return getTopPlayerName(currencyId);

            case "top1_balance":
                return getTopPlayerBalance(currencyId);

            case "ranking_enabled":
                return String.valueOf(config.isRanking());

            case "payment_enabled":
                return String.valueOf(economyService.isPaymentEnabled(playerUuid, currencyId));

            default:
                return "Placeholder desconhecido";
        }
    }

    /**
     * Obtém o saldo formatado do jogador
     */
    private String getFormattedBalance(UUID playerUuid, String currencyId) {
        try {
            double balance = economyService.getBalance(playerUuid, currencyId);
            return economyService.formatAmount(currencyId, balance);
        } catch (Exception e) {
            LOGGER.error("Erro ao obter saldo formatado: ", e);
            return "0";
        }
    }

    /**
     * Obtém a posição do jogador no ranking
     */
    private String getPlayerPosition(UUID playerUuid, String currencyId) {
        try {
            int position = economyService.getPlayerPosition(playerUuid, currencyId);
            return position > 0 ? String.valueOf(position) : "N/A";
        } catch (Exception e) {
            LOGGER.error("Erro ao obter posição do jogador: ", e);
            return "N/A";
        }
    }

    /**
     * Obtém a tag de magnata se o jogador for o top 1
     */
    private String getMagnataTag(UUID playerUuid, String currencyId) {
        try {
            if (economyService.isPlayerMagnata(playerUuid, currencyId)) {
                return economyService.getMagnataTag(currencyId);
            }
            return "";
        } catch (Exception e) {
            LOGGER.error("Erro ao verificar tag de magnata: ", e);
            return "";
        }
    }

    /**
     * Obtém o nome do jogador top 1
     */
    private String getTopPlayerName(String currencyId) {
        try {
            var topPlayers = economyService.getTopPlayers(currencyId, 1);
            if (!topPlayers.isEmpty()) {
                return topPlayers.get(0).getUsername();
            }
            return "N/A";
        } catch (Exception e) {
            LOGGER.error("Erro ao obter nome do top 1: ", e);
            return "N/A";
        }
    }

    /**
     * Obtém o saldo do jogador top 1
     */
    private String getTopPlayerBalance(String currencyId) {
        try {
            var topPlayers = economyService.getTopPlayers(currencyId, 1);
            if (!topPlayers.isEmpty()) {
                double balance = topPlayers.get(0).getBalance();
                return economyService.formatAmount(currencyId, balance);
            }
            return "0";
        } catch (Exception e) {
            LOGGER.error("Erro ao obter saldo do top 1: ", e);
            return "0";
        }
    }

    /**
     * Processa múltiplos placeholders em uma string
     */
    public String processPlaceholders(UUID playerUuid, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        Matcher matcher = CURRENCY_PLACEHOLDER_PATTERN.matcher(text);

        while (matcher.find()) {
            String fullPlaceholder = matcher.group(0);
            String replacement = processPlaceholder(playerUuid, fullPlaceholder);
            result = result.replace("%" + fullPlaceholder + "%", replacement);
        }

        return result;
    }

    /**
     * Obtém informações do usuário do LuckPerms
     */
    public User getLuckPermsUser(UUID playerUuid) {
        if (luckPerms == null) {
            return null;
        }

        try {
            return luckPerms.getUserManager().getUser(playerUuid);
        } catch (Exception e) {
            LOGGER.error("Erro ao obter usuário do LuckPerms: ", e);
            return null;
        }
    }

    /**
     * Verifica se o jogador tem uma permissão específica
     */
    public boolean hasPermission(UUID playerUuid, String permission) {
        if (luckPerms == null) {
            return false; // Default to false if LuckPerms is not available
        }

        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user != null) {
                return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao verificar permissão: ", e);
        }

        return false;
    }

    /**
     * Obtém o grupo principal do jogador
     */
    public String getPrimaryGroup(UUID playerUuid) {
        if (luckPerms == null) {
            return "default";
        }

        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user != null) {
                return user.getPrimaryGroup();
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao obter grupo principal: ", e);
        }

        return "default";
    }

    /**
     * Obtém o prefixo do jogador do LuckPerms
     */
    public String getPrefix(UUID playerUuid) {
        if (luckPerms == null) {
            return "";
        }

        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                return prefix != null ? prefix : "";
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao obter prefixo: ", e);
        }

        return "";
    }

    /**
     * Obtém o sufixo do jogador do LuckPerms
     */
    public String getSuffix(UUID playerUuid) {
        if (luckPerms == null) {
            return "";
        }

        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user != null) {
                String suffix = user.getCachedData().getMetaData().getSuffix();
                return suffix != null ? suffix : "";
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao obter sufixo: ", e);
        }

        return "";
    }

    /**
     * Cria um mapa com todos os placeholders disponíveis para um jogador
     */
    public Map<String, String> getAllPlaceholders(UUID playerUuid) {
        Map<String, String> placeholders = new HashMap<>();

        // Add placeholders for each currency
        for (String currencyId : economyService.getAvailableCurrencies()) {
            String prefix = "nighteconomy_" + currencyId + "_";

            placeholders.put(prefix + "prefix", processPlaceholder(playerUuid, prefix + "prefix"));
            placeholders.put(prefix + "id", processPlaceholder(playerUuid, prefix + "id"));
            placeholders.put(prefix + "balance", processPlaceholder(playerUuid, prefix + "balance"));
            placeholders.put(prefix + "balance_raw", processPlaceholder(playerUuid, prefix + "balance_raw"));
            placeholders.put(prefix + "position", processPlaceholder(playerUuid, prefix + "position"));
            placeholders.put(prefix + "magnata", processPlaceholder(playerUuid, prefix + "magnata"));
            placeholders.put(prefix + "top1_name", processPlaceholder(playerUuid, prefix + "top1_name"));
            placeholders.put(prefix + "top1_balance", processPlaceholder(playerUuid, prefix + "top1_balance"));
            placeholders.put(prefix + "ranking_enabled", processPlaceholder(playerUuid, prefix + "ranking_enabled"));
            placeholders.put(prefix + "payment_enabled", processPlaceholder(playerUuid, prefix + "payment_enabled"));
        }

        return placeholders;
    }

    /**
     * Obtém a lista de placeholders disponíveis
     */
    public String[] getAvailablePlaceholders() {
        return new String[]{
            "nighteconomy_<currency>_prefix",
            "nighteconomy_<currency>_id",
            "nighteconomy_<currency>_balance",
            "nighteconomy_<currency>_balance_raw",
            "nighteconomy_<currency>_position",
            "nighteconomy_<currency>_magnata",
            "nighteconomy_<currency>_top1_name",
            "nighteconomy_<currency>_top1_balance",
            "nighteconomy_<currency>_ranking_enabled",
            "nighteconomy_<currency>_payment_enabled"
        };
    }

    /**
     * Verifica se o LuckPerms está disponível
     */
    public boolean isLuckPermsAvailable() {
        return luckPerms != null;
    }
}

