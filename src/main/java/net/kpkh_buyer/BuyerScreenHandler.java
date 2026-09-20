package net.kpkh_buyer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.kpkh_buyer.economy.EconomyManager;

public class BuyerScreenHandler extends AbstractContainerMenu {

    private static final int GHOST_COUNT = 18; // 2 строки × 9 столбцов

    private final Inventory playerInventory;
    private final BuyerBlockEntity blockEntity;
    private final SimpleContainer ghostContainer = new SimpleContainer(GHOST_COUNT);
    private final SimpleContainerData data = new SimpleContainerData(1); // [0] = индекс категории

    public BuyerScreenHandler(int syncId, Inventory playerInventory, BuyerBlockEntity blockEntity) {
        super(ModScreenHandlers.BUYER_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.blockEntity = blockEntity;

        this.addDataSlots(data);
        addGhostSlots();
        addPlayerSlots(playerInventory);
        refillGhostSlots(data.get(0));
    }
    public void forceRefillLocal(int idx) {
        if (this.blockEntity != null) return; // только на клиенте
        refillGhostSlots(idx);
    }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        if (id == 0) refillGhostSlots(value);
    }
    public BuyerScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    public int getCurrentCategory() {
        return data.get(0);
    }

    /** Заполняет ghost-слоты предметами текущей категории */
    private void refillGhostSlots(int categoryIdx) {
        for (int i = 0; i < GHOST_COUNT; i++) {
            ghostContainer.setItem(i, ItemStack.EMPTY);
        }
        var cats = BuyerConfig.getCategories();
        if (cats.isEmpty()) return;
        if (categoryIdx < 0 || categoryIdx >= cats.size()) categoryIdx = 0;

        var cat = cats.get(categoryIdx);
        int count = Math.min(cat.items.size(), GHOST_COUNT);
        for (int i = 0; i < count; i++) {
            BuyerConfig.ItemEntry entry = cat.items.get(i);
            if (entry.items == null || entry.items.isEmpty()) continue;
            Identifier id = Identifier.tryParse(entry.items.get(0));
            if (id == null) continue;
            var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null) ghostContainer.setItem(i, new ItemStack(item));
        }
    }

    /** Вызывается когда игрок кликает по кнопке-вкладке (клиент → сервер) */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        var cats = BuyerConfig.getCategories();
        if (id >= 0 && id < cats.size()) {
            data.set(0, id);
            refillGhostSlots(id);
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    private void addGhostSlots() {
        // 2 ряда × 9 колонок
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new GhostSlot(ghostContainer, col + row * 9,
                        8 + col * 18, 26 + row * 18));
            }
        }
    }

    private void addPlayerSlots(Inventory inv) {
        // Основной инвентарь (3 ряда)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        // Хотбар
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId < 0 || slotId >= this.slots.size()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        // ─── Клик по ghost-слоту (0..17) — покупка у скупщика ───
        if (slotId < GHOST_COUNT && clickType == ContainerInput.PICKUP) {
            var cats = BuyerConfig.getCategories();
            int catIdx = data.get(0);
            if (catIdx < 0 || catIdx >= cats.size()) return;
            var cat = cats.get(catIdx);
            int idx = slotId;
            if (idx >= cat.items.size()) return;

            BuyerConfig.ItemEntry entry = cat.items.get(idx);
            if (!"sell".equals(entry.mode)) return; // если это buy — ничего не делаем

            if (!player.level().isClientSide()) {
                boolean ok = EconomyManager.withdraw(player.getUUID(), entry.price);
                if (!ok) return;
            
                Identifier id = Identifier.tryParse(entry.items.get(0));
                if (id == null) return;
                var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null) return;
            
                ItemStack give = new ItemStack(item);
                if (!player.getInventory().add(give)) {
                    net.minecraft.world.level.block.Block.popResource(
                            player.level(),
                            player.blockPosition(),
                            give
                    );
                }
            
                EconomyManager.registerName(player.getUUID(), player.getName().getString());
            }
            return;
        }

        // ─── Клик по слоту игрока (18..53) — продажа скупщику ───
        Slot slot = this.slots.get(slotId);
        if (slot.container instanceof Inventory
                && clickType == ContainerInput.PICKUP
                && slot.hasItem()) {

            ItemStack stack = slot.getItem();
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            BuyerConfig.ItemEntry entry = BuyerConfig.findEntry(id.toString());

            if (entry != null && "buy".equals(entry.mode)) {
                if (!player.level().isClientSide()) {
                    String reason = "sell";
                    if (this.blockEntity != null && this.blockEntity.getLevel() != null) {
                        var state = this.blockEntity.getLevel().getBlockState(
                                this.blockEntity.getBlockPos());
                        reason = "sell:" + state.getBlock().getDescriptionId();
                    }
                    EconomyManager.registerName(player.getUUID(), player.getName().getString());
                    EconomyManager.deposit(player.getUUID(),
                            entry.price * stack.getCount(), reason);
                    slot.set(ItemStack.EMPTY);
                    slot.setChanged();
                    this.broadcastChanges();
                }
                return;
            }
        }

        super.clicked(slotId, button, clickType, player);
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