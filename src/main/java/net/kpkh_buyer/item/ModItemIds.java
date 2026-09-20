package net.kpkh_buyer.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.kpkh_buyer.Kpkh_buyer;

public class ModItemIds {
    public static final ResourceKey<Item> RUNE_EFFICIENCY = create("rune_of_efficiency");
    public static final ResourceKey<Item> RUNE_FORTUNE = create("rune_of_fortune");
    public static final ResourceKey<Item> RUNE_SHARPNESS = create("rune_of_sharpness");
    public static final ResourceKey<Item> RUNE_PROTECTION = create("rune_of_protection");
    public static final ResourceKey<Item> RUNE_FEATHER_FALLING = create("rune_of_feather_falling");
    public static final ResourceKey<Item> RUNE_THORNS = create("rune_of_thorns");
    public static final ResourceKey<Item> RUNE_LOOTING = create("rune_of_looting");
    public static final ResourceKey<Item> RUNE_UNBREAKING = create("rune_of_unbreaking");

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Kpkh_buyer.MOD_ID, name));
    }
}