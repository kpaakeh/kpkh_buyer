package net.kpkh_buyer.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.kpkh_buyer.Kpkh_buyer;

public class ModItems {

    public static final Item RUNE_EFFICIENCY = register(ModItemIds.RUNE_EFFICIENCY);
    public static final Item RUNE_FORTUNE = register(ModItemIds.RUNE_FORTUNE);
    public static final Item RUNE_SHARPNESS = register(ModItemIds.RUNE_SHARPNESS);
    public static final Item RUNE_PROTECTION = register(ModItemIds.RUNE_PROTECTION);
    public static final Item RUNE_FEATHER_FALLING = register(ModItemIds.RUNE_FEATHER_FALLING);
    public static final Item RUNE_THORNS = register(ModItemIds.RUNE_THORNS);
    public static final Item RUNE_LOOTING = register(ModItemIds.RUNE_LOOTING);
    public static final Item RUNE_UNBREAKING = register(ModItemIds.RUNE_UNBREAKING);

    private static Item register(ResourceKey<Item> key) {
        Item.Properties props = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(net.minecraft.core.component.DataComponents.REPAIR_COST, 0)
                .fireResistant();
        Item item = new Item(props);
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    public static void initialize() {}
}