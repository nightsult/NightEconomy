package org.night.nighteconomy.api.event;

import net.neoforged.bus.api.Event;
import org.night.nighteconomy.api.NightEconomyAPI;

/**
 * Evento disparado quando a API está pronta para uso.
 * Ouvintes podem chamar NightEconomyAPIProvider.get() com segurança após este evento.
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