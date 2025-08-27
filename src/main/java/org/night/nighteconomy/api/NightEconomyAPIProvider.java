package org.night.nighteconomy.api;

import java.util.Objects;

/**
 * Ponto de acesso estático à implementação da API.
 * O mod principal define {@link #set(NightEconomyAPI)} quando estiver pronto.
 * Mods terceiros usam {@link #get()} após receberem o NightEconomyReadyEvent.
 */
public final class NightEconomyAPIProvider {
    private static volatile NightEconomyAPI INSTANCE;

    private NightEconomyAPIProvider() {}

    /**
     * Obtém a instância atual da API.
     *
     * @return instância da API
     * @throws IllegalStateException se a API ainda não foi inicializada
     */
    public static NightEconomyAPI get() {
        NightEconomyAPI ref = INSTANCE;
        if (ref == null) {
            throw new IllegalStateException("NightEconomyAPI ainda não está disponível.");
        }
        return ref;
    }

    /**
     * Define a implementação da API. Uso interno do mod.
     *
     * @param api instância concreta
     */
    public static void set(NightEconomyAPI api) {
        INSTANCE = Objects.requireNonNull(api, "api");
    }
}