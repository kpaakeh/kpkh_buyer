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
        // Если нужен tooltip — он отрисовывается автоматически в AbstractContainerScreen
    }
}