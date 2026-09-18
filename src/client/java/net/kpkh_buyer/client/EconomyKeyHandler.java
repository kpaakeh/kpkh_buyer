package net.kpkh_buyer.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class EconomyKeyHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Пока открыт любой экран (чат, инвентарь, наш GUI) — не реагируем
            if (client.gui.screen() != null) return;
            if (client.player == null) return;

            // consumeClick() возвращает true один раз за нажатие
            while (EconomyKeys.OPEN_ECONOMY_KEY.consumeClick()) {
                client.player.connection.sendCommand("eco");
            }
        });
    }
}