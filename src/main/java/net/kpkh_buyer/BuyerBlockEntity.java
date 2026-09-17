package net.kpkh_buyer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.kpkh_buyer.economy.EconomyManager;

public class BuyerBlockEntity extends BaseContainerBlockEntity {

    private NonNullList<ItemStack> items = NonNullList.withSize(98, ItemStack.EMPTY);

    public BuyerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUYER_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.kpkh_buyer.buyer");
    }

    // Абстрактный метод из BaseContainerBlockEntity — обязателен
    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new BuyerScreenHandler(syncId, playerInventory, this);
    }

    // Метод из MenuConstructor — должен быть public
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        this.setLastPlayer(player);
        return new BuyerScreenHandler(syncId, playerInventory, this);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }
    private net.minecraft.world.entity.player.Player lastPlayer;

    public void setLastPlayer(net.minecraft.world.entity.player.Player player) {
        this.lastPlayer = player;
    }

    public net.minecraft.world.entity.player.Player getLastPlayer() {
        return this.lastPlayer;
    }
    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && this.level != null && !this.level.isClientSide()) {
            // Получаем ID предмета
            net.minecraft.resources.Identifier itemId = 
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            String id = itemId.toString();

            double price = BuyerConfig.getPrice(id);
            if (price > 0 && this.lastPlayer != null) {
                // Начисляем деньги игроку
                EconomyManager.deposit(
                    this.lastPlayer.getUUID(), price * stack.getCount());
                // Не кладём предмет в слот — он "продан"
                this.setChanged();
                return;
            }
        }
        // Если предмет не покупается — кладём как обычно
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }
}