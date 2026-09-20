package net.kpkh_buyer;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.kpkh_buyer.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> KPKH_TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            Identifier.fromNamespaceAndPath(Kpkh_buyer.MOD_ID, "kpkh_tab")
    );

    public static final CreativeModeTab KPKH_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(kpkh_block.BUYER_ITEM))
            .title(Component.translatable("itemGroup.kpkh_buyer.kpkh_tab"))
            .displayItems((params, output) -> {
                // Блок скупщика
                output.accept(kpkh_block.BUYER_ITEM);

                // Руны
                output.accept(ModItems.RUNE_EFFICIENCY);
                output.accept(ModItems.RUNE_FORTUNE);
                output.accept(ModItems.RUNE_SHARPNESS);
                output.accept(ModItems.RUNE_PROTECTION);
                output.accept(ModItems.RUNE_FEATHER_FALLING);
                output.accept(ModItems.RUNE_THORNS);
                output.accept(ModItems.RUNE_LOOTING);
                output.accept(ModItems.RUNE_UNBREAKING);
            })
            .build();

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KPKH_TAB_KEY, KPKH_TAB);
    }
}