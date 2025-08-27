package org.night.nighteconomy;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.night.nighteconomy.api.NightEconomyAPI;
import org.night.nighteconomy.api.NightEconomyAPIImpl;
import org.night.nighteconomy.api.NightEconomyAPIProvider;
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

import java.nio.file.Path;
import java.nio.file.Paths;

@Mod(org.night.nighteconomy.Nighteconomy.MODID)
public class Nighteconomy {
    public static final String MODID = "nighteconomy";
    public static final String VERSION = "3.1.0";
    private static final Logger LOGGER = LogManager.getLogger();

    private ConfigManager configManager;
    private MultiCurrencyDatabaseManager databaseManager;
    private MultiCurrencyEconomyService economyService;
    private RankingManager rankingManager;
    private PlaceholderManager placeholderManager;
    private MultiCurrencyCommand commandManager;
    private NightEconomyAPIImpl apiImpl;

    private static org.night.nighteconomy.Nighteconomy instance;

    // Controla publicação única do evento de API pronta
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
            java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + databasePath.toString());
            databaseManager = new MultiCurrencyDatabaseManager(conn);

            rankingManager = new RankingManager(databaseManager, configManager);

            economyService = new MultiCurrencyEconomyService(databaseManager, configManager);

            placeholderManager = new PlaceholderManager(economyService, configManager);
            placeholderManager.registerPlaceholders();

            commandManager = new MultiCurrencyCommand(economyService, configManager);

            apiImpl = new NightEconomyAPIImpl(economyService, configManager, placeholderManager, rankingManager);

            // Disponibiliza a API para outros mods o quanto antes
            if (!NightEconomyAPIProvider.isReady()) {
                NightEconomyAPIProvider.set(apiImpl);
                LOGGER.info("NightEconomy API instance set in provider (version {}).", apiImpl.getAPIVersion());
            }

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

            // Publica evento avisando que a API está pronta (uma única vez)
            if (!apiPublished && apiImpl != null) {
                // Certifica que o provider tem a instância
                if (!NightEconomyAPIProvider.isReady()) {
                    NightEconomyAPIProvider.set(apiImpl);
                }
                NeoForge.EVENT_BUS.post(new NightEconomyReadyEvent(apiImpl));
                apiPublished = true;
                LOGGER.info("NightEconomyReadyEvent posted. API is ready for external mods.");
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

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        try {
            ServerPlayer p = (ServerPlayer) e.getEntity();

            NightEconomyAPI api = Nighteconomy.getAPI();
            if (api == null) {
                return;
            }

            ConfigManager cfgMgr = getConfigManager();
            if (cfgMgr == null || cfgMgr.getCurrencies().isEmpty()) {
                return;
            }

            String currency;
            if (cfgMgr.getCurrencies().containsKey("money")) {
                currency = "money";
            } else {
                currency = cfgMgr.getCurrencies().keySet().iterator().next();
            }

            api.ensureAccountExists(p.getUUID(), currency, p.getName().getString());
        } catch (Exception ex) {
        }
    }

    public static org.night.nighteconomy.Nighteconomy getInstance() {
        return instance;
    }

    public static NightEconomyAPI getAPI() {
        return instance != null ? instance.apiImpl : null;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MultiCurrencyDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MultiCurrencyEconomyService getEconomyService() {
        return economyService;
    }

    public RankingManager getRankingManager() {
        return rankingManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public MultiCurrencyCommand getCommandManager() {
        return commandManager;
    }

    public NightEconomyAPIImpl getAPIImpl() {
        return apiImpl;
    }

    public String getModId() {
        return MODID;
    }

    public String getVersion() {
        return VERSION;
    }

    public boolean isLoaded() {
        return configManager != null &&
                databaseManager != null &&
                economyService != null &&
                rankingManager != null &&
                placeholderManager != null &&
                commandManager != null &&
                apiImpl != null;
    }

    public void reloadMod() {
        LOGGER.info("Reloding NightEconomy...");

        try {
            if (configManager != null) {
                configManager.reloadConfigurations();
            }

            if (rankingManager != null) {
                rankingManager.forceUpdateAll();
            }

            LOGGER.info("NightEconomy reload successfully");

        } catch (Exception e) {
            LOGGER.error("Error reloading NightEconomy: ", e);
        }
    }
}