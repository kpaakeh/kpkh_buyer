package net.kpkh_buyer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import me.andy.ecobal.api.EconomyManager;

public class BuyerScreenHandler extends AbstractContainerMenu {

    // Призрачные слоты: используем заглушку-контейнер
    private static final Container GHOST_CONTAINER = createGhostContainer();
    private final Inventory playerInventory;

    private static Container createGhostContainer() {
        SimpleContainer container = new SimpleContainer(98);
        int i = 0;
        for (BuyerConfig.PriceEntry entry : BuyerConfig.getAllPrices()) {
            if (i >= 98) break;
            Identifier id = Identifier.tryParse(entry.item);
            if (id == null) continue;
            var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null) {
                container.setItem(i, new ItemStack(item));
            }
            i++;
        }
        return container;
    }

    // Конструктор для сервера
    public BuyerScreenHandler(int syncId, Inventory playerInventory, BuyerBlockEntity blockEntity) {
        super(ModScreenHandlers.BUYER_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.addGhostSlots(GHOST_CONTAINER);
        this.addPlayerSlots(playerInventory);
    }

    // Конструктор для клиента
    public BuyerScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.BUYER_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.addGhostSlots(GHOST_CONTAINER);
        this.addPlayerSlots(playerInventory);
    }

    private void addGhostSlots(Container container) {
        // Левая сетка 7×7
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                this.addSlot(new GhostSlot(container, col + row * 7,
                        8 + col * 18, 18 + row * 18));
            }
        }
        // Правая сетка 7×7
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                this.addSlot(new GhostSlot(container, 49 + col + row * 7,
                        170 + col * 18, 18 + row * 18));
            }
        }
    }

    private void addPlayerSlots(Inventory playerInventory) {
        // Основной инвентарь
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        71 + col * 18, 156 + row * 18));
            }
        }
        // Хотбар
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    71 + col * 18, 214));
        }
    }

    // Продажа по клику на предмет в инвентаре игрока
    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < this.slots.size() && clickType == ContainerInput.PICKUP) {
            Slot slot = this.slots.get(slotId);
            // Проверяем, что это слот инвентаря игрока (не призрачный)
            if (slot.container instanceof Inventory && slot.hasItem()) {
                ItemStack stack = slot.getItem();
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                double price = BuyerConfig.getPrice(id.toString());
                if (price > 0) {
                    if (!player.level().isClientSide()) {
                        EconomyManager.silentDeposit(player.getUUID(), price * stack.getCount());
                        slot.set(ItemStack.EMPTY);
                        slot.setChanged();
                        this.broadcastChanges();
                    }
                    return; // не выполняем стандартную логику перемещения
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // Shift+Click отключён для простоты
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}