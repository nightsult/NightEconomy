package org.night.nighteconomy.command;

import org.night.nighteconomy.service.MultiCurrencyEconomyService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BalanceCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(BalanceCommand::executeBalanceOther)
            )
        );
        
        dispatcher.register(Commands.literal("bal")
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(BalanceCommand::executeBalanceOther)
            )
        );
    }

    private static int executeBalanceOther(CommandContext<CommandSourceStack> context) {
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

            source.sendSuccess(() -> Component.literal("§aBalancy " + name + ": §f" + formatted), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError executing command: " + e.getMessage()));
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

