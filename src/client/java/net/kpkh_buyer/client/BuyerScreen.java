package net.kpkh_buyer.client;

import net.kpkh_buyer.BuyerConfig;
import net.kpkh_buyer.BuyerScreenHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class BuyerScreen extends AbstractContainerScreen<BuyerScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/buyer_gui.png");

    public BuyerScreen(BuyerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 304, 238);
        this.inventoryLabelX = 71;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y,
                      0, 0, this.imageWidth, this.imageHeight, 304, 238);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);

        if (stack.isEmpty()) return tooltip;

        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        double price = BuyerConfig.getPrice(id.toString());

        if (price > 0) {
            tooltip.add(Component.literal("§6Цена продажи: §e" 
                    + net.kpkh_buyer.economy.EconomyManager.format(price)));
            tooltip.add(Component.literal("§7Кликните по предмету в инвентаре, чтобы продать"));
        }
        return tooltip;
    }
}