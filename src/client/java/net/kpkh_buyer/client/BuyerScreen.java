package net.kpkh_buyer.client;

import net.kpkh_buyer.BuyerConfig;
import net.kpkh_buyer.BuyerScreenHandler;
import net.kpkh_buyer.economy.EconomyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BuyerScreen extends AbstractContainerScreen<BuyerScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/buyer_gui.png");

    private static final Identifier ARROW_BUY = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/arrow_buy.png");
    private static final Identifier ARROW_SELL = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/arrow_sell.png");
    private static final int ARROW_SIZE = 5;

    private static final Identifier SCROLL_LEFT = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/arrow_scroll_left.png");
    private static final Identifier SCROLL_RIGHT = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/arrow_scroll_right.png");
    private static final int SCROLL_BTN_SIZE = 10;

    private static final Identifier TAB_NORMAL = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/tab_normal.png");
    private static final Identifier TAB_HOVERED = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/tab_hovered.png");
    private static final Identifier TAB_ACTIVE = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/tab_active.png");

    private static final int TAB_TEX_WIDTH = 64;
    private static final int TAB_TEX_HEIGHT = 16;
    private static final int TAB_EDGE_W = 4;

    private static final int TAB_HEIGHT = 16;
    private static final int TAB_GAP = 2;
    private static final int TAB_ICON_SIZE = 16;
    private static final int TAB_PAD = 4;

    private static final int WIN_W = 176;
    private static final int WIN_H = 166;

    // ─── Иконка валюты ───
    private static final Identifier CURRENCY_TEX = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/currency.png");
    private static final int CURRENCY_SIZE = 8;

    private static final int C_TOOLTIP_BG   = 0xF0100010;
    private static final int C_TOOLTIP_EDGE = 0xFF5000FF;

    private int tabX0;
    private int tabY;
    private int tabViewWidth;
    private int tabContentWidth;
    private int tabScroll = 0;

    private int[] tabXPositions = new int[0];
    private int[] tabWidths = new int[0];

    private BuyerConfig.Category hoveredTab = null;

    private static int lastScroll = 0;
    private boolean firstOpen = true;

    public BuyerScreen(BuyerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, WIN_W, WIN_H);
        this.titleLabelY = -1000;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.tabX0 = x + 4;
        this.tabY = y + 4;
        this.tabViewWidth = this.imageWidth - 8;

        rebuildTabLayout();

        if (firstOpen) {
            tabScroll = lastScroll;
            firstOpen = false;
            scrollToActive();
        }
    }

    private void scrollToActive() {
        int active = this.menu.getCurrentCategory();
        if (active < 0 || active >= tabXPositions.length) return;

        int tabStart = tabXPositions[active];
        int tabEnd = tabStart + tabWidths[active];

        int viewStart = tabScroll;
        int viewEnd = tabScroll + effectiveViewWidth();

        if (tabStart < viewStart) {
            tabScroll = tabStart;
        } else if (tabEnd > viewEnd) {
            tabScroll = tabEnd - effectiveViewWidth();
        }
        clampTabScroll();
        lastScroll = tabScroll;
    }

    private void rebuildTabLayout() {
        var cats = BuyerConfig.getCategories();
        tabXPositions = new int[cats.size()];
        tabWidths = new int[cats.size()];

        int cursor = 0;
        for (int i = 0; i < cats.size(); i++) {
            var cat = cats.get(i);
            String label = cat.display_name != null ? cat.display_name : cat.id;
            int textW = this.font.width(label);
            boolean hasIcon = cat.icon_texture != null && !cat.icon_texture.isEmpty();
            int w = textW + TAB_PAD * 2 + (hasIcon ? TAB_ICON_SIZE * 2 + 4 : 0);
            tabXPositions[i] = cursor;
            tabWidths[i] = w;
            cursor += w + TAB_GAP;
        }
        tabContentWidth = Math.max(0, cursor - TAB_GAP);
        clampTabScroll();
    }

    private void clampTabScroll() {
        int max = Math.max(0, tabContentWidth - effectiveViewWidth());
        if (tabScroll > max) tabScroll = max;
        if (tabScroll < 0) tabScroll = 0;
    }

    private boolean needsScroll() {
        return tabContentWidth > tabViewWidth;
    }

    private int effectiveViewWidth() {
        return needsScroll() ? tabViewWidth - SCROLL_BTN_SIZE * 2 : tabViewWidth;
    }

    private int viewClipLeft() {
        return tabX0 + (needsScroll() ? SCROLL_BTN_SIZE : 0);
    }

    private int viewClipRight() {
        return tabX0 + tabViewWidth - (needsScroll() ? SCROLL_BTN_SIZE : 0);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y,
                0, 0, this.imageWidth, this.imageHeight, WIN_W, WIN_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt) {
        hoveredTab = null;

        super.extractRenderState(g, mouseX, mouseY, pt);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // ─── Стрелки у ghost-слотов ───
        int active = this.menu.getCurrentCategory();
        var cats = BuyerConfig.getCategories();
        if (active >= 0 && active < cats.size()) {
            var cat = cats.get(active);
            for (int i = 0; i < Math.min(cat.items.size(), 18); i++) {
                BuyerConfig.ItemEntry e = cat.items.get(i);
                int col = i % 9;
                int row = i / 9;
                int sx = x + 8 + col * 18;
                int sy = y + 26 + row * 18;

                Identifier arrow = "sell".equals(e.mode) ? ARROW_SELL : ARROW_BUY;
                int ax = sx + 16 - ARROW_SIZE;
                int ay = sy - 1;

                g.blit(RenderPipelines.GUI_TEXTURED, arrow,
                        ax, ay, 0, 0, ARROW_SIZE, ARROW_SIZE, ARROW_SIZE, ARROW_SIZE);
            }
        }

        renderTabs(g, mouseX, mouseY);

        // ─── Тултип вкладки ───
        if (hoveredTab != null) {
            drawTabTooltip(g, hoveredTab, mouseX, mouseY);
        }

        // ─── Тултип предмета (ручной) ───
        drawItemTooltip(g, x, y, mouseX, mouseY);
    }

    private void renderTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        var cats = BuyerConfig.getCategories();
        if (cats.isEmpty()) return;

        int active = this.menu.getCurrentCategory();

        boolean needScroll = needsScroll();
        boolean canScrollLeft  = needScroll && tabScroll > 0;
        boolean canScrollRight = needScroll && tabScroll < tabContentWidth - effectiveViewWidth();

        int clipLeft = viewClipLeft();
        int clipRight = viewClipRight();

        g.enableScissor(clipLeft, tabY, clipRight, tabY + TAB_HEIGHT);

        int drawX = clipLeft - tabScroll;
        for (int i = 0; i < cats.size(); i++) {
            int w = tabWidths[i];
            int tX = drawX + tabXPositions[i];

            if (tX + w < clipLeft || tX > clipRight) continue;

            boolean isActive = (i == active);
            boolean hovered = mouseX >= tX && mouseX <= tX + w
                    && mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT
                    && mouseX >= clipLeft && mouseX <= clipRight;

            drawTab(g, tX, tabY, w, TAB_HEIGHT, isActive, hovered);

            var cat = cats.get(i);
            int contentX = tX + TAB_PAD;
            boolean hasIcon = cat.icon_texture != null && !cat.icon_texture.isEmpty();
            Identifier iconTex = hasIcon ? Identifier.tryParse(cat.icon_texture) : null;

            if (iconTex != null) {
                g.blit(RenderPipelines.GUI_TEXTURED, iconTex,
                        contentX, tabY,
                        0, 0, TAB_ICON_SIZE, TAB_ICON_SIZE,
                        TAB_ICON_SIZE * 2, TAB_ICON_SIZE);
                contentX += TAB_ICON_SIZE + 4;
            }

            String label = cat.display_name != null ? cat.display_name : cat.id;
            int textW = this.font.width(label);
            g.text(this.font, label, contentX, tabY + (TAB_HEIGHT - 8) / 2, 0xFFFFFFFF, true);
            contentX += textW + 4;

            if (iconTex != null) {
                g.blit(RenderPipelines.GUI_TEXTURED, iconTex,
                        contentX, tabY,
                        TAB_ICON_SIZE, 0, TAB_ICON_SIZE, TAB_ICON_SIZE,
                        TAB_ICON_SIZE * 2, TAB_ICON_SIZE);
            }

            if (hovered) hoveredTab = cat;
        }

        g.disableScissor();

        if (needScroll) {
            int leftBtnX = tabX0;
            int rightBtnX = tabX0 + tabViewWidth - SCROLL_BTN_SIZE;
            int btnY = tabY + (TAB_HEIGHT - SCROLL_BTN_SIZE) / 2;

            drawScrollButton(g, leftBtnX, btnY, canScrollLeft, true);
            drawScrollButton(g, rightBtnX, btnY, canScrollRight, false);
        }
    }

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, int h,
                         boolean active, boolean hovered) {
        Identifier tex;
        if (active) tex = TAB_ACTIVE;
        else if (hovered) tex = TAB_HOVERED;
        else tex = TAB_NORMAL;

        int edgeW = TAB_EDGE_W;

        if (w <= edgeW * 2) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex,
                    x, y, 0, 0, w, h, TAB_TEX_WIDTH, TAB_TEX_HEIGHT);
            return;
        }

        g.blit(RenderPipelines.GUI_TEXTURED, tex,
                x, y, 0, 0, edgeW, h, TAB_TEX_WIDTH, TAB_TEX_HEIGHT);
        g.blit(RenderPipelines.GUI_TEXTURED, tex,
                x + w - edgeW, y, TAB_TEX_WIDTH - edgeW, 0, edgeW, h,
                TAB_TEX_WIDTH, TAB_TEX_HEIGHT);

        int midW = w - edgeW * 2;
        for (int i = 0; i < midW; i++) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex,
                    x + edgeW + i, y, edgeW, 0, 1, h,
                    TAB_TEX_WIDTH, TAB_TEX_HEIGHT);
        }
    }

    private void drawScrollButton(GuiGraphicsExtractor g, int x, int y,
                                  boolean active, boolean left) {
        Identifier tex = left ? SCROLL_LEFT : SCROLL_RIGHT;
        g.blit(RenderPipelines.GUI_TEXTURED, tex,
                x, y, 0, 0,
                SCROLL_BTN_SIZE, SCROLL_BTN_SIZE,
                SCROLL_BTN_SIZE, SCROLL_BTN_SIZE);

        if (!active) {
            g.fill(x, y, x + SCROLL_BTN_SIZE, y + SCROLL_BTN_SIZE, 0x80000000);
        }
    }

    private void drawTabTooltip(GuiGraphicsExtractor g, BuyerConfig.Category cat,
                                int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        String title = cat.display_name != null ? cat.display_name : cat.id;
        lines.add("§6" + title);
        if (cat.description != null && !cat.description.isEmpty()) {
            lines.add("§7" + cat.description);
        }
        if (cat.items != null) {
            int buyCount = 0, sellCount = 0;
            for (var e : cat.items) {
                if ("sell".equals(e.mode)) sellCount++;
                else buyCount++;
            }
            lines.add("§7Продать скупщику: §e" + buyCount
                    + " §7| Купить: §a" + sellCount);
        }

        int maxW = 0;
        for (String s : lines) maxW = Math.max(maxW, this.font.width(s));
        int padding = 4;
        int lineH = 10;
        int boxW = maxW + padding * 2;
        int boxH = lines.size() * lineH + padding * 2;

        int tx = mouseX + 12;
        int ty = mouseY + 8;
        if (tx + boxW > this.width) tx = mouseX - boxW - 12;
        if (ty + boxH > this.height) ty = this.height - boxH;

        g.fill(tx, ty, tx + boxW, ty + boxH, C_TOOLTIP_BG);
        g.fill(tx, ty, tx + boxW, ty + 1, C_TOOLTIP_EDGE);
        g.fill(tx, ty + boxH - 1, tx + boxW, ty + boxH, C_TOOLTIP_EDGE);
        g.fill(tx, ty, tx + 1, ty + boxH, C_TOOLTIP_EDGE);
        g.fill(tx + boxW - 1, ty, tx + boxW, ty + boxH, C_TOOLTIP_EDGE);

        int lineY = ty + padding;
        for (String line : lines) {
            g.text(this.font, line, tx + padding, lineY, 0xFFFFFFFF, true);
            lineY += lineH;
        }
    }

    /**
     * Ручной тултип с иконкой валюты. Работает при наведении на:
     * - ghost-слот категории (всегда показывает режим и цену)
     * - слот инвентаря игрока, если предмет продаётся
     */
    private void drawItemTooltip(GuiGraphicsExtractor g, int x, int y,
        int mouseX, int mouseY) {
        Slot hovered = this.hoveredSlot;
        if (hovered == null || !hovered.hasItem()) return;

        ItemStack stack = hovered.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        int slotIndex = hovered.index;
        int ghostCount = 18;

        String mode = null;
        double price = 0;

        if (slotIndex < ghostCount) {
        var cats = BuyerConfig.getCategories();
        int catIdx = this.menu.getCurrentCategory();
        if (catIdx < 0 || catIdx >= cats.size()) return;
        var cat = cats.get(catIdx);
        if (slotIndex >= cat.items.size()) return;
        BuyerConfig.ItemEntry e = cat.items.get(slotIndex);
        mode = e.mode;
        price = e.price;
        } else {
        BuyerConfig.ItemEntry e = BuyerConfig.findEntry(id.toString());
        if (e == null) return;
        if (!"buy".equals(e.mode)) return;
        mode = e.mode;
        price = e.price;
        }

        String priceStr = EconomyManager.formatNumber(price);
        String name = stack.getHoverName().getString();

        List<String> lines = new ArrayList<>();
        lines.add("§f" + name);
        if ("buy".equals(mode)) {
        lines.add("§7Продать за:");
        lines.add("§a+" + priceStr + "  §7(за 1 шт)");
        } else {
        lines.add("§7Купить за:");
        lines.add("§c−" + priceStr + "  §7(за 1 шт)");
        }
        lines.add("§8Нажмите, чтобы " + ("buy".equals(mode) ? "продать" : "купить"));

        // Размеры: считаем ширину каждой строки, прибавляем место под иконку
        int padding = 4;
        int lineH = 10;
        int maxW = 0;
        for (String s : lines) {
        String clean = s.replaceAll("§.", "");
        int w = this.font.width(clean);
        // на строке с ценой (индекс 2) добавляем иконку
        maxW = Math.max(maxW, w);
        }
        // плюс иконка валюты
        maxW += CURRENCY_SIZE + 3;

        int boxW = maxW + padding * 2;
        int boxH = lines.size() * lineH + padding * 2;

        int tx = mouseX + 12;
        int ty = mouseY - 6;
        if (tx + boxW > this.width) tx = mouseX - boxW - 12;
        if (ty + boxH > this.height) ty = this.height - boxH;

        g.fill(tx, ty, tx + boxW, ty + boxH, C_TOOLTIP_BG);
        g.fill(tx, ty, tx + boxW, ty + 1, C_TOOLTIP_EDGE);
        g.fill(tx, ty + boxH - 1, tx + boxW, ty + boxH, C_TOOLTIP_EDGE);
        g.fill(tx, ty, tx + 1, ty + boxH, C_TOOLTIP_EDGE);
        g.fill(tx + boxW - 1, ty, tx + boxW, ty + boxH, C_TOOLTIP_EDGE);

        int lineY = ty + padding;
        for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        int color = 0xFFFFFFFF;
        if (line.startsWith("§7")) color = 0xFFAAAAAA;
        else if (line.startsWith("§a")) color = 0xFF55FF55;
        else if (line.startsWith("§c")) color = 0xFFFF5555;
        else if (line.startsWith("§8")) color = 0xFF777777;

        String clean = line.replaceAll("§.", "");

        // Строка с ценой (i == 2): "+ 500 [иконка]  (за 1 шт)"
        if (i == 2) {
        boolean isBuy = "buy".equals(mode);
        String prefix = isBuy ? "+" : "−";
        int prefixColor = isBuy ? 0xFF55FF55 : 0xFFFF5555;

        // знак
        g.text(this.font, prefix, tx + padding, lineY, prefixColor, true);
        int prefixW = this.font.width(prefix);

        // число
        int numX = tx + padding + prefixW + 1;
        g.text(this.font, priceStr, numX, lineY, prefixColor, true);
        int numW = this.font.width(priceStr);

        // иконка ПОСЛЕ числа
        g.blit(RenderPipelines.GUI_TEXTURED, CURRENCY_TEX,
        numX + numW + 3, lineY - 1,
        0, 0, CURRENCY_SIZE, CURRENCY_SIZE,
        CURRENCY_SIZE, CURRENCY_SIZE);

        // "(за 1 шт)" — после иконки
        int suffixX = numX + numW + 3 + CURRENCY_SIZE + 3;
        g.text(this.font, "§7(за 1 шт)", suffixX, lineY, 0xFFAAAAAA, true);
        } else {
        g.text(this.font, clean, tx + padding, lineY, color, true);
        }
        lineY += lineH;
        }
    }

        /** Отключаем ванильные тултипы, чтобы не рисовались поверх наших */
            /** Отключаем ванильные тултипы, чтобы не рисовались поверх наших */
    @Override
    public void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Пусто — все тултипы рисуем сами в drawItemTooltip
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseEvent,
                                boolean doubleClick) {
        double mouseX = mouseEvent.x();
        double mouseY = mouseEvent.y();

        if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT
                && mouseX >= tabX0 && mouseX <= tabX0 + tabViewWidth) {

            boolean needScroll = needsScroll();

            if (needScroll) {
                int leftBtnX = tabX0;
                int rightBtnX = tabX0 + tabViewWidth - SCROLL_BTN_SIZE;

                if (mouseX >= leftBtnX && mouseX <= leftBtnX + SCROLL_BTN_SIZE) {
                    if (tabScroll > 0) {
                        tabScroll = Math.max(0, tabScroll - 40);
                        lastScroll = tabScroll;
                    }
                    return true;
                }
                if (mouseX >= rightBtnX && mouseX <= rightBtnX + SCROLL_BTN_SIZE) {
                    int max = Math.max(0, tabContentWidth - effectiveViewWidth());
                    if (tabScroll < max) {
                        tabScroll = Math.min(max, tabScroll + 40);
                        lastScroll = tabScroll;
                    }
                    return true;
                }
            }

            int clipLeft = viewClipLeft();
            int drawX = clipLeft - tabScroll;
            var cats = BuyerConfig.getCategories();
            for (int i = 0; i < cats.size(); i++) {
                int tX = drawX + tabXPositions[i];
                int w = tabWidths[i];
                if (mouseX >= tX && mouseX <= tX + w
                        && mouseX >= clipLeft && mouseX <= viewClipRight()) {
                    this.menu.forceRefillLocal(i);
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(
                                this.menu.containerId, i);
                    }
                    return true;
                }
            }
            return true;
        }

        return super.mouseClicked(mouseEvent, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double scrollX, double scrollY) {
        if (mouseY >= tabY - 2 && mouseY <= tabY + TAB_HEIGHT + 2
                && mouseX >= tabX0 && mouseX <= tabX0 + tabViewWidth) {
            int max = Math.max(0, tabContentWidth - effectiveViewWidth());
            if (max > 0) {
                tabScroll = (int) Math.max(0, Math.min(max, tabScroll - scrollY * 20));
                lastScroll = tabScroll;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}