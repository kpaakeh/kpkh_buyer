package net.kpkh_buyer.economy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class EconomyEvents {
    public static void register() {

        // Инициализируем БД при старте мира: <мир>/economy/economy.db
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path worldPath = server.getWorldPath(LevelResource.ROOT);
            Path economyDir = worldPath.resolve("economy");
            EconomyManager.initialize(economyDir);
        });

        // Закрываем БД при остановке мира
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            EconomyManager.shutdown();
        });

        // Запоминаем имя игрока при входе (нужно для /pay по нику)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            EconomyManager.registerName(
                    handler.getPlayer().getUUID(),
                    handler.getPlayer().getName().getString()
            );
        });
    }
}