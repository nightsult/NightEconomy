package org.night.nighteconomy.api.event;

import net.neoforged.bus.api.Event;
import org.night.nighteconomy.api.NightEconomyAPI;

/**
 * Event fired when the API is ready for use.
 * Listeners can safely call NightEconomyAPIProvider.get() after this event.
 */
public final class NightEconomyReadyEvent extends Event {
    private final NightEconomyAPI api;

    public NightEconomyReadyEvent(NightEconomyAPI api) {
        this.api = api;
    }

    public NightEconomyAPI getApi() {
        return api;
    }
}