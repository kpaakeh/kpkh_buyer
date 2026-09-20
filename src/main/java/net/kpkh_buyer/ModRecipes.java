package net.kpkh_buyer;

import net.kpkh_buyer.item.RuneSmithingRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {

    public static final RecipeSerializer<RuneSmithingRecipe> RUNE_SMITHING_SERIALIZER =
        Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Kpkh_buyer.MOD_ID, "rune_smithing"),
            RuneSmithingRecipe.SERIALIZER
        );

    public static void initialize() {}
}