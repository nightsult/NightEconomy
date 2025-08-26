package org.night.nighteconomy.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Path configDir;
    private final Path currenciesDir;
    private final MessageManager messageManager;
    private final Map<String, CurrencyConfig> currencies = new ConcurrentHashMap<>();

    public ConfigManager(Path configDir) {
        this.configDir = configDir;
        this.currenciesDir = configDir.resolve("currencies");
        this.messageManager = new MessageManager(configDir);
        loadConfigurations();
    }

    public void loadConfigurations() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (!Files.exists(currenciesDir)) {
                Files.createDirectories(currenciesDir);
            }
            loadCurrencies();
            LOGGER.info("Reloading Sucess!");
        } catch (Exception e) {
            LOGGER.error("Error reloading: ", e);
        }
    }

    private void loadCurrencies() {
        currencies.clear();

        try {
            if (!Files.exists(currenciesDir)) {
                Files.createDirectories(currenciesDir);
                createDefaultCurrencies();
            }

            try (var paths = Files.list(currenciesDir)) {
                paths.filter(path -> path.toString().endsWith(".toml"))
                        .forEach(this::loadCurrency);
            }

            if (currencies.isEmpty()) {
                LOGGER.warn("No coins found, creating default coins...");
                createDefaultCurrencies();
            }

            LOGGER.info("Loaded " + currencies.size() + " currencies.");

        } catch (IOException e) {
            LOGGER.error("Error! Not loaded Currencies: ", e);
        }
    }


    private void loadCurrency(Path currencyFile) {
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(currencyFile)
                .preserveInsertionOrder()
                .sync()
                .build()) {
            cfg.load();

            CurrencyConfig config = CurrencyConfig.fromConfig(cfg);
            if (config != null) {
                currencies.put(config.getId(), config);
                LOGGER.debug("Loaded currency: " + config.getId() + " (" + config.getName() + ")");
            } else {
                LOGGER.warn("Failed to load currency from file: " + currencyFile);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading currency file " + currencyFile + ": ", e);
        }
    }


    private void createDefaultCurrencies() {
        try {
            CurrencyConfig money = CurrencyConfig.createDefault("money", "Money");
            saveCurrency(money);
            currencies.put(money.getId(), money);

            CurrencyConfig coins = CurrencyConfig.createDefault("cash", "Cash");
            coins.setDefaultValue(100.0);
            saveCurrency(coins);
            currencies.put(coins.getId(), coins);

            LOGGER.info("Default currencies created: money, coins");
        } catch (Exception e) {
            LOGGER.error("Error creating default currencies: ", e);
        }
    }


    private void saveCurrency(CurrencyConfig config) {
        try {
            Path currencyFile = currenciesDir.resolve(config.getId() + ".toml");
            config.saveToFile(currencyFile);
            LOGGER.debug("Currency saved: " + config.getId());
        } catch (Exception e) {
            LOGGER.error("Error saving currency " + config.getId() + ": ", e);
        }
    }


    public CurrencyConfig getCurrency(String currencyId) {
        return currencies.get(currencyId);
    }

    public Map<String, CurrencyConfig> getCurrencies() {
        return new HashMap<>(currencies);
    }

    public void reloadConfigurations() {
        LOGGER.info("Reloading all settings...");
        loadConfigurations();
        messageManager.reloadMessages();
    }

    public void reloadCurrency(String currencyId) {
        LOGGER.info("Reloading currency: " + currencyId);
        Path currencyFile = currenciesDir.resolve(currencyId + ".toml");
        if (Files.exists(currencyFile)) {
            loadCurrency(currencyFile);
        } else {
            LOGGER.warn("Currency file not found: " + currencyFile);
        }
    }

    public String getGlobalMessage(String key) {
        return messageManager.getMessage(key);
    }

    public String getGlobalMessage(String key, String defaultMessage) {
        return messageManager.getMessage(key, defaultMessage);
    }

    public String getFormattedGlobalMessage(String key, Map<String, String> placeholders) {
        return messageManager.getFormattedMessage(key, placeholders);
    }

    public String getFormattedGlobalMessage(String key, String... placeholderPairs) {
        return messageManager.getFormattedMessage(key, placeholderPairs);
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public String translateColors(String message) {
        return messageManager.translateColors(message);
    }

    public String getPrefix() {
        return messageManager.getPrefix();
    }

    public String getPrefixedMessage(String key) {
        return messageManager.getPrefixedMessage(key);
    }

    public String getPrefixedMessage(String key, Map<String, String> placeholders) {
        return messageManager.getPrefixedMessage(key, placeholders);
    }

    public String getPrefixedMessage(String key, String... placeholderPairs) {
        return messageManager.getPrefixedMessage(key, placeholderPairs);
    }

    public String getErrorMessage(String key) {
        return messageManager.getErrorMessage(key);
    }

    public String getSuccessMessage(String key) {
        return messageManager.getSuccessMessage(key);
    }

    public String getCommandMessage(String key) {
        return messageManager.getCommandMessage(key);
    }

    public String getRankingMessage(String key) {
        return messageManager.getRankingMessage(key);
    }

    public String getPaymentMessage(String key) {
        return messageManager.getPaymentMessage(key);
    }

    public String getErrorMessage(String key, Map<String, String> placeholders) {
        return messageManager.getErrorMessage(key, placeholders);
    }

    public String getSuccessMessage(String key, Map<String, String> placeholders) {
        return messageManager.getSuccessMessage(key, placeholders);
    }

    public String getCommandMessage(String key, Map<String, String> placeholders) {
        return messageManager.getCommandMessage(key, placeholders);
    }

    public String getRankingMessage(String key, Map<String, String> placeholders) {
        return messageManager.getRankingMessage(key, placeholders);
    }

    public String getPaymentMessage(String key, Map<String, String> placeholders) {
        return messageManager.getPaymentMessage(key, placeholders);
    }

    public String getErrorMessage(String key, String... placeholderPairs) {
        return messageManager.getErrorMessage(key, placeholderPairs);
    }

    public String getSuccessMessage(String key, String... placeholderPairs) {
        return messageManager.getSuccessMessage(key, placeholderPairs);
    }

    public String getCommandMessage(String key, String... placeholderPairs) {
        return messageManager.getCommandMessage(key, placeholderPairs);
    }

    public String getRankingMessage(String key, String... placeholderPairs) {
        return messageManager.getRankingMessage(key, placeholderPairs);
    }

    public String getPaymentMessage(String key, String... placeholderPairs) {
        return messageManager.getPaymentMessage(key, placeholderPairs);
    }
}
