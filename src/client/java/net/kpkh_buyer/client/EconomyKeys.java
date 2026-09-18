package net.kpkh_buyer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class EconomyKeys {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("kpkh_buyer", "economy")
    );

    public static final KeyMapping SHIFT_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.kpkh_buyer.shift",
                    InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_LSHIFT,
                    CATEGORY
            )
    );

    public static final KeyMapping CTRL_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.kpkh_buyer.ctrl",
                    InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_LCONTROL,
                    CATEGORY
            )
    );

    // ─── Клавиша открытия окна экономики (по умолчанию — K) ───
    public static final KeyMapping OPEN_ECONOMY_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.kpkh_buyer.open_economy",
                    InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_K,
                    CATEGORY
            )
    );

    public static void register() {}
}