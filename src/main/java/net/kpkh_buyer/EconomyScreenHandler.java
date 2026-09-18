package net.kpkh_buyer;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class EconomyScreenHandler extends AbstractContainerMenu {

    public EconomyScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.ECONOMY_SCREEN_HANDLER, syncId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}