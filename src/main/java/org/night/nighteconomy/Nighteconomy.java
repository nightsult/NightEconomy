package org.night.nighteconomy;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.night.nighteconomy.api.NightEconomyAPI;
import org.night.nighteconomy.api.NightEconomyAPIImpl;
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
    public static final String VERSION = "2.0.0";
    private static final Logger LOGGER = LogManager.getLogger();

    // Core components
    private ConfigManager configManager;
    private MultiCurrencyDatabaseManager databaseManager;
    private MultiCurrencyEconomyService economyService;
    private RankingManager rankingManager;
    private PlaceholderManager placeholderManager;
    private MultiCurrencyCommand commandManager;
    private NightEconomyAPIImpl apiImpl;

    // Static instance for API access
    private static org.night.nighteconomy.Nighteconomy instance;

    public Nighteconomy(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        // Register the setup method for modloading
        modEventBus.addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("NightEconomy v{} inicializando...", VERSION);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Configurando NightEconomy...");

        try {
            // Initialize configuration
            Path configDir = Paths.get("config", MODID);
            java.nio.file.Files.createDirectories(configDir);
            configManager = new ConfigManager(configDir);

            // Initialize database (abre conexão JDBC e injeta no DatabaseManager)
            Path databasePath = configDir.resolve("nighteconomy.db");
            java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + databasePath.toString());
            databaseManager = new MultiCurrencyDatabaseManager(conn);

            // Initialize ranking manager
            rankingManager = new RankingManager(databaseManager, configManager);

            // Initialize economy service
            economyService = new MultiCurrencyEconomyService(databaseManager, configManager);

            // Initialize placeholder manager
            placeholderManager = new PlaceholderManager(economyService, configManager);
            placeholderManager.registerPlaceholders();

            // Initialize command manager
            commandManager = new MultiCurrencyCommand(economyService, configManager);

            // Initialize API implementation
            apiImpl = new NightEconomyAPIImpl(economyService, configManager, placeholderManager, rankingManager);

            LOGGER.info("NightEconomy v{} configurado com sucesso!", VERSION);
        } catch (Exception e) {
            LOGGER.error("Falha ao configurar NightEconomy: ", e);
            // Deixa apiImpl nulo; handlers vão checar null para evitar NPE
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Servidor iniciando - Carregando NightEconomy...");

        try {
            // Ensure database is properly initialized
            if (databaseManager != null) {
                LOGGER.info("Banco de dados inicializado com sucesso!");
            } else {
                LOGGER.warn("DatabaseManager não inicializado.");
            }

            // Load configurations
            if (configManager != null) {
                configManager.loadConfigurations();
                LOGGER.info("Configurações carregadas com sucesso!");
            } else {
                LOGGER.warn("ConfigManager não inicializado.");
            }

            // Force initial ranking update for all currencies
            if (rankingManager != null) {
                rankingManager.forceUpdateAll();
                LOGGER.info("Rankings inicializados com sucesso!");
            }

            LOGGER.info("NightEconomy v{} carregado com sucesso no servidor!", VERSION);

        } catch (Exception e) {
            LOGGER.error("Erro ao carregar NightEconomy no servidor: ", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Servidor parando - Salvando dados do NightEconomy...");

        try {
            // Shutdown economy service
            if (economyService != null) {
                economyService.shutdown();
                LOGGER.info("Serviço de economia finalizado!");
            }

            // Close database connections
            if (databaseManager != null) {
                databaseManager.close();
                LOGGER.info("Conexões do banco de dados fechadas!");
            }

            LOGGER.info("NightEconomy v{} finalizado com sucesso!", VERSION);

        } catch (Exception e) {
            LOGGER.error("Erro ao finalizar NightEconomy: ", e);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registrando comandos do NightEconomy...");

        try {
            if (commandManager != null) {
                commandManager.register(event.getDispatcher());
                LOGGER.info("Comandos registrados com sucesso!");
            } else {
                LOGGER.warn("CommandManager não inicializado - comandos não registrados!");
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao registrar comandos: ", e);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        try {
            ServerPlayer p = (ServerPlayer) e.getEntity();

            // Evita NPE caso setup tenha falhado e API ainda não esteja pronta
            NightEconomyAPI api = Nighteconomy.getAPI();
            if (api == null) {
                LOGGER.warn("NightEconomy API não inicializada ainda; ignorando ensureAccountExists para {}", p.getGameProfile().getName());
                return;
            }

            ConfigManager cfgMgr = getConfigManager();
            if (cfgMgr == null || cfgMgr.getCurrencies().isEmpty()) {
                LOGGER.warn("ConfigManager não pronto ou sem moedas definidas; ignorando ensureAccountExists para {}", p.getGameProfile().getName());
                return;
            }

            String currency;
            if (cfgMgr.getCurrencies().containsKey("money")) {
                currency = "money";
            } else {
                // Pega a primeira moeda disponível
                currency = cfgMgr.getCurrencies().keySet().iterator().next();
            }

            api.ensureAccountExists(p.getUUID(), currency, p.getName().getString());
        } catch (Exception ex) {
            LOGGER.error("Erro no handler de login do NightEconomy: ", ex);
        }
    }

    // Static getters for API access
    public static org.night.nighteconomy.Nighteconomy getInstance() {
        return instance;
    }

    public static NightEconomyAPI getAPI() {
        return instance != null ? instance.apiImpl : null;
    }

    // Getters for components
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

    // Utility methods
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

    // Reload method for admin commands
    public void reloadMod() {
        LOGGER.info("Recarregando NightEconomy...");

        try {
            // Reload configurations
            if (configManager != null) {
                configManager.reloadConfigurations();
            }

            // Force ranking updates
            if (rankingManager != null) {
                rankingManager.forceUpdateAll();
            }

            LOGGER.info("NightEconomy recarregado com sucesso!");

        } catch (Exception e) {
            LOGGER.error("Erro ao recarregar NightEconomy: ", e);
        }
    }
}