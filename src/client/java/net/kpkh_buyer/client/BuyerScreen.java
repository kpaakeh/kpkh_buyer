package net.kpkh_buyer.client;

import net.kpkh_buyer.BuyerScreenHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class BuyerScreen extends AbstractContainerScreen<BuyerScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/buyer_gui.png");

    public BuyerScreen(BuyerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 304, 238);
        this.inventoryLabelX = 71; // центрируем подпись «Инвентарь»
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y,
                      0, 0, this.imageWidth, this.imageHeight, 304, 238);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Баланс игрока справа от подписи «Инвентарь»
        if (this.minecraft != null && this.minecraft.player != null) {
            double balance = me.andy.ecobal.api.EconomyManager.getPlayerBalance(this.minecraft.player.getUUID());
            String balanceText = me.andy.ecobal.api.EconomyManager.formatBalance(balance);

            int labelX = this.inventoryLabelX;
            int labelY = this.inventoryLabelY;
            int labelWidth = this.font.width(this.playerInventoryTitle);

            graphics.text(this.font, balanceText,
                    labelX + labelWidth + 12, labelY, 0xFFD700, true);
        }
    }
}