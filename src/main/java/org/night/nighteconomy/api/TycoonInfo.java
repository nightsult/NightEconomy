package org.night.nighteconomy.api;

import java.util.UUID;

/**
 * Informações do Tycoon de uma moeda.
 * uuid pode ser null se desconhecido; username pode ser null se não disponível.
 */
public class TycoonInfo {
    private final UUID uuid;
    private final String username;

    public TycoonInfo(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }
}