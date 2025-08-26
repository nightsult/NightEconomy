package org.night.nighteconomy.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.CommentedConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Path configDir;
    private final Path messagesFile;
    private Map<String, String> messages = new HashMap<>();

    public MessageManager(Path configDir) {
        this.configDir = configDir;
        this.messagesFile = configDir.resolve("Messages.toml");
        loadMessages();
    }

    private void loadMessages() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(messagesFile)) {
                createDefaultMessagesFile();
            }

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(messagesFile)
                    .preserveInsertionOrder()
                    .sync()
                    .build()) {
                cfg.load();

                messages.clear();

                loadMessagesFromSection(cfg, "global", "");
                loadMessagesFromSection(cfg, "commands", "commands.");
                loadMessagesFromSection(cfg, "errors", "errors.");
                loadMessagesFromSection(cfg, "success", "success.");
                loadMessagesFromSection(cfg, "ranking", "ranking.");
                loadMessagesFromSection(cfg, "payment", "payment.");
            }

        } catch (Exception e) {
            LOGGER.error("Error loading messages: ", e);
            loadDefaultMessages();
        }
    }

    private void loadMessagesFromSection(UnmodifiableConfig cfg, String section, String prefix) {
        if (!cfg.contains(section)) return;
        UnmodifiableConfig sec = cfg.get(section);
        if (sec == null) return;

        Map<String, Object> sectionMap = sec.valueMap();
        if (sectionMap == null) return;

        for (Map.Entry<String, Object> entry : sectionMap.entrySet()) {
            String key = prefix + entry.getKey();
            String value = String.valueOf(entry.getValue());
            messages.put(key, value);
        }
    }

    private void createDefaultMessagesFile() {
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(messagesFile)
                .preserveInsertionOrder()
                .sync()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            CommentedConfig root = cfg;

            CommentedConfig global = CommentedConfig.inMemory();
            global.set("prefix", "&7[&eNightEconomy&7]&r");
            global.set("no-permission", "&cYou do not have permission to use this command!");
            global.set("player-not-found", "&cPlayer not found!");
            global.set("invalid-amount", "&cInvalid amount! Please use a positive number..");
            global.set("currency-not-found", "&cCurrency '{currency}' not found!");
            global.set("reload-success", "&aSettings reloaded successfully!");
            global.set("transaction-fee", "&eTransaction fee: &f{fee}");
            root.set("global", global);

            CommentedConfig commands = CommentedConfig.inMemory();
            commands.set("balance", "&aYour current balance: &f{amount}");
            commands.set("balance-other", "&aBalance of {player}: &f{amount}");
            commands.set("pay-sent", "&aYou paid &f{amount} &afor the &f{player}!");
            commands.set("pay-received", "&aYou received &f{amount} &afrom &f{player}!");
            commands.set("money-added", "&aAdded &f{amount} &ato &f{player}!");
            commands.set("money-removed", "&cRemoved &f{amount} &cfrom &f{player}!");
            commands.set("money-set", "&aBalance of &f{player} &aset to &f{amount}!");
            commands.set("balance-reset", "&aBalance of &f{player} &areset!");
            commands.set("currency-reloaded", "&aCurrency &f{currency} &areloaded!");
            root.set("commands", commands);

            CommentedConfig errors = CommentedConfig.inMemory();
            errors.set("insufficient-funds", "&cYou do not have sufficient funds!");
            errors.set("payment-disabled", "&c{player} does not accept payments!");
            errors.set("cannot-pay-self", "&cYou cannot pay yourself!");
            errors.set("account-creation-failed", "&cError creating account!");
            errors.set("balance-operation-failed", "&cError performing balance operation!");
            errors.set("payment-failed", "&cError performing payment!");
            errors.set("database-error", "&cDatabase error!");
            root.set("errors", errors);

            CommentedConfig success = CommentedConfig.inMemory();
            success.set("account-created", "&aAccount created successfully!");
            success.set("payment-completed", "&aPayment completed successfully!");
            success.set("balance-updated", "&aBalance updated successfully!");
            success.set("settings-saved", "&aSettings saved!");
            root.set("success", success);

            CommentedConfig ranking = CommentedConfig.inMemory();
            ranking.set("header", "&6=== Top {currency} ===");
            ranking.set("entry", "&e{position}. &f{player} &7- &a{amount}");
            ranking.set("empty", "&cNo players found in the ranking!");
            ranking.set("position", "&aYour position in the ranking: &f{position}");
            ranking.set("not-in-ranking", "&cYou are not in the ranking!");
            ranking.set("magnate-tag", "&6[MAGNATE]");
            ranking.set("updated", "&aRanking updated!");
            root.set("ranking", ranking);

            CommentedConfig payment = CommentedConfig.inMemory();
            payment.set("toggle-enabled", "&aPayments enabled!");
            payment.set("toggle-disabled", "&cPayments disabled!");
            payment.set("fee-charged", "&eFee charged: &f{fee}");
            payment.set("confirmation", "&eConfirm payment of &f{amount} &eto &f{player}? &a/confirm");
            root.set("payment", payment);

            cfg.save();
        } catch (Exception e) {
            LOGGER.error("Error creating default message file: ", e);
        }
    }

    private void loadDefaultMessages() {
        LOGGER.warn("Loading default messages due to configuration error");
        messages.clear();

        messages.put("prefix", "&6[NightEconomy]&r");
        messages.put("no-permission", "&cYou do not have permission to use this command!");
        messages.put("player-not-found", "&cPlayer not found!");
        messages.put("invalid-amount", "&cInvalid amount! Please use a positive number.");
        messages.put("currency-not-found", "&cCurrency '{currency}' not found!");
        messages.put("reload-success", "&aSettings reloaded successfully!");

        messages.put("commands.balance", "&aYour current balance: &f{amount}");
        messages.put("commands.balance-other", "&aBalance of {player}: &f{amount}");
        messages.put("commands.pay-sent", "&aYou paid &f{amount} &ato &f{player}!");
        messages.put("commands.pay-received", "&aYou received &f{amount} &afrom &f{player}!");

        messages.put("errors.insufficient-funds", "&cYou do not have sufficient funds!");
        messages.put("errors.payment-disabled", "&c{player} does not accept payments!");
        messages.put("errors.cannot-pay-self", "&cYou cannot pay yourself!");

        messages.put("ranking.header", "&6=== Top {currency} ===");
        messages.put("ranking.entry", "&e{position}. &f{player} &7- &a{amount}");
        messages.put("ranking.empty", "&cNo players found in the ranking!");

        messages.put("payment.toggle-enabled", "&aPayments enabled!");
        messages.put("payment.toggle-disabled", "&cPayments disabled!");
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    public String getMessage(String key, String defaultMessage) {
        return messages.getOrDefault(key, defaultMessage);
    }

    public String getFormattedMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public String getFormattedMessage(String key, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        return getFormattedMessage(key, placeholders);
    }

    public String getFormattedMessage(String key, String... placeholderPairs) {
        if (placeholderPairs.length % 2 != 0) {
            LOGGER.warn("Odd number of arguments for placeholders in: {}", key);
            return getMessage(key);
        }
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < placeholderPairs.length; i += 2) {
            placeholders.put(placeholderPairs[i], placeholderPairs[i + 1]);
        }
        return getFormattedMessage(key, placeholders);
    }

    public void reloadMessages() {
        LOGGER.info("Reloading messages...");
        loadMessages();
    }

    public boolean hasMessage(String key) {
        return messages.containsKey(key);
    }

    public Map<String, String> getAllMessages() {
        return new HashMap<>(messages);
    }

    public void setMessage(String key, String value) {
        messages.put(key, value);
    }

    public void saveMessages() {
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(messagesFile)
                .preserveInsertionOrder()
                .sync()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            CommentedConfig root = cfg;

            Map<String, String> global = new HashMap<>();
            CommentedConfig commands = CommentedConfig.inMemory();
            CommentedConfig errors = CommentedConfig.inMemory();
            CommentedConfig success = CommentedConfig.inMemory();
            CommentedConfig ranking = CommentedConfig.inMemory();
            CommentedConfig payment = CommentedConfig.inMemory();

            for (Map.Entry<String, String> entry : messages.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("commands.")) {
                    commands.set(key.substring(9), value);
                } else if (key.startsWith("errors.")) {
                    errors.set(key.substring(7), value);
                } else if (key.startsWith("success.")) {
                    success.set(key.substring(8), value);
                } else if (key.startsWith("ranking.")) {
                    ranking.set(key.substring(8), value);
                } else if (key.startsWith("payment.")) {
                    payment.set(key.substring(8), value);
                } else {
                    global.put(key, value);
                }
            }

            root.set("global", global);
            root.set("commands", commands);
            root.set("errors", errors);
            root.set("success", success);
            root.set("ranking", ranking);
            root.set("payment", payment);

            cfg.save();
            LOGGER.info("Messages saved successfully!");
        } catch (Exception e) {
            LOGGER.error("Error saving messages: ", e);
        }
    }

    public String translateColors(String message) {
        return message.replace("&", "§");
    }

    public String getPrefix() {
        return translateColors(getMessage("prefix"));
    }

    public String getPrefixedMessage(String key) {
        return getPrefix() + " " + translateColors(getMessage(key));
    }

    public String getPrefixedMessage(String key, Map<String, String> placeholders) {
        return getPrefix() + " " + translateColors(getFormattedMessage(key, placeholders));
    }

    public String getPrefixedMessage(String key, String... placeholderPairs) {
        return getPrefix() + " " + translateColors(getFormattedMessage(key, placeholderPairs));
    }

    public String getErrorMessage(String key) { return getPrefixedMessage("errors." + key); }
    public String getSuccessMessage(String key) { return getPrefixedMessage("success." + key); }
    public String getCommandMessage(String key) { return getPrefixedMessage("commands." + key); }
    public String getRankingMessage(String key) { return getPrefixedMessage("ranking." + key); }
    public String getPaymentMessage(String key) { return getPrefixedMessage("payment." + key); }

    public String getErrorMessage(String key, Map<String, String> placeholders) {
        return getPrefixedMessage("errors." + key, placeholders);
    }
    public String getSuccessMessage(String key, Map<String, String> placeholders) {
        return getPrefixedMessage("success." + key, placeholders);
    }
    public String getCommandMessage(String key, Map<String, String> placeholders) {
        return getPrefixedMessage("commands." + key, placeholders);
    }
    public String getRankingMessage(String key, Map<String, String> placeholders) {
        return getPrefixedMessage("ranking." + key, placeholders);
    }
    public String getPaymentMessage(String key, Map<String, String> placeholders) {
        return getPrefixedMessage("payment." + key, placeholders);
    }

    public String getErrorMessage(String key, String... placeholderPairs) {
        return getPrefixedMessage("errors." + key, placeholderPairs);
    }
    public String getSuccessMessage(String key, String... placeholderPairs) {
        return getPrefixedMessage("success." + key, placeholderPairs);
    }
    public String getCommandMessage(String key, String... placeholderPairs) {
        return getPrefixedMessage("commands." + key, placeholderPairs);
    }
    public String getRankingMessage(String key, String... placeholderPairs) {
        return getPrefixedMessage("ranking." + key, placeholderPairs);
    }
    public String getPaymentMessage(String key, String... placeholderPairs) {
        return getPrefixedMessage("payment." + key, placeholderPairs);
    }
}
