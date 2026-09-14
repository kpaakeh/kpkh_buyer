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

    private static BlockItem register(
            BlockItemId id,
            Function<BlockBehaviour.Properties, Block> blockFactory,
            BlockBehaviour.Properties properties
    ) {
        // Создаём и регистрируем блок
        Block block = blockFactory.apply(properties.setId(id.block()));
        Registry.register(BuiltInRegistries.BLOCK, id.block(), block);

        // Создаём и регистрируем предмет блока
        BlockItem blockItem = new BlockItem(
            block,
            new Item.Properties().setId(id.item())
        );
        return Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);
    }

    public static final BlockItem BUYER_ITEM = register(
        ModBlockItemIds.BUYER,
        Block::new,
        BlockBehaviour.Properties.of().sound(SoundType.IRON)
    );

    public static void initialize() {
    }
}