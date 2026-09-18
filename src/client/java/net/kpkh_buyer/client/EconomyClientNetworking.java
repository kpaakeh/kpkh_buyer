package net.kpkh_buyer.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kpkh_buyer.economy.net.EconomySyncPayload;
import net.minecraft.client.Minecraft;

public class EconomyClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EconomySyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = context.client();
                if (mc.player == null) return;

                EconomyScreen screen = EconomyScreen.getCurrent();
                if (screen == null) {
                    screen = new EconomyScreen(0, mc.player.getInventory());
                    mc.gui.setScreen(screen);
                }
                screen.updateData(payload);
            });
        });
    }
}