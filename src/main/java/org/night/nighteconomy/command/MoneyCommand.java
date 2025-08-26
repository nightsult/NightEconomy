package org.night.nighteconomy.command;

import org.night.nighteconomy.service.MultiCurrencyEconomyService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MoneyCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("money")
            .executes(MoneyCommand::executeMoney)
        );
        
        dispatcher.register(Commands.literal("bal")
            .executes(MoneyCommand::executeMoney)
        );
        
        dispatcher.register(Commands.literal("balance")
            .executes(MoneyCommand::executeMoney)
        );
    }

    private static int executeMoney(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores!"));
            return 0;
        }

        MultiCurrencyEconomyService economyService = org.night.nighteconomy.Nighteconomy.getInstance().getEconomyService();
        String cur = currencyId();

        var uuid = player.getUUID();
        var name = player.getName().getString();

        economyService.ensureAccountExists(uuid, cur, name);

        double balance = economyService.getBalance(uuid, cur);
        String formatted = economyService.formatAmount(cur, balance);

        player.sendSystemMessage(Component.literal("§aSeu saldo atual: §f" + formatted));
        return 1;
    }

    private static String currencyId() {
        var cfg = org.night.nighteconomy.Nighteconomy.getInstance().getConfigManager();
        var all = cfg.getCurrencies();
        if (all.containsKey("money")) return "money";
        return all.isEmpty() ? "money" : all.keySet().iterator().next();
    }

}

