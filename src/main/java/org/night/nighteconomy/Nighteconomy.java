package org.night.nighteconomy;

import org.night.nighteconomy.api.NightEconomyAPI;
import org.night.nighteconomy.api.NightEconomyAPIProvider;
import org.night.nighteconomy.api.data.RankEntry;
import org.night.nighteconomy.api.data.TycoonInfo;
import org.night.nighteconomy.api.event.NightEconomyReadyEvent;
import org.night.nighteconomy.command.MultiCurrencyCommand;
import org.night.nighteconomy.config.ConfigManager;
import org.night.nighteconomy.database.MultiCurrencyDatabaseManager;
import org.night.nighteconomy.placeholder.PlaceholderManager;
import org.night.nighteconomy.ranking.RankingManager;
import org.night.nighteconomy.service.MultiCurrencyEconomyService;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mod(org.night.nighteconomy.Nighteconomy.MODID)
public class Nighteconomy {
    public static final String MODID = "nighteconomy";
    public static final String VERSION = "3.3.0";
    private static final Logger LOGGER = LogManager.getLogger();

    private ConfigManager configManager;
    private MultiCurrencyDatabaseManager databaseManager;
    private MultiCurrencyEconomyService economyService;
    private RankingManager rankingManager;
    private PlaceholderManager placeholderManager;
    private MultiCurrencyCommand commandManager;

    private NightEconomyAPI api;

    private static Nighteconomy instance;
    private boolean apiPublished = false;

    public Nighteconomy(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("NightEconomy v{} starting...", VERSION);
    }

