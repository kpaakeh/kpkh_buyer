package net.kpkh_buyer.client;

import net.kpkh_buyer.EconomyScreenHandler;
import net.kpkh_buyer.economy.net.EconomySyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.PlayerSkin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EconomyScreen extends AbstractContainerScreen<EconomyScreenHandler> {

    private static EconomyScreen current;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    // Палитра
    private static final int C_BG          = 0xF0181822;
    private static final int C_PANEL       = 0xFF111119;
    private static final int C_PANEL_HEAD  = 0xFF232332;
    private static final int C_BORDER      = 0xFF3A3A50;
    private static final int C_BORDER_ACC  = 0xFF6A6A8A;
    private static final int C_TITLE       = 0xFFFFD966;
    private static final int C_HEADER      = 0xFFFFE8A0;
    private static final int C_TEXT        = 0xFFDDDDDD;
    private static final int C_MUTED       = 0xFF888899;
    private static final int C_INCOME      = 0xFF66DD66;
    private static final int C_EXPENSE     = 0xFFFF6B6B;
    private static final int C_GOLD        = 0xFFFFCC44;
    private static final int C_RANK        = 0xFFAAAAAA;

    private double balance = 0.0;
    private List<EconomySyncPayload.HistoryEntry> history = new ArrayList<>();
    private List<EconomySyncPayload.TopEntry> top = new ArrayList<>();

    private int topScroll = 0;
    private int filter = 0; // 0=all, 1=income, 2=expense

    private static final int TOP_VISIBLE = 7;
    private static final int HISTORY_MAX = 10;

    private EditBox nickField;
    private EditBox amountField;

    private Button filterAll;
    private Button filterIncome;
    private Button filterExpense;

    public EconomyScreen(int syncId, Inventory inventory) {
        super(new EconomyScreenHandler(syncId, inventory), inventory,
              Component.literal("Экономика"), 400, 220);
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;
        current = this;
    }

    public static EconomyScreen getCurrent() { return current; }

    @Override
    public void removed() {
        current = null;
        super.removed();
    }

    public void updateData(EconomySyncPayload payload) {
        this.balance = payload.balance();
        this.history = payload.history();
        this.top = payload.top();
        this.topScroll = 0;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2 - 200;
        int y = this.height / 2 - 110;

        // Фильтры
        filterAll = Button.builder(tr("filter.all"), b -> filter = 0)
                .bounds(x + 14, y + 62, 54, 14).build();
        filterIncome = Button.builder(tr("filter.income"), b -> filter = 1)
                .bounds(x + 72, y + 62, 54, 14).build();
        filterExpense = Button.builder(tr("filter.expense"), b -> filter = 2)
                .bounds(x + 130, y + 62, 54, 14).build();
        addRenderableWidget(filterAll);
        addRenderableWidget(filterIncome);
        addRenderableWidget(filterExpense);

        // Поля перевода (подняты)
        this.nickField = new EditBox(this.font, x + 14, y + 184, 120, 18, tr("nick"));
        this.nickField.setMaxLength(32);
        this.nickField.setHint(tr("nick"));
        addRenderableWidget(this.nickField);

        this.amountField = new EditBox(this.font, x + 140, y + 184, 80, 18, tr("amount"));
        this.amountField.setMaxLength(12);
        this.amountField.setHint(tr("amount"));
        addRenderableWidget(this.amountField);

        addRenderableWidget(Button.builder(tr("send"), b -> doTransfer())
                .bounds(x + 226, y + 184, 80, 18).build());
    }

    private Component tr(String key) {
        return Component.translatable("gui.kpkh_buyer.economy." + key);
    }

    private void doTransfer() {
        String nick = this.nickField.getValue().trim();
        String amount = this.amountField.getValue().trim();
        if (nick.isEmpty() || amount.isEmpty()) return;
        if (this.minecraft == null || this.minecraft.player == null) return;
        this.minecraft.player.connection.sendCommand("pay " + nick + " " + amount);
        this.nickField.setValue("");
        this.amountField.setValue("");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0 && topScroll + TOP_VISIBLE < top.size()) topScroll++;
        if (scrollY > 0 && topScroll > 0) topScroll--;
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int x2 = x + this.imageWidth;
        int y2 = y + this.imageHeight;

        // Фон
        g.fill(x, y, x2, y2, C_BG);

        // Шапка (заголовок + баланс)
        g.fill(x + 1, y + 1, x2 - 1, y + 42, C_PANEL_HEAD);
        // Нижняя панель
        g.fill(x + 1, y2 - 50, x2 - 1, y2 - 1, C_PANEL_HEAD);

        // Левая и правая панели
        g.fill(x + 10, y + 48, x + 198, y2 - 54, C_PANEL);
        g.fill(x + 204, y + 48, x2 - 10, y2 - 54, C_PANEL);

        // Рамка
        g.fill(x, y, x2, y + 1, C_BORDER_ACC);
        g.fill(x, y2 - 1, x2, y2, C_BORDER_ACC);
        g.fill(x, y, x + 1, y2, C_BORDER_ACC);
        g.fill(x2 - 1, y, x2, y2, C_BORDER_ACC);

        // Разделитель по центру
        g.fill(x + 200, y + 48, x + 202, y2 - 54, C_BORDER);

        // Разделители под шапкой и перед панелью перевода
        g.fill(x + 1, y + 42, x2 - 1, y + 43, C_BORDER);
        g.fill(x + 1, y2 - 51, x2 - 1, y2 - 50, C_BORDER);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt) {
        super.extractRenderState(g, mouseX, mouseY, pt);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int y2 = y + this.imageHeight;

        // Заголовок
        g.text(this.font, tr("title"), x + 14, y + 8, C_TITLE, true);

        // Баланс
        g.text(this.font, tr("balance"), x + 14, y + 24, C_MUTED, false);
        int balLabelW = this.font.width(tr("balance"));
        g.text(this.font, format(balance), x + 14 + balLabelW + 6, y + 24, C_GOLD, true);

        // Левая панель — заголовок
        g.text(this.font, tr("history"), x + 14, y + 50, C_HEADER, true);

        // Правая панель — заголовок
        g.text(this.font, tr("top"), x + 208, y + 50, C_HEADER, true);

        // История
        renderHistory(g, x, y);

        // Топ
        renderTop(g, x, y);

        // Перевод
        g.text(this.font, tr("transfer"), x + 14, y2 - 46, C_HEADER, true);
    }

    private void renderHistory(GuiGraphicsExtractor g, int x, int y) {
        List<EconomySyncPayload.HistoryEntry> filtered = getFilteredHistory();

        if (filtered.isEmpty()) {
            g.text(this.font, tr("no_operations"), x + 14, y + 82, C_MUTED, false);
            return;
        }

        int hy = y + 82;
        for (int i = 0; i < filtered.size() && i < HISTORY_MAX; i++) {
            EconomySyncPayload.HistoryEntry h = filtered.get(i);

            // Дата
            String date = DATE_FMT.format(
                    Instant.ofEpochMilli(h.timestamp()).atZone(ZoneId.systemDefault()));
            g.text(this.font, date, x + 14, hy, C_MUTED, false);

            // Причина
            Component reason = Component.translatable("gui.kpkh_buyer.economy.type." + h.type());
            String reasonStr = reason.getString();
            if (!h.otherName().isEmpty()) {
                String prep = Component.translatable(
                        "gui.kpkh_buyer.economy." + (h.positive() ? "from" : "to")).getString();
                reasonStr += " " + prep + " " + h.otherName();
            }
            g.text(this.font, reasonStr, x + 70, hy, C_TEXT, false);

            // Сумма (справа)
            int color = h.positive() ? C_INCOME : C_EXPENSE;
            String sign = h.positive() ? "+" : "−";
            String amountStr = sign + format(h.amount());
            int aw = this.font.width(amountStr);
            g.text(this.font, amountStr, x + 192 - aw, hy, color, true);

            hy += 11;
        }
    }

    private List<EconomySyncPayload.HistoryEntry> getFilteredHistory() {
        if (filter == 1) return history.stream().filter(EconomySyncPayload.HistoryEntry::positive).toList();
        if (filter == 2) return history.stream().filter(h -> !h.positive()).toList();
        return history;
    }

    private void renderTop(GuiGraphicsExtractor g, int x, int y) {
    if (top.isEmpty()) {
        g.text(this.font, tr("no_data"), x + 208, y + 82, C_MUTED, false);
        return;
    }

    int ty = y + 68;
    int shown = 0;
    for (int i = topScroll; i < top.size() && shown < TOP_VISIBLE; i++, shown++) {
        EconomySyncPayload.TopEntry e = top.get(i);

        // Место
        String rank = (i + 1) + ".";
        g.text(this.font, rank, x + 208, ty, C_RANK, false);

        // Ник
        g.text(this.font, e.name(), x + 226, ty, C_TEXT, false);

        // Баланс (справа)
        String balStr = format(e.balance());
        int bw = this.font.width(balStr);
        g.text(this.font, balStr, x + 392 - bw, ty, C_GOLD, false);

        ty += 14;
    }

    if (top.size() > TOP_VISIBLE) {
        g.text(this.font, tr("scroll_hint"), x + 208, this.height / 2 + 110 - 60, C_MUTED, false);
    }
}

    private static String format(double d) {
        return String.format("$%,.2f", d);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}