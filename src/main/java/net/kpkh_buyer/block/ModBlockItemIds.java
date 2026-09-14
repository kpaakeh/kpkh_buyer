package net.kpkh_buyer.block;

import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;

import net.kpkh_buyer.Kpkh_buyer;
import net.kpkh_buyer.kpkh_block;

public class ModBlockItemIds {
    public static final BlockItemId BUYER = create("buyer");

    private static BlockItemId create(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Kpkh_buyer.MOD_ID, name);
        return BlockItemId.create(id, id);
    }
}