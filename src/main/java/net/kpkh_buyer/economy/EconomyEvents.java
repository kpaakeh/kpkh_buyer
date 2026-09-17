package net.kpkh_buyer.economy;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class EconomyEvents {
    public static void register() {
        // Запоминаем имя игрока, когда он заходит — чтобы /pay работал по нику
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            EconomyManager.registerName(
                handler.getPlayer().getUUID(),
                handler.getPlayer().getName().getString()
            );
        });
    }
}