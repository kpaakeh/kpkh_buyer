package net.kpkh_buyer;

import java.util.function.Function;

import net.kpkh_buyer.block.ModBlockItemIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class kpkh_block {

    public static Block BUYER_BLOCK;
    public static BlockItem BUYER_ITEM = register(
        ModBlockItemIds.BUYER,
        BuyerBlock::new,
        BlockBehaviour.Properties.of()
            .sound(SoundType.IRON)
            .destroyTime(1.5f)
            .explosionResistance(1.5f)
            .requiresCorrectToolForDrops()
    );

    private static BlockItem register(
            BlockItemId id,
            Function<BlockBehaviour.Properties, Block> blockFactory,
            BlockBehaviour.Properties properties
    ) {
        Block block = blockFactory.apply(properties.setId(id.block()));
        Registry.register(BuiltInRegistries.BLOCK, id.block(), block);

        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(id.item()));
        Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);

        BUYER_BLOCK = block;
        return blockItem;
    }

    public static void initialize() {
        // Класс уже загружен, статика выполнилась при первом обращении
    }
}