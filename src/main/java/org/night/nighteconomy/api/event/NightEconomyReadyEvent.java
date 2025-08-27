package org.night.nighteconomy.api.event;

import net.neoforged.bus.api.Event;
import org.night.nighteconomy.api.NightEconomyAPI;

/**
 * Event fired when the NightEconomy API is ready for consumption by other mods.
 */
public class NightEconomyReadyEvent extends Event {
    private final NightEconomyAPI api;

    public NightEconomyReadyEvent(NightEconomyAPI api) {
        this.api = api;
    }

    public NightEconomyAPI getAPI() {
        return api;
    }
}