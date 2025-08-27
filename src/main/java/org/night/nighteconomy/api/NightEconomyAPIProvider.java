package org.night.nighteconomy.api;

/**
 * Static provider to expose the NightEconomyAPI instance to other mods.
 * Set the instance once the API is initialized.
 */
public final class NightEconomyAPIProvider {
    private static volatile NightEconomyAPI INSTANCE;

    private NightEconomyAPIProvider() { }

    public static NightEconomyAPI get() {
        return INSTANCE;
    }

    public static void set(NightEconomyAPI api) {
        INSTANCE = api;
    }

    public static boolean isReady() {
        return INSTANCE != null;
    }
}