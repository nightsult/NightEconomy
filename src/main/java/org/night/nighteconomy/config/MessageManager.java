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

/**
 * MessageManager baseado em NightConfig (TOML)
 */
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
            // Garante diretório
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // Cria arquivo default se não existir
            if (!Files.exists(messagesFile)) {
                createDefaultMessagesFile();
            }

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(messagesFile)
                    .preserveInsertionOrder()
                    .sync()
                    .build()) {
                cfg.load();

                messages.clear();

                // Carrega seções
                loadMessagesFromSection(cfg, "global", "");
                loadMessagesFromSection(cfg, "commands", "commands.");
                loadMessagesFromSection(cfg, "errors", "errors.");
                loadMessagesFromSection(cfg, "success", "success.");
                loadMessagesFromSection(cfg, "ranking", "ranking.");
                loadMessagesFromSection(cfg, "payment", "payment.");
            }

            LOGGER.info("Mensagens carregadas com sucesso! Total: {}", messages.size());
        } catch (Exception e) {
            LOGGER.error("Erro ao carregar mensagens: ", e);
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

            // Monta estrutura padrão
            CommentedConfig root = cfg;

            // Seção global
            CommentedConfig global = CommentedConfig.inMemory();
            global.set("prefix", "&6[NightEconomy]&r");
            global.set("no-permission", "&cVocê não tem permissão para usar este comando!");
            global.set("player-not-found", "&cJogador não encontrado!");
            global.set("invalid-amount", "&cQuantia inválida! Use um número positivo.");
            global.set("currency-not-found", "&cMoeda '{currency}' não encontrada!");
            global.set("reload-success", "&aConfigurações recarregadas com sucesso!");
            global.set("transaction-fee", "&eTaxa de transação: &f{fee}");
            root.set("global", global);

            // Seção commands
            CommentedConfig commands = CommentedConfig.inMemory();
            commands.set("balance", "&aSeu saldo atual: &f{amount}");
            commands.set("balance-other", "&aSaldo de {player}: &f{amount}");
            commands.set("pay-sent", "&aVocê pagou &f{amount} &apara &f{player}!");
            commands.set("pay-received", "&aVocê recebeu &f{amount} &ade &f{player}!");
            commands.set("money-added", "&aAdicionado &f{amount} &apara &f{player}!");
            commands.set("money-removed", "&cRemovido &f{amount} &cde &f{player}!");
            commands.set("money-set", "&aSaldo de &f{player} &adefinido para &f{amount}!");
            commands.set("balance-reset", "&aSaldo de &f{player} &aresetado!");
            commands.set("currency-reloaded", "&aMoeda &f{currency} &arecarregada!");
            root.set("commands", commands);

            // Seção errors
            CommentedConfig errors = CommentedConfig.inMemory();
            errors.set("insufficient-funds", "&cVocê não tem saldo suficiente!");
            errors.set("payment-disabled", "&c{player} não aceita pagamentos!");
            errors.set("cannot-pay-self", "&cVocê não pode pagar para si mesmo!");
            errors.set("account-creation-failed", "&cErro ao criar conta!");
            errors.set("balance-operation-failed", "&cErro ao realizar operação de saldo!");
            errors.set("payment-failed", "&cErro ao realizar pagamento!");
            errors.set("database-error", "&cErro no banco de dados!");
            root.set("errors", errors);

            // Seção success
            CommentedConfig success = CommentedConfig.inMemory();
            success.set("account-created", "&aConta criada com sucesso!");
            success.set("payment-completed", "&aPagamento realizado com sucesso!");
            success.set("balance-updated", "&aSaldo atualizado com sucesso!");
            success.set("settings-saved", "&aConfigurações salvas!");
            root.set("success", success);

            // Seção ranking
            CommentedConfig ranking = CommentedConfig.inMemory();
            ranking.set("header", "&6=== Top {currency} ===");
            ranking.set("entry", "&e{position}. &f{player} &7- &a{amount}");
            ranking.set("empty", "&cNenhum jogador encontrado no ranking!");
            ranking.set("position", "&aSua posição no ranking: &f{position}");
            ranking.set("not-in-ranking", "&cVocê não está no ranking!");
            ranking.set("magnata-tag", "&6[MAGNATA]");
            ranking.set("updated", "&aRanking atualizado!");
            root.set("ranking", ranking);

            // Seção payment
            CommentedConfig payment = CommentedConfig.inMemory();
            payment.set("toggle-enabled", "&aPagamentos habilitados!");
            payment.set("toggle-disabled", "&cPagamentos desabilitados!");
            payment.set("fee-charged", "&eTaxa cobrada: &f{fee}");
            payment.set("confirmation", "&eConfirmar pagamento de &f{amount} &epara &f{player}? &a/confirm");
            root.set("payment", payment);

            cfg.save();
            LOGGER.info("Arquivo de mensagens padrão criado: {}", messagesFile);
        } catch (Exception e) {
            LOGGER.error("Erro ao criar arquivo de mensagens padrão: ", e);
        }
    }

    private void loadDefaultMessages() {
        LOGGER.warn("Carregando mensagens padrão devido a erro na configuração");
        messages.clear();

        // Global
        messages.put("prefix", "&6[NightEconomy]&r");
        messages.put("no-permission", "&cVocê não tem permissão para usar este comando!");
        messages.put("player-not-found", "&cJogador não encontrado!");
        messages.put("invalid-amount", "&cQuantia inválida! Use um número positivo.");
        messages.put("currency-not-found", "&cMoeda '{currency}' não encontrada!");
        messages.put("reload-success", "&aConfigurações recarregadas com sucesso!");

        // Commands
        messages.put("commands.balance", "&aSeu saldo atual: &f{amount}");
        messages.put("commands.balance-other", "&aSaldo de {player}: &f{amount}");
        messages.put("commands.pay-sent", "&aVocê pagou &f{amount} &apara &f{player}!");
        messages.put("commands.pay-received", "&aVocê recebeu &f{amount} &ade &f{player}!");

        // Errors
        messages.put("errors.insufficient-funds", "&cVocê não tem saldo suficiente!");
        messages.put("errors.payment-disabled", "&c{player} não aceita pagamentos!");
        messages.put("errors.cannot-pay-self", "&cVocê não pode pagar para si mesmo!");

        // Ranking
        messages.put("ranking.header", "&6=== Top {currency} ===");
        messages.put("ranking.entry", "&e{position}. &f{player} &7- &a{amount}");
        messages.put("ranking.empty", "&cNenhum jogador encontrado no ranking!");

        // Payment
        messages.put("payment.toggle-enabled", "&aPagamentos habilitados!");
        messages.put("payment.toggle-disabled", "&cPagamentos desabilitados!");
    }

    // ======= API pública =======

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMensagem não encontrada: " + key);
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
            LOGGER.warn("Número ímpar de argumentos para placeholders em: {}", key);
            return getMessage(key);
        }
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < placeholderPairs.length; i += 2) {
            placeholders.put(placeholderPairs[i], placeholderPairs[i + 1]);
        }
        return getFormattedMessage(key, placeholders);
    }

    public void reloadMessages() {
        LOGGER.info("Recarregando mensagens...");
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

            // Reconstroi estrutura em seções
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

            // grava
            root.set("global", global);
            root.set("commands", commands);
            root.set("errors", errors);
            root.set("success", success);
            root.set("ranking", ranking);
            root.set("payment", payment);

            cfg.save();
            LOGGER.info("Mensagens salvas com sucesso!");
        } catch (Exception e) {
            LOGGER.error("Erro ao salvar mensagens: ", e);
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

    // Atalhos
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
