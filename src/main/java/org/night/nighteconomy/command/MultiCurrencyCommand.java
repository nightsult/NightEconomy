package org.night.nighteconomy.command;

import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.currency.CurrencyConfig;
import org.night.nighteconomy.service.MultiCurrencyEconomyService;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.RankingEntry;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager.Transaction;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MultiCurrencyCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final MultiCurrencyEconomyService economyService;
    private final ConfigManager configManager;
    
    public MultiCurrencyCommand(MultiCurrencyEconomyService economyService, ConfigManager configManager) {
        this.economyService = economyService;
        this.configManager = configManager;
    }
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register commands for each currency
        Map<String, CurrencyConfig> currencies = configManager.getCurrencies();
        
        for (Map.Entry<String, CurrencyConfig> entry : currencies.entrySet()) {
            String currencyId = entry.getKey();
            CurrencyConfig config = entry.getValue();
            
            registerCurrencyCommands(dispatcher, currencyId, config);
        }
        
        // Register global economy command
        registerGlobalEconomyCommand(dispatcher);
    }
    
    private void registerCurrencyCommands(CommandDispatcher<CommandSourceStack> dispatcher, String currencyId, CurrencyConfig config) {
        if (config.getCommands() == null || config.getCommands().getMain() == null) {
            return;
        }
        
        // Register main commands (e.g., /money, /coins, /balance)
        for (String mainCommand : config.getCommands().getMain()) {
            dispatcher.register(Commands.literal(mainCommand)
                .executes(context -> showBalance(context, currencyId))
                
                // /money <player> - See other player's balance
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getSee()))
                    .executes(context -> showOtherBalance(context, currencyId)))
                
                // /money pay <player> <amount>
                .then(Commands.literal("pay")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getPay()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> payPlayer(context, currencyId)))))
                
                // /money add <player> <amount>
                .then(Commands.literal("add")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getAdd()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> addMoney(context, currencyId)))))
                
                // /money remove <player> <amount>
                .then(Commands.literal("remove")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getRemove()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> removeMoney(context, currencyId)))))
                
                // /money set <player> <amount>
                .then(Commands.literal("set")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getSet()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                            .executes(context -> setMoney(context, currencyId)))))
                
                // /money top
                .then(Commands.literal("top")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getTop()))
                    .executes(context -> showRanking(context, currencyId)))
                
                // /money reset <player>
                .then(Commands.literal("reset")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getReset()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> resetPlayer(context, currencyId))))
                
                // /money reload
                .then(Commands.literal("reload")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getReload()))
                    .executes(context -> reloadCurrency(context, currencyId)))
                
                // /money toggle
                .then(Commands.literal("toggle")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getTogglePayment()))
                    .executes(context -> togglePayments(context, currencyId)))
                
                // /money transactions
                .then(Commands.literal("transactions")
                    .requires(source -> hasPermission(source, config.getCommands().getSubcommands().getTransactions()))
                    .executes(context -> showTransactions(context, currencyId))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> hasPermissionList(source, config.getCommands().getSubcommands().getTransactions().getPermissions()))
                        .executes(context -> showOtherTransactions(context, currencyId))))
            );
        }
    }
    
    private void registerGlobalEconomyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neconomy")
            .requires(source -> source.hasPermission(2)) // OP level 2
            
            // /neconomy reload
            .then(Commands.literal("reload")
                .executes(this::reloadAll))
            
            // /neconomy list
            .then(Commands.literal("list")
                .executes(this::listCurrencies))
            
            // /neconomy info <currency>
            .then(Commands.literal("info")
                .then(Commands.argument("currency", StringArgumentType.string())
                    .executes(this::showCurrencyInfo)))
        );
    }
    
    // Command implementations
    private int showBalance(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerUuid = player.getUUID();
        
        economyService.ensureAccountExists(playerUuid, currencyId, player.getName().getString());
        double balance = economyService.getBalance(playerUuid, currencyId);
        String formattedBalance = economyService.formatAmount(currencyId, balance);
        
        CurrencyConfig config = configManager.getCurrency(currencyId);
        String message = getMessageFromConfig(config, "balance", "&aSeu saldo atual: &f{amount}")
            .replace("{amount}", formattedBalance);
        
        context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        return 1;
    }
    
    private int showOtherBalance(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        UUID targetUuid = targetPlayer.getUUID();
        
        economyService.ensureAccountExists(targetUuid, currencyId, targetPlayer.getName().getString());
        double balance = economyService.getBalance(targetUuid, currencyId);
        String formattedBalance = economyService.formatAmount(currencyId, balance);
        
        CurrencyConfig config = configManager.getCurrency(currencyId);
        String message = getMessageFromConfig(config, "balance-other", "&aSaldo de {player}: &f{amount}")
            .replace("{player}", targetPlayer.getName().getString())
            .replace("{amount}", formattedBalance);
        
        context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        return 1;
    }
    
    private int payPlayer(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        ServerPlayer receiver = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        if (sender.getUUID().equals(receiver.getUUID())) {
            context.getSource().sendFailure(Component.literal(translateColors("&cVocê não pode pagar para si mesmo!")));
            return 0;
        }
        
        economyService.ensureAccountExists(sender.getUUID(), currencyId, sender.getName().getString());
        economyService.ensureAccountExists(receiver.getUUID(), currencyId, receiver.getName().getString());
        
        MultiCurrencyEconomyService.PaymentResult result = economyService.payPlayer(
            sender.getUUID(), receiver.getUUID(), currencyId, amount);
        
        CurrencyConfig config = configManager.getCurrency(currencyId);
        String formattedAmount = economyService.formatAmount(currencyId, amount);
        
        if (result.isSuccess()) {
            String senderMessage = getMessageFromConfig(config, "pay-sent", "&aVocê pagou &f{amount} &apara &f{player}!")
                .replace("{amount}", formattedAmount)
                .replace("{player}", receiver.getName().getString());
            
            String receiverMessage = getMessageFromConfig(config, "pay-received", "&aVocê recebeu &f{amount} &ade &f{player}!")
                .replace("{amount}", formattedAmount)
                .replace("{player}", sender.getName().getString());
            
            context.getSource().sendSuccess(() -> Component.literal(translateColors(senderMessage)), false);
            receiver.sendSystemMessage(Component.literal(translateColors(receiverMessage)));
            
            if (result.getFee() > 0) {
                String feeMessage = configManager.getGlobalMessage("transaction-fee")
                    .replace("{fee}", economyService.formatAmount(currencyId, result.getFee()));
                context.getSource().sendSuccess(() -> Component.literal(translateColors(feeMessage)), false);
            }
        } else {
            String errorMessage = getErrorMessage(result.getMessage(), config);
            context.getSource().sendFailure(Component.literal(translateColors(errorMessage)));
        }
        
        return result.isSuccess() ? 1 : 0;
    }
    
    private int addMoney(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        economyService.ensureAccountExists(targetPlayer.getUUID(), currencyId, targetPlayer.getName().getString());
        boolean success = economyService.addBalance(targetPlayer.getUUID(), currencyId, amount);
        
        if (success) {
            String formattedAmount = economyService.formatAmount(currencyId, amount);
            String message = "&aAdicionado &f" + formattedAmount + " &apara &f" + targetPlayer.getName().getString() + "&a!";
            context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        } else {
            context.getSource().sendFailure(Component.literal(translateColors("&cErro ao adicionar dinheiro!")));
        }
        
        return success ? 1 : 0;
    }
    
    private int removeMoney(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        economyService.ensureAccountExists(targetPlayer.getUUID(), currencyId, targetPlayer.getName().getString());
        boolean success = economyService.subtractBalance(targetPlayer.getUUID(), currencyId, amount);
        
        if (success) {
            String formattedAmount = economyService.formatAmount(currencyId, amount);
            String message = "&cRemovido &f" + formattedAmount + " &cde &f" + targetPlayer.getName().getString() + "&c!";
            context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        } else {
            context.getSource().sendFailure(Component.literal(translateColors("&cErro ao remover dinheiro ou saldo insuficiente!")));
        }
        
        return success ? 1 : 0;
    }
    
    private int setMoney(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        economyService.ensureAccountExists(targetPlayer.getUUID(), currencyId, targetPlayer.getName().getString());
        boolean success = economyService.setBalance(targetPlayer.getUUID(), currencyId, amount);
        
        if (success) {
            String formattedAmount = economyService.formatAmount(currencyId, amount);
            String message = "&aSaldo de &f" + targetPlayer.getName().getString() + " &adefinido para &f" + formattedAmount + "&a!";
            context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        } else {
            context.getSource().sendFailure(Component.literal(translateColors("&cErro ao definir saldo!")));
        }
        
        return success ? 1 : 0;
    }
    
    private int showRanking(CommandContext<CommandSourceStack> context, String currencyId) {
        List<RankingEntry> ranking = economyService.getTopPlayers(currencyId, 10);
        CurrencyConfig config = configManager.getCurrency(currencyId);
        
        if (ranking.isEmpty()) {
            String message = configManager.getGlobalMessage("ranking-empty");
            context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
            return 1;
        }
        
        String header = configManager.getGlobalMessage("ranking-header")
            .replace("{currency}", config.getName());
        context.getSource().sendSuccess(() -> Component.literal(translateColors(header)), false);
        
        for (RankingEntry entry : ranking) {
            String formattedAmount = economyService.formatAmount(currencyId, entry.getBalance());
            String line = configManager.getGlobalMessage("ranking-entry")
                .replace("{position}", String.valueOf(entry.getPosition()))
                .replace("{player}", entry.getUsername())
                .replace("{amount}", formattedAmount);
            
            context.getSource().sendSuccess(() -> Component.literal(translateColors(line)), false);
        }
        
        return 1;
    }
    
    private int resetPlayer(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        
        economyService.ensureAccountExists(targetPlayer.getUUID(), currencyId, targetPlayer.getName().getString());
        boolean success = economyService.resetBalance(targetPlayer.getUUID(), currencyId);
        
        if (success) {
            String message = "&aSaldo de &f" + targetPlayer.getName().getString() + " &aresetado!";
            context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        } else {
            context.getSource().sendFailure(Component.literal(translateColors("&cErro ao resetar saldo!")));
        }
        
        return success ? 1 : 0;
    }
    
    private int reloadCurrency(CommandContext<CommandSourceStack> context, String currencyId) {
        configManager.reloadCurrency(currencyId);
        economyService.forceRankingUpdate(currencyId);
        
        String message = "&aMoeda &f" + currencyId + " &arecarregada com sucesso!";
        context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        return 1;
    }
    
    private int togglePayments(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        economyService.ensureAccountExists(player.getUUID(), currencyId, player.getName().getString());
        boolean currentSetting = economyService.isPaymentEnabled(player.getUUID(), currencyId);
        boolean newSetting = !currentSetting;
        
        boolean success = economyService.setPaymentEnabled(player.getUUID(), currencyId, newSetting);
        
        if (success) {
            String messageKey = newSetting ? "payment-toggle-enabled" : "payment-toggle-disabled";
            String message = configManager.getGlobalMessage(messageKey);
            context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        } else {
            context.getSource().sendFailure(Component.literal(translateColors("&cErro ao alterar configuração de pagamentos!")));
        }
        
        return success ? 1 : 0;
    }
    
    private int showTransactions(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<Transaction> transactions = economyService.getPlayerTransactions(player.getUUID(), currencyId, 10);
        
        if (transactions.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(translateColors("&cNenhuma transação encontrada!")), false);
            return 1;
        }
        
        context.getSource().sendSuccess(() -> Component.literal(translateColors("&6=== Suas Transações ===")), false);
        
        for (Transaction transaction : transactions) {
            String formattedAmount = economyService.formatAmount(currencyId, transaction.getAmount());
            String line = String.format("&e%s &7- &f%s &7(%s)", 
                transaction.getType(), 
                formattedAmount, 
                transaction.getTimestamp().toString());
            
            context.getSource().sendSuccess(() -> Component.literal(translateColors(line)), false);
        }
        
        return 1;
    }
    
    private int showOtherTransactions(CommandContext<CommandSourceStack> context, String currencyId) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        List<Transaction> transactions = economyService.getPlayerTransactions(targetPlayer.getUUID(), currencyId, 10);
        
        if (transactions.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(translateColors("&cNenhuma transação encontrada!")), false);
            return 1;
        }
        
        String header = "&6=== Transações de " + targetPlayer.getName().getString() + " ===";
        context.getSource().sendSuccess(() -> Component.literal(translateColors(header)), false);
        
        for (Transaction transaction : transactions) {
            String formattedAmount = economyService.formatAmount(currencyId, transaction.getAmount());
            String line = String.format("&e%s &7- &f%s &7(%s)", 
                transaction.getType(), 
                formattedAmount, 
                transaction.getTimestamp().toString());
            
            context.getSource().sendSuccess(() -> Component.literal(translateColors(line)), false);
        }
        
        return 1;
    }
    
    // Global commands
    private int reloadAll(CommandContext<CommandSourceStack> context) {
        configManager.reloadConfigurations();
        
        String message = configManager.getGlobalMessage("reload-success");
        context.getSource().sendSuccess(() -> Component.literal(translateColors(message)), false);
        return 1;
    }
    
    private int listCurrencies(CommandContext<CommandSourceStack> context) {
        Map<String, CurrencyConfig> currencies = configManager.getCurrencies();
        
        context.getSource().sendSuccess(() -> Component.literal(translateColors("&6=== Moedas Disponíveis ===")), false);
        
        for (Map.Entry<String, CurrencyConfig> entry : currencies.entrySet()) {
            String currencyId = entry.getKey();
            CurrencyConfig config = entry.getValue();
            String line = String.format("&e%s &7- &f%s &7(Ranking: %s)", 
                currencyId, 
                config.getName(), 
                config.isRanking() ? "&aAtivo" : "&cInativo");
            
            context.getSource().sendSuccess(() -> Component.literal(translateColors(line)), false);
        }
        
        return 1;
    }
    
    private int showCurrencyInfo(CommandContext<CommandSourceStack> context) {
        String currencyId = StringArgumentType.getString(context, "currency");
        CurrencyConfig config = configManager.getCurrency(currencyId);
        
        if (config == null) {
            String message = configManager.getGlobalMessage("currency-not-found")
                .replace("{currency}", currencyId);
            context.getSource().sendFailure(Component.literal(translateColors(message)));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> Component.literal(translateColors("&6=== Informações da Moeda ===")), false);
        context.getSource().sendSuccess(() -> Component.literal(translateColors("&eID: &f" + config.getId())), false);
        context.getSource().sendSuccess(() -> Component.literal(translateColors("&eNome: &f" + config.getName())), false);
        context.getSource().sendSuccess(() -> Component.literal(translateColors("&eValor Padrão: &f" + config.getDefaultValue())), false);
        context.getSource().sendSuccess(() -> Component.literal(translateColors("&eRanking: &f" + (config.isRanking() ? "Ativo" : "Inativo"))), false);
        
        if (config.isRanking()) {
            context.getSource().sendSuccess(() -> Component.literal(translateColors("&eIntervalo de Atualização: &f" + config.getUpdate() + "s")), false);
        }
        
        return 1;
    }
    
    // Utility methods
    private boolean hasPermission(CommandSourceStack source, CurrencyConfig.SubcommandConfig subcommand) {
        if (subcommand == null) return true;
        
        if (subcommand.getPermission() != null) {
            return source.hasPermission(2); // For now, require OP level 2
        }
        
        return true;
    }
    
    private boolean hasPermissionList(CommandSourceStack source, List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) return true;
        
        // For now, require OP level 2 for any permission
        return source.hasPermission(2);
    }
    
    private String getMessageFromConfig(CurrencyConfig config, String key, String defaultMessage) {
        if (config.getMessages() != null && config.getMessages().containsKey(key)) {
            return config.getMessages().get(key);
        }
        return defaultMessage;
    }
    
    private String getErrorMessage(String errorKey, CurrencyConfig config) {
        switch (errorKey) {
            case "Saldo insuficiente":
                return getMessageFromConfig(config, "insufficient-funds", "&cVocê não tem saldo suficiente!");
            case "Jogador não aceita pagamentos":
                return configManager.getGlobalMessage("payment-disabled").replace("{player}", "o jogador");
            case "Quantia inválida":
                return getMessageFromConfig(config, "invalid-amount", "&cQuantia inválida!");
            default:
                return "&cErro: " + errorKey;
        }
    }
    
    private String translateColors(String message) {
        return message.replace("&", "§");
    }
}

