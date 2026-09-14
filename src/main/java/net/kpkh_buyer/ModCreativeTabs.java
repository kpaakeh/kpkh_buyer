package net.kpkh_buyer;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
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
            .icon(() -> new ItemStack(kpkh_block.BUYER_ITEM)) // ← используем BlockItem
            .title(Component.translatable("itemGroup.kpkh_buyer.kpkh_tab"))
            .displayItems((params, output) -> {
                output.accept(kpkh_block.BUYER_ITEM); // ← передаём BlockItem, а не Block
            })
            .build();

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KPKH_TAB_KEY, KPKH_TAB);
    }
}