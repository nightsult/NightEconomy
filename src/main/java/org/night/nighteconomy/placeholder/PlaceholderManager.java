package org.night.nighteconomy.placeholder;

import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.service.MultiCurrencyEconomyService;
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

    private static final Pattern CURRENCY_PLACEHOLDER_PATTERN =
            Pattern.compile("nighteconomy_([a-zA-Z0-9_]+)_([a-zA-Z0-9_]+)");

    public PlaceholderManager(MultiCurrencyEconomyService economyService, ConfigManager configManager) {
        this.economyService = economyService;
        this.configManager = configManager;
    }

    public void registerPlaceholders() {
    }

    public String processPlaceholder(UUID playerUuid, String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return placeholder;
        }
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

    private String processCurrencyPlaceholder(UUID playerUuid, String currencyId, String placeholderType) {
        CurrencyConfig config = configManager.getCurrency(currencyId);
        if (config == null) {
            return "Money not found";
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
                return "Placeholder unknown";
        }
    }

    private String getFormattedBalance(UUID playerUuid, String currencyId) {
        try {
            double balance = economyService.getBalance(playerUuid, currencyId);
            return economyService.formatAmount(currencyId, balance);
        } catch (Exception e) {
            LOGGER.error("Error getting formatted balance: ", e);
            return "0";
        }
    }

    private String getPlayerPosition(UUID playerUuid, String currencyId) {
        try {
            int position = economyService.getPlayerPosition(playerUuid, currencyId);
            return position > 0 ? String.valueOf(position) : "N/A";
        } catch (Exception e) {
            LOGGER.error("Error getting player position: ", e);
            return "N/A";
        }
    }

    private String getMagnataTag(UUID playerUuid, String currencyId) {
        try {
            if (economyService.isPlayerMagnata(playerUuid, currencyId)) {
                return economyService.getMagnataTag(currencyId);
            }
            return "";
        } catch (Exception e) {
            LOGGER.error("Error verifying tycoon tag: ", e);
            return "";
        }
    }

    private String getTopPlayerName(String currencyId) {
        try {
            var topPlayers = economyService.getTopPlayers(currencyId, 1);
            if (!topPlayers.isEmpty()) {
                return topPlayers.get(0).getUsername();
            }
            return "N/A";
        } catch (Exception e) {
            LOGGER.error("Error getting top 1 name: ", e);
            return "N/A";
        }
    }

    private String getTopPlayerBalance(String currencyId) {
        try {
            var topPlayers = economyService.getTopPlayers(currencyId, 1);
            if (!topPlayers.isEmpty()) {
                double balance = topPlayers.get(0).getBalance();
                return economyService.formatAmount(currencyId, balance);
            }
            return "0";
        } catch (Exception e) {
            LOGGER.error("Error getting top 1 balance: ", e);
            return "0";
        }
    }

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

    public Map<String, String> getAllPlaceholders(UUID playerUuid) {
        Map<String, String> placeholders = new HashMap<>();

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

    public boolean isLuckPermsAvailable() {
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            java.lang.reflect.Method get = provider.getMethod("get");
            Object api = get.invoke(null);
            return api != null;
        } catch (Throwable t) {
            return false;
        }
    }
}