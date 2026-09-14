package net.kpkh_buyer;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

public class BuyerScreenHandler extends AbstractContainerMenu {

    private final BuyerBlockEntity blockEntity;

    // Конструктор для сервера
    public BuyerScreenHandler(int syncId, Inventory playerInventory, BuyerBlockEntity blockEntity) {
        super(ModScreenHandlers.BUYER_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;

        // Слоты блока: 27 слотов (3 ряда по 9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(blockEntity, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Инвентарь игрока: 3 ряда по 9
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Хотбар игрока: 1 ряд
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // Конструктор для клиента (MenuType вызывает именно его)
    public BuyerScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.BUYER_SCREEN_HANDLER, syncId);
        this.blockEntity = null;

        // На клиенте предметы синхронизируются сервером,
        // поэтому достаточно создать контейнер-заглушку
        SimpleContainer dummy = new SimpleContainer(27);

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(dummy, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Базовая логика Shift+Click — можно оставить заглушкой
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        // На клиенте blockEntity == null, поэтому просто возвращаем true
        if (this.blockEntity == null) return true;
        return this.blockEntity.stillValid(player);
    }
}