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

public class PayCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(PayCommand::executePay)
                )
            )
        );
    }

    private static int executePay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores!"));
            return 0;
        }

        try {
            ServerPlayer receiver = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            if (sender.getUUID().equals(receiver.getUUID())) {
                sender.sendSystemMessage(Component.literal("§cVocê não pode pagar para si mesmo!"));
                return 0;
            }

            MultiCurrencyEconomyService economyService = org.night.nighteconomy.Nighteconomy.getInstance().getEconomyService();
            String cur = currencyId();

            var su = sender.getUUID();
            var ru = receiver.getUUID();

            economyService.ensureAccountExists(su, cur, sender.getName().getString());
            economyService.ensureAccountExists(ru, cur, receiver.getName().getString());

            // checa saldo
            double sb = economyService.getBalance(su, cur);
            if (sb < amount) {
                sender.sendSystemMessage(Component.literal("§cVocê não tem saldo suficiente! Saldo atual: " +
                        economyService.formatAmount(cur, sb)));
                return 0;
            }

            var result = economyService.payPlayer(su, ru, cur, amount);
            if (result.isSuccess()) {
                String fmt = economyService.formatAmount(cur, amount);
                sender.sendSystemMessage(Component.literal("§aVocê pagou " + fmt + " para " + receiver.getName().getString() + "!"));
                receiver.sendSystemMessage(Component.literal("§aVocê recebeu " + fmt + " de " + sender.getName().getString() + "!"));
                return 1;
            } else {
                sender.sendSystemMessage(Component.literal("§cErro ao processar pagamento: " + result.getMessage()));
                return 0;
            }
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