    private void setup(final FMLCommonSetupEvent event) {
        try {
            Path configDir = Paths.get("config", MODID);
            java.nio.file.Files.createDirectories(configDir);
            configManager = new ConfigManager(configDir);

            Path databasePath = configDir.resolve("nighteconomy.db");
            java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            databaseManager = new MultiCurrencyDatabaseManager(conn);

            rankingManager = new RankingManager(databaseManager, configManager);
            economyService = new MultiCurrencyEconomyService(databaseManager, configManager);

            placeholderManager = new PlaceholderManager(economyService, configManager);
            placeholderManager.registerPlaceholders();

            commandManager = new MultiCurrencyCommand(economyService, configManager);

            this.api = new DefaultNightEconomyAPI(economyService, rankingManager);

            NightEconomyAPIProvider.set(this.api);
            LOGGER.info("NightEconomy API set in provider.");

            LOGGER.info("NightEconomy v{} successfully configured!", VERSION);
        } catch (Exception e) {
            LOGGER.error("Failed to configure NightEconomy: ", e);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server Starting - Loading NightEconomy...");

        try {
            if (databaseManager != null) {
                LOGGER.info("Database initialized successfully!");
            } else {
                LOGGER.warn("Database not initialized.");
            }

            if (configManager != null) {
                configManager.loadConfigurations();
                LOGGER.info("Settings loaded successfully!");
            }

            if (rankingManager != null) {
                rankingManager.forceUpdateAll();
                LOGGER.info("Rankings successfully initialized!");
            }

            if (!apiPublished && api != null) {
                NightEconomyAPIProvider.set(api);
                NeoForge.EVENT_BUS.post(new NightEconomyReadyEvent(api));
                apiPublished = true;
                LOGGER.info("NightEconomyReadyEvent posted; API ready.");
            }

            LOGGER.info("NightEconomy v{} successfully uploaded to the server!", VERSION);
        } catch (Exception e) {
            LOGGER.error("Error loading NightEconomy on server: ", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server Stopping - Saving NightEconomy Data...");
        try {
            if (economyService != null) {
                economyService.shutdown();
            }
            if (databaseManager != null) {
                databaseManager.close();
            }
        } catch (Exception e) {
            LOGGER.error("Error terminating NightEconomy: ", e);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering NightEconomy Commands...");
        try {
            if (commandManager != null) {
                commandManager.register(event.getDispatcher());
                LOGGER.info("Commands registered successfully!");
            } else {
                LOGGER.warn("CommandManager not initialized - commands not registered!");
            }
        } catch (Exception e) {
            LOGGER.error("Error registering commands: ", e);
        }
    }

    public static Nighteconomy getInstance() {
        return instance;
    }

    public static NightEconomyAPI getAPI() {
        return instance != null ? instance.api : null;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MultiCurrencyDatabaseManager getDatabaseManager() { return databaseManager; }
    public MultiCurrencyEconomyService getEconomyService() { return economyService; }
    public RankingManager getRankingManager() { return rankingManager; }
    public PlaceholderManager getPlaceholderManager() { return placeholderManager; }
    public MultiCurrencyCommand getCommandManager() { return commandManager; }

    public String getModId() { return MODID; }
    public String getVersion() { return VERSION; }

    public boolean isLoaded() {
        return configManager != null &&
                databaseManager != null &&
                economyService != null &&
                rankingManager != null &&
                placeholderManager != null &&
                commandManager != null &&
                api != null;
    }

    public void reloadMod() {
        LOGGER.info("Reloding NightEconomy...");
        try {
            if (configManager != null) configManager.reloadConfigurations();
            if (rankingManager != null) rankingManager.forceUpdateAll();
            LOGGER.info("NightEconomy reload successfully");
        } catch (Exception e) {
            LOGGER.error("Error reloading NightEconomy: ", e);
        }
    }

    private static final class DefaultNightEconomyAPI implements NightEconomyAPI {
        private final MultiCurrencyEconomyService economyService;
        private final RankingManager rankingManager;

        DefaultNightEconomyAPI(MultiCurrencyEconomyService economyService, RankingManager rankingManager) {
            this.economyService = economyService;
            this.rankingManager = rankingManager;
        }

        @Override
        public Set<String> getAvailableCurrencies() {
            return economyService.getAvailableCurrencies();
        }

        @Override
        public BigDecimal getBalance(UUID playerId, String currencyId) {
            double balance = economyService.getBalance(playerId, currencyId);
            return BigDecimal.valueOf(balance);
        }

        @Override
        public TycoonInfo getCurrentTycoon(String currencyId) {
            MultiCurrencyDatabaseManager.RankingEntry top = rankingManager.getTopPlayer(currencyId);
            if (top == null) return null;

            UUID uuid = null;
            try {
                uuid = top.getUuid() != null ? UUID.fromString(top.getUuid()) : null;
            } catch (Exception ignored) {}

            String tag = rankingManager.getTycoonTag(currencyId);
            return new TycoonInfo(
                    uuid,
                    top.getUsername(),
                    BigDecimal.valueOf(top.getBalance()),
                    tag
            );
        }

        @Override
        public String getTycoonTag(String currencyId) {
            return rankingManager.getTycoonTag(currencyId);
        }

        @Override
        public List<RankEntry> getTopRanking(String currencyId, int limit) {
            List<MultiCurrencyDatabaseManager.RankingEntry> top = rankingManager.getTopPlayers(currencyId, limit);
            return top.stream()
                    .map(e -> {
                        UUID uuid = null;
                        try {
                            uuid = e.getUuid() != null ? UUID.fromString(e.getUuid()) : null;
                        } catch (Exception ignored) {}
                        return new RankEntry(
                                e.getPosition(),
                                uuid,
                                e.getUsername(),
                                BigDecimal.valueOf(e.getBalance())
                        );
                    })
                    .collect(Collectors.toList());
        }

        @Override
        public String formatAmount(String currencyId, BigDecimal amount) {
            if (amount == null) return "0";
            try {
                return economyService.formatAmount(currencyId, amount.doubleValue());
            } catch (Throwable t) {
                return amount.toPlainString();
            }
        }

        @Override
        public boolean tryDebit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
            if (playerId == null || currencyId == null || amount == null) return false;
            if (amount.signum() <= 0) return false;
            try {
                return economyService.subtractBalance(playerId, currencyId, amount.doubleValue());
            } catch (Throwable t) {
                return false;
            }
        }
    }
}