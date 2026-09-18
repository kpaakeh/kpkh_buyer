package net.kpkh_buyer.client;

import net.fabricmc.api.ClientModInitializer;
import net.kpkh_buyer.EconomyScreenHandler;
import net.kpkh_buyer.ModScreenHandlers;
import net.minecraft.client.gui.screens.MenuScreens;

public class Kpkh_buyerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModScreenHandlers.BUYER_SCREEN_HANDLER, BuyerScreen::new);
        EconomyClientNetworking.register();
    }
}