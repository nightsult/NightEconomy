package org.night.nighteconomy.command;

import org.night.nighteconomy.service.MultiCurrencyEconomyService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EcoCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eco")
            .requires(source -> source.hasPermission(2)) // Requer permissão de OP
            .then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(EcoCommand::executeGive)
                    )
                )
            )
            .then(Commands.literal("take")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(EcoCommand::executeTake)
                    )
                )
            )
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(EcoCommand::executeSet)
                    )
                )
            )
            .then(Commands.literal("balance")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(EcoCommand::executeBalance)
                )
            )
        );
    }

    private static int executeGive(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            MultiCurrencyEconomyService economyService = org.night.nighteconomy.Nighteconomy.getInstance().getEconomyService();
            String cur = currencyId();

            var uuid = targetPlayer.getUUID();
            var name = targetPlayer.getName().getString();

            economyService.ensureAccountExists(uuid, cur, name);
            boolean ok = economyService.addBalance(uuid, cur, amount);

            if (ok) {
                String fmtAmount = economyService.formatAmount(cur, amount);
                double nb = economyService.getBalance(uuid, cur);
                String fmtBal = economyService.formatAmount(cur, nb);

                source.sendSuccess(() -> Component.literal("§aAdicionado " + fmtAmount +
                        " para " + name + ". Novo saldo: " + fmtBal), true);
                targetPlayer.sendSystemMessage(Component.literal("§aVocê recebeu " + fmtAmount + " de um administrador!"));
                return 1;
            } else {
                source.sendFailure(Component.literal("§cErro ao adicionar dinheiro!"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cErro ao executar comando: " + e.getMessage()));
            return 0;
        }
    }


    private static int executeTake(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            MultiCurrencyEconomyService economyService = org.night.nighteconomy.Nighteconomy.getInstance().getEconomyService();
            String cur = currencyId();

            var uuid = targetPlayer.getUUID();
            var name = targetPlayer.getName().getString();

            economyService.ensureAccountExists(uuid, cur, name);

            double bal = economyService.getBalance(uuid, cur);
            if (bal < amount) {
                source.sendFailure(Component.literal("§c" + name + " não tem saldo suficiente! Saldo atual: " +
                        economyService.formatAmount(cur, bal)));
                return 0;
            }

            boolean ok = economyService.subtractBalance(uuid, cur, amount);

            if (ok) {
                String fmtAmount = economyService.formatAmount(cur, amount);
                double nb = economyService.getBalance(uuid, cur);
                String fmtBal = economyService.formatAmount(cur, nb);

                source.sendSuccess(() -> Component.literal("§aRemovido " + fmtAmount +
                        " de " + name + ". Novo saldo: " + fmtBal), true);
                targetPlayer.sendSystemMessage(Component.literal("§c" + fmtAmount +
                        " foi removido da sua conta por um administrador!"));
                return 1;
            } else {
                source.sendFailure(Component.literal("§cErro ao remover dinheiro!"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cErro ao executar comando: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSet(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            MultiCurrencyEconomyService economyService = org.night.nighteconomy.Nighteconomy.getInstance().getEconomyService();
            String cur = currencyId();

            var uuid = targetPlayer.getUUID();
            var name = targetPlayer.getName().getString();

            economyService.ensureAccountExists(uuid, cur, name);

            boolean ok = economyService.setBalance(uuid, cur, amount);
            if (ok) {
                String fmtAmount = economyService.formatAmount(cur, amount);
                source.sendSuccess(() -> Component.literal("§aSaldo de " + name + " definido para: " + fmtAmount), true);
                targetPlayer.sendSystemMessage(Component.literal("§aSeu saldo foi definido para " + fmtAmount + " por um administrador!"));
                return 1;
            } else {
                source.sendFailure(Component.literal("§cErro ao definir saldo!"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cErro ao executar comando: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeBalance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            MultiCurrencyEconomyService economyService = org.night.nighteconomy.Nighteconomy.getInstance().getEconomyService();
            String cur = currencyId();

            var uuid = targetPlayer.getUUID();
            var name = targetPlayer.getName().getString();

            economyService.ensureAccountExists(uuid, cur, name);

            double balance = economyService.getBalance(uuid, cur);
            String formatted = economyService.formatAmount(cur, balance);

            source.sendSuccess(() -> Component.literal("§aSaldo de " + name + ": §f" + formatted), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cErro ao executar comando: " + e.getMessage()));
            return 0;
        }
    }

    private static String currencyId() {
        var cfg = org.night.nighteconomy.Nighteconomy.getInstance().getConfigManager();
        var all = cfg.getCurrencies();
        if (all.containsKey("money")) return "money";
        return all.isEmpty() ? "money" : all.keySet().iterator().next();
    }

}

