package net.kpkh_buyer;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BuyerScreenHandler extends AbstractContainerMenu {

    private final BuyerBlockEntity blockEntity;

    // Конструктор для сервера
    public BuyerScreenHandler(int syncId, Inventory playerInventory, BuyerBlockEntity blockEntity) {
        super(ModScreenHandlers.BUYER_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.addCustomSlots(blockEntity);
        this.addPlayerSlots(playerInventory);
    }

    // Конструктор для клиента
    public BuyerScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.BUYER_SCREEN_HANDLER, syncId);
        this.blockEntity = null;
        SimpleContainer dummy = new SimpleContainer(98);
        this.addCustomSlots(dummy);
        this.addPlayerSlots(playerInventory);
    }

    // Слоты блока: две сетки 7×7 по бокам
    private void addCustomSlots(net.minecraft.world.Container container) {
        // Левая сетка: 7×7 (слоты 0..48)
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                this.addSlot(new Slot(container, col + row * 7,
                        8 + col * 18, 18 + row * 18));
            }
        }
        // Правая сетка: 7×7 (слоты 49..97)
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                this.addSlot(new Slot(container, 49 + col + row * 7,
                        170 + col * 18, 18 + row * 18));
            }
        }
    }

    // Слоты игрока: инвентарь 3×9 + хотбар
    private void addPlayerSlots(Inventory playerInventory) {
        // Основной инвентарь (3 ряда) — центрирован
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        71 + col * 18, 156 + row * 18));
            }
        }
        // Хотбар — тоже центрирован
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    71 + col * 18, 214));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // Слоты 0..97 — это слоты блока, 98..124 — инвентарь игрока (без хотбара), 125..133 — хотбар
            if (index < 98) {
                // Из блока → в инвентарь игрока
                if (!this.moveItemStackTo(stack, 98, 134, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Из инвентаря → в блок
                if (!this.moveItemStackTo(stack, 0, 98, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity == null) return true;
        return this.blockEntity.stillValid(player);
    }
}