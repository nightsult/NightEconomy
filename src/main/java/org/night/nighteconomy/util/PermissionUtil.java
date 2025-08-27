package org.night.nighteconomy.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PermissionUtil {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ADMIN_NODE = "nighteconomy.admin";
    private static final int ADMIN_OP_LEVEL = 4;

    private static volatile boolean warnedNoProvider = false;

    private PermissionUtil() {}

    public static boolean has(CommandSourceStack source, String node) {
        if (node == null || node.isEmpty()) return true;

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return true;
        }

        if (canUseAnyCommand(source)) {
            return true;
        }

        try {
            Boolean lp = checkLuckPerms(player, node);
            if (lp != null) return lp;
        } catch (Throwable ignored) {}

        try {
            Boolean nf = checkNeoForgePermissionAPI(player, node);
            if (nf != null) return nf;
        } catch (Throwable ignored) {}

        try {
            Boolean fg = checkForgePermissionAPI(player, node);
            if (fg != null) return fg;
        } catch (Throwable ignored) {}

        logNoProviderOnce(node);
        return source.hasPermission(2);
    }

    public static boolean any(CommandSourceStack source, List<String> nodes) {
        if (nodes == null || nodes.isEmpty()) return true;
        for (String n : nodes) {
            if (has(source, n)) return true;
        }
        return false;
    }

    public static boolean canUseAnyCommand(CommandSourceStack source) {
        if (source.hasPermission(ADMIN_OP_LEVEL)) {
            return true;
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            Boolean admin = directCheckProviders(player, ADMIN_NODE);
            return Boolean.TRUE.equals(admin);
        }
        return false;
    }

    private static Boolean directCheckProviders(ServerPlayer player, String node) {
        try {
            Boolean lp = checkLuckPerms(player, node);
            if (lp != null) return lp;
        } catch (Throwable ignored) {}

        try {
            Boolean nf = checkNeoForgePermissionAPI(player, node);
            if (nf != null) return nf;
        } catch (Throwable ignored) {}

        try {
            Boolean fg = checkForgePermissionAPI(player, node);
            if (fg != null) return fg;
        } catch (Throwable ignored) {}

        return null;
    }

    private static Boolean checkLuckPerms(ServerPlayer player, String node) {
        try {
            Class<?> providerCls = Class.forName("net.luckperms.api.LuckPermsProvider");
            Method getMethod = providerCls.getMethod("get");
            Object api = getMethod.invoke(null);
            if (api == null) return false;

            try {
                Method getAdapter = api.getClass().getMethod("getPlayerAdapter", Class.class);
                Object adapter = getAdapter.invoke(api, ServerPlayer.class);
                Method hasPerm = adapter.getClass().getMethod("hasPermission", Object.class, String.class);
                Object res = hasPerm.invoke(adapter, player, node);
                if (res instanceof Boolean b) return b;
            } catch (NoSuchMethodException ignore) {
            }

            UUID uuid = player.getUUID();
            Method getUserManager = api.getClass().getMethod("getUserManager");
            Object userManager = getUserManager.invoke(api);
            Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUser.invoke(userManager, uuid);
            if (user == null) {
                return false;
            }
            Method getCachedData = user.getClass().getMethod("getCachedData");
            Object cachedData = getCachedData.invoke(user);
            Method getPermissionData = cachedData.getClass().getMethod("getPermissionData");
            Object permissionData = getPermissionData.invoke(cachedData);
            Method checkPermission = permissionData.getClass().getMethod("checkPermission", String.class);
            Object result = checkPermission.invoke(permissionData, node);
            Method asBoolean = result.getClass().getMethod("asBoolean");
            Object bool = asBoolean.invoke(result);
            if (bool instanceof Boolean b) return b;

            return false;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Boolean checkNeoForgePermissionAPI(ServerPlayer player, String node) {
        try {
            Class<?> apiCls = Class.forName("net.neoforged.neoforge.server.permission.PermissionAPI");
            Method hasPerm = apiCls.getMethod("hasPermission", ServerPlayer.class, String.class);
            Object res = hasPerm.invoke(null, player, node);
            return (res instanceof Boolean) ? (Boolean) res : null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Boolean checkForgePermissionAPI(ServerPlayer player, String node) {
        try {
            Class<?> apiCls = Class.forName("net.minecraftforge.server.permission.PermissionAPI");
            Method hasPerm = apiCls.getMethod("hasPermission", ServerPlayer.class, String.class);
            Object res = hasPerm.invoke(null, player, node);
            return (res instanceof Boolean) ? (Boolean) res : null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void logNoProviderOnce(String node) {
        if (!warnedNoProvider) {
            warnedNoProvider = true;
            LOGGER.warn("Nenhum provedor de permissões detectado (LuckPerms/PermissionAPI). " +
                    "Usando fallback de OP para nós configurados. Ex.: '{}'", Objects.toString(node, ""));
        }
    }
}