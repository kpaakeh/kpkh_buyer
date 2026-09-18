package net.kpkh_buyer;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    public static final MenuType<BuyerScreenHandler> BUYER_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(Kpkh_buyer.MOD_ID, "buyer"),
            new MenuType<>(BuyerScreenHandler::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<EconomyScreenHandler> ECONOMY_SCREEN_HANDLER = Registry.register(
        BuiltInRegistries.MENU,
        Identifier.fromNamespaceAndPath(Kpkh_buyer.MOD_ID, "economy"),
        new MenuType<>(EconomyScreenHandler::new, FeatureFlags.VANILLA_SET)
    );
    public static void initialize() {}
}