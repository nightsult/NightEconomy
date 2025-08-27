package org.night.nighteconomy.api;

import java.util.Objects;

/**
 * Static access point to the API implementation.
 * The main mod sets {@link #set(NightEconomyAPI)} when ready.
 * Third-party mods use {@link #get()} after receiving the NightEconomyReadyEvent.
 */
public final class NightEconomyAPIProvider {
    private static volatile NightEconomyAPI INSTANCE;

    private NightEconomyAPIProvider() {}

    /**
     * Gets the current API instance.
     *
     * @return API instance
     * @throws IllegalStateException if the API has not yet been initialized
     */
    public static NightEconomyAPI get() {
        NightEconomyAPI ref = INSTANCE;
        if (ref == null) {
            throw new IllegalStateException("NightEconomyAPI not yet available.");
        }
        return ref;
    }

    /**
     * Defines the API implementation. Internal use of the mod.
     *
     * @param api concrete instance
     */
    public static void set(NightEconomyAPI api) {
        INSTANCE = Objects.requireNonNull(api, "api");
    }
}