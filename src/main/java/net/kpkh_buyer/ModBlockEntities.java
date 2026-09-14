package net.kpkh_buyer;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final BlockEntityType<BuyerBlockEntity> BUYER_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Kpkh_buyer.MOD_ID, "buyer"),
            FabricBlockEntityTypeBuilder.<BuyerBlockEntity>create(BuyerBlockEntity::new, kpkh_block.BUYER_BLOCK).build()
    );

    public static void initialize() {
    }
}