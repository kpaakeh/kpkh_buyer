package net.kpkh_buyer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class RuneSmithingRecipe extends SimpleSmithingRecipe {

    // ─── Кодеки ───
    public static final MapCodec<RuneSmithingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            Ingredient.CODEC.optionalFieldOf("template").forGetter(r -> r.template),
            Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
            Ingredient.CODEC.optionalFieldOf("addition").forGetter(r -> r.addition),
            Enchantment.CODEC.optionalFieldOf("enchantment").forGetter(r -> r.enchantment),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("unbreakable", false).forGetter(r -> r.unbreakable)
        ).apply(instance, RuneSmithingRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RuneSmithingRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, r -> r.template,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.base,
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, r -> r.addition,
            Enchantment.STREAM_CODEC.apply(ByteBufCodecs::optional), r -> r.enchantment,
            ByteBufCodecs.BOOL, r -> r.unbreakable,
            RuneSmithingRecipe::new
        );

    public static final RecipeSerializer<RuneSmithingRecipe> SERIALIZER =
        new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    // ─── Поля ───
    private final Recipe.CommonInfo commonInfo;
    private final Optional<Ingredient> template;
    private final Ingredient base;
    private final Optional<Ingredient> addition;
    private final Optional<Holder<Enchantment>> enchantment;
    private final boolean unbreakable;

    public RuneSmithingRecipe(Recipe.CommonInfo commonInfo,
                              Optional<Ingredient> template,
                              Ingredient base,
                              Optional<Ingredient> addition,
                              Optional<Holder<Enchantment>> enchantment,
                              boolean unbreakable) {
        super(commonInfo);
        this.commonInfo = commonInfo;
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.enchantment = enchantment;
        this.unbreakable = unbreakable;
    }

    // ─── Логика ───
    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        ItemStack baseStack = input.base();
        ItemStack additionStack = input.addition();

        if (baseStack.isEmpty() || additionStack.isEmpty()) return false;

        // Проверяем, что в addition лежит нужная руна
        if (this.addition.isPresent() && !this.addition.get().test(additionStack)) return false;

        // Проверяем, что baseStack подходит под base
        if (!this.base.test(baseStack)) return false;

        // Руна неразрушимости — не требует зачарования
        if (unbreakable) return true;

        // Обычная руна — проверяем, что на предмете есть нужное зачарование на макс. уровне
        if (enchantment.isEmpty()) return false;

        Holder<Enchantment> ench = enchantment.get();
        ItemEnchantments enchantments = baseStack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int currentLevel = enchantments.getLevel(ench);
        int maxLevel = ench.value().getMaxLevel();

        return currentLevel == maxLevel;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack result = input.base().copy();

        if (unbreakable) {
            result.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            return result;
        }

        if (enchantment.isEmpty()) return result;

        Holder<Enchantment> ench = enchantment.get();
        EnchantmentHelper.updateEnchantments(result, mutable -> {
            int currentLevel = mutable.getLevel(ench);
            int maxLevel = ench.value().getMaxLevel();
            mutable.set(ench, Math.min(currentLevel + 1, maxLevel + 1));
        });

        return result;
    }

    // ─── Обязательные методы SimpleSmithingRecipe ───
    @Override
    public Optional<Ingredient> templateIngredient() {
        return this.template;
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return this.addition;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(
                List.of(this.template, Optional.of(this.base), this.addition));
    }

    @Override
    public RecipeSerializer<? extends SimpleSmithingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}