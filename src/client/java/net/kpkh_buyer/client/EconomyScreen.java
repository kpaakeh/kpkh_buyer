package net.kpkh_buyer.client;

import net.kpkh_buyer.EconomyScreenHandler;
import net.kpkh_buyer.economy.EconomyManager;
import net.kpkh_buyer.economy.net.EconomySyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EconomyScreen extends AbstractContainerScreen<EconomyScreenHandler> {

    private static EconomyScreen current;
    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FULL_FMT  =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/economy_gui.png");

    private static final Identifier CURRENCY_TEX = Identifier.fromNamespaceAndPath(
            "kpkh_buyer", "textures/gui/currency.png");
    private static final int CURRENCY_SIZE = 8;

    // ─── Палитра ───
    private static final int C_TEXT          = 0xFFFFFFFF;
    private static final int C_MUTED         = 0xFFBBBBBB;
    private static final int C_HEADER        = 0xFFFFAA00;
    private static final int C_GOLD          = 0xFFFFAA00;
    private static final int C_INCOME        = 0xFF55FF55;
    private static final int C_EXPENSE       = 0xFFFF5555;
    private static final int C_RANK          = 0xFFFFFFFF;
    private static final int C_SELF          = 0xFF55FFFF;
    private static final int C_ONLINE        = 0xFF44FF44;
    private static final int C_OFFLINE       = 0xFF666666;
    private static final int C_TOOLTIP_BG    = 0xF0100010;
    private static final int C_TOOLTIP_EDGE  = 0xFF5000FF;

    private double balance = 0.0;
    private List<EconomySyncPayload.HistoryEntry> history = new ArrayList<>();
    private List<EconomySyncPayload.TopEntry> top = new ArrayList<>();

    private int topScroll = 0;
    private int historyScroll = 0;
    private String filterType = "all";
    private String sortType = "date_desc";

    private static final int HISTORY_VISIBLE = 7;
    private static final int TOP_VISIBLE = 4;
    private static final int TOP_ROW_H = 22;

    private static final int HIST_PANEL_LEFT  = 26;
    private static final int HIST_PANEL_RIGHT = 230;
    private static final int TOP_PANEL_LEFT   = 268;
    private static final int TOP_PANEL_RIGHT  = 410;
    private static final int HIST_CENTER = (HIST_PANEL_LEFT + HIST_PANEL_RIGHT) / 2;
    private static final int TOP_CENTER  = (TOP_PANEL_LEFT + TOP_PANEL_RIGHT) / 2;

    private static final int MAX_NICK = 16;

    private static final List<String> nickHistory = new ArrayList<>();
    private static int nickHistoryCursor = -1;
    private static final int NICK_HISTORY_MAX = 10;

    private EditBox nickField;
    private EditBox amountField;
    private EditBox commentField;

    private Button filterButton;
    private Button sortButton;

    private static final String[] FILTER_KEYS = {"all", "sell", "deposit", "withdraw", "transfer"};
    private static final String[] FILTER_LANG = {
            "gui.kpkh_buyer.economy.filter.all",
            "gui.kpkh_buyer.economy.filter.sell",
            "gui.kpkh_buyer.economy.filter.deposit",
            "gui.kpkh_buyer.economy.filter.withdraw",
            "gui.kpkh_buyer.economy.filter.transfer"
    };
    private int filterIndex = 0;

    private static final String[] SORT_KEYS = {"date_desc", "date_asc", "amount_desc", "amount_asc", "name"};
    private static final String[] SORT_LANG = {
            "gui.kpkh_buyer.economy.sort.date_desc",
            "gui.kpkh_buyer.economy.sort.date_asc",
            "gui.kpkh_buyer.economy.sort.amount_desc",
            "gui.kpkh_buyer.economy.sort.amount_asc",
            "gui.kpkh_buyer.economy.sort.name"
    };
    private int sortIndex = 0;

    private static final int WIN_W = 420;
    private static final int WIN_H = 244;

    private EconomySyncPayload.HistoryEntry hoveredEntry = null;

    public EconomyScreen(int syncId, Inventory inventory) {
        super(new EconomyScreenHandler(syncId, inventory), inventory,
              Component.literal("Экономика"), WIN_W, WIN_H);
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
        this.historyScroll = 0;
    }

    // ─── Иконка валюты (число, затем иконка) ───
    private int currencyWidth(double amount) {
        String num = EconomyManager.formatNumber(amount);
        return this.font.width(num) + 3 + CURRENCY_SIZE;
    }

    private void drawCurrency(GuiGraphicsExtractor g, double amount, int x, int y,
                              int color, boolean shadow) {
        String num = EconomyManager.formatNumber(amount);
        g.text(this.font, num, x, y, color, shadow);
        g.blit(RenderPipelines.GUI_TEXTURED, CURRENCY_TEX,
                x + this.font.width(num) + 3, y - 1, 0, 0,
                CURRENCY_SIZE, CURRENCY_SIZE,
                CURRENCY_SIZE, CURRENCY_SIZE);
    }

    private void drawCurrencyRight(GuiGraphicsExtractor g, double amount, int rightX, int y,
                                   int color, boolean shadow) {
        int w = currencyWidth(amount);
        drawCurrency(g, amount, rightX - w, y, color, shadow);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2 - WIN_W / 2;
        int y = this.height / 2 - WIN_H / 2;

        int panelW = HIST_PANEL_RIGHT - HIST_PANEL_LEFT;
        int gap = 4;
        int btnW = (panelW - gap) / 2;
        int btnY = y + 62;
        int btnH = 14;

        filterButton = Button.builder(tr(FILTER_LANG[filterIndex]), b -> {
            filterIndex = (filterIndex + 1) % FILTER_KEYS.length;
            filterType = FILTER_KEYS[filterIndex];
            b.setMessage(tr(FILTER_LANG[filterIndex]));
            historyScroll = 0;
        }).bounds(x + HIST_PANEL_LEFT, btnY, btnW, btnH).build();
        addRenderableWidget(filterButton);

        sortButton = Button.builder(tr(SORT_LANG[sortIndex]), b -> {
            sortIndex = (sortIndex + 1) % SORT_KEYS.length;
            sortType = SORT_KEYS[sortIndex];
            b.setMessage(tr(SORT_LANG[sortIndex]));
            historyScroll = 0;
        }).bounds(x + HIST_PANEL_LEFT + btnW + gap, btnY, btnW, btnH).build();
        addRenderableWidget(sortButton);

        int inputsW = 120 + 6 + 70 + 6 + 80;
        int inputsX = x + (WIN_W - inputsW) / 2;

        this.nickField = new EditBox(this.font, inputsX, y + 198, 120, 16,
                tr("gui.kpkh_buyer.economy.hint.nick"));
        this.nickField.setMaxLength(32);
        this.nickField.setHint(tr("gui.kpkh_buyer.economy.hint.nick"));
        addRenderableWidget(this.nickField);

        this.amountField = new EditBox(this.font, inputsX + 126, y + 198, 70, 16,
                tr("gui.kpkh_buyer.economy.hint.amount"));
        this.amountField.setMaxLength(12);
        this.amountField.setHint(tr("gui.kpkh_buyer.economy.hint.amount"));
        addRenderableWidget(this.amountField);

        addRenderableWidget(Button.builder(tr("gui.kpkh_buyer.economy.send"), b -> doTransfer())
                .bounds(inputsX + 202, y + 198, 80, 16).build());

        this.commentField = new EditBox(this.font, inputsX, y + 218, 282, 16,
                Component.literal(""));
        this.commentField.setMaxLength(100);
        this.commentField.setHint(tr("gui.kpkh_buyer.economy.hint.comment"));
        addRenderableWidget(this.commentField);
    }

    private Component tr(String key) {
        return Component.translatable(key);
    }

    private double parseAmount(String input) {
        if (input == null || input.isEmpty()) return -1;
        String s = input.toLowerCase().trim().replace(",", ".");
        if (s.equals("max") || s.equals("all") || s.equals("все")) return balance;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void doTransfer() {
        String nick = this.nickField.getValue().trim();
        String amountRaw = this.amountField.getValue().trim();
        String comment = this.commentField.getValue().trim();
        if (nick.isEmpty() || amountRaw.isEmpty()) return;
        if (this.minecraft == null || this.minecraft.player == null) return;

        double parsed = parseAmount(amountRaw);
        if (parsed <= 0) return;

        String finalAmount = String.format(Locale.US, "%.2f", parsed);
        String cmd = "pay " + nick + " " + finalAmount;
        if (!comment.isEmpty()) cmd += " " + comment;
        this.minecraft.player.connection.sendCommand(cmd);

        addNickToHistory(nick);

        this.nickField.setValue("");
        this.amountField.setValue("");
        this.commentField.setValue("");
    }

    private void addNickToHistory(String nick) {
        nickHistory.remove(nick);
        nickHistory.add(0, nick);
        while (nickHistory.size() > NICK_HISTORY_MAX) {
            nickHistory.remove(nickHistory.size() - 1);
        }
        nickHistoryCursor = -1;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
        int keyCode = keyEvent.key();

        if (keyCode == 257 || keyCode == 335) {
            if ((this.nickField != null && this.nickField.isFocused())
                    || (this.amountField != null && this.amountField.isFocused())
                    || (this.commentField != null && this.commentField.isFocused())) {
                doTransfer();
                return true;
            }
        }
        if (this.nickField != null && this.nickField.isFocused()
                && !nickHistory.isEmpty()) {
            if (keyCode == 265) {
                if (nickHistoryCursor < nickHistory.size() - 1) nickHistoryCursor++;
                this.nickField.setValue(nickHistory.get(nickHistoryCursor));
                return true;
            }
            if (keyCode == 264) {
                if (nickHistoryCursor > 0) {
                    nickHistoryCursor--;
                    this.nickField.setValue(nickHistory.get(nickHistoryCursor));
                } else {
                    nickHistoryCursor = -1;
                    this.nickField.setValue("");
                }
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseEvent, boolean doubleClick) {
        double mouseX = mouseEvent.x();
        double mouseY = mouseEvent.y();

        if (super.mouseClicked(mouseEvent, doubleClick)) return true;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int shownTop = 0;
        for (int i = topScroll; i < top.size() && shownTop < TOP_VISIBLE; i++, shownTop++) {
            EconomySyncPayload.TopEntry e = top.get(i);
            int rowY = y + 68 + shownTop * TOP_ROW_H;
            int px = x + TOP_PANEL_LEFT;

            String rank = (i + 1) + ".";
            int headX = px + this.font.width(rank) + 3;
            int nickX = headX + 12;

            if (mouseX >= nickX && mouseX <= x + TOP_PANEL_RIGHT - 4
                    && mouseY >= rowY + 1 && mouseY <= rowY + 11) {
                if (EconomyKeys.SHIFT_KEY.isDown()) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(e.name());
                } else {
                    this.nickField.setValue(e.name());
                    this.nickField.setFocused(true);
                }
                return true;
            }
        }

        if (EconomyKeys.CTRL_KEY.isDown()) {
            List<EconomySyncPayload.HistoryEntry> filtered = getFilteredAndSortedHistory();
            int rowLeft = x + HIST_PANEL_LEFT;
            int rowRight = x + HIST_PANEL_RIGHT;
            int hy = y + 80;
            int shown = 0;
            for (int i = historyScroll; i < filtered.size() && shown < HISTORY_VISIBLE; i++, shown++) {
                if (mouseX >= rowLeft && mouseX <= rowRight
                        && mouseY >= hy - 1 && mouseY <= hy + 10) {
                    EconomySyncPayload.HistoryEntry h = filtered.get(i);
                    if (!h.otherName().isEmpty()) {
                        this.nickField.setValue(h.otherName());
                        this.amountField.setValue(String.format(Locale.US, "%.2f", h.amount()));
                        if (h.comment() != null && !h.comment().isEmpty()) {
                            this.commentField.setValue(h.comment());
                        }
                    }
                    return true;
                }
                hy += 12;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = (this.width - this.imageWidth) / 2;
        boolean overLeft = mouseX < x + TOP_PANEL_LEFT - 4;
        if (overLeft) {
            int size = getFilteredAndSortedHistory().size();
            if (scrollY < 0 && historyScroll + HISTORY_VISIBLE < size) historyScroll++;
            if (scrollY > 0 && historyScroll > 0) historyScroll--;
        } else {
            if (scrollY < 0 && topScroll + TOP_VISIBLE < top.size()) topScroll++;
            if (scrollY > 0 && topScroll > 0) topScroll--;
        }
        return true;
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
        hoveredEntry = null;

        super.extractRenderState(g, mouseX, mouseY, pt);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int cx = x + this.imageWidth / 2;

        // ─── Баланс (центр) ───
        String balLabel = tr("gui.kpkh_buyer.economy.balance").getString();
        int balLabelW = this.font.width(balLabel);
        int balValueW = currencyWidth(balance);
        int balTotalW = balLabelW + 6 + balValueW;
        int balX = cx - balTotalW / 2;
        g.text(this.font, balLabel, balX, y + 20, C_MUTED, true);
        drawCurrency(g, balance, balX + balLabelW + 6, y + 20, C_GOLD, true);

        // ─── Шапки панелей ───
        Component histTitle = tr("gui.kpkh_buyer.economy.history");
        int histW = this.font.width(histTitle);
        g.text(this.font, histTitle, x + HIST_CENTER - histW / 2, y + 49, C_HEADER, true);

        Component topTitle = tr("gui.kpkh_buyer.economy.top");
        int topW = this.font.width(topTitle);
        g.text(this.font, topTitle, x + TOP_CENTER - topW / 2, y + 49, C_HEADER, true);

        renderHistory(g, x, y, mouseX, mouseY);
        renderTop(g, x, y);

        // ─── «Перевод средств» ───
        Component trTitle = tr("gui.kpkh_buyer.economy.transfer");
        int trW = this.font.width(trTitle);
        g.text(this.font, trTitle, cx - trW / 2, y + 184, C_HEADER, true);

        // ─── Остаток после перевода ───
        String amountRaw = this.amountField != null ? this.amountField.getValue().trim() : "";
        if (!amountRaw.isEmpty() && this.balance > 0) {
            double parsed = parseAmount(amountRaw);
            if (parsed > 0) {
                double remaining = this.balance - parsed;
                String remLabel = tr("gui.kpkh_buyer.economy.hint.remaining").getString();
                int inputsW = 120 + 6 + 70 + 6 + 80;
                int inputsX = x + (WIN_W - inputsW) / 2;
                int amtX = inputsX + 126;
                int amtW = 70;

                int labelW = this.font.width(remLabel);
                int curW = currencyWidth(Math.max(0, remaining));
                int totalW = labelW + 4 + curW;
                int textX = amtX + (amtW - totalW) / 2;
                int color = remaining < 0 ? C_EXPENSE : C_MUTED;

                g.text(this.font, remLabel, textX, y + 188, color, false);
                drawCurrency(g, Math.max(0, remaining),
                        textX + labelW + 4, y + 188, color, false);
            }
        }

        if (hoveredEntry != null) {
            drawHistoryTooltip(g, hoveredEntry, mouseX, mouseY);
        }

        if (getFilteredAndSortedHistory().size() > HISTORY_VISIBLE) {
            g.text(this.font, "▼", x + HIST_CENTER - 3, y + 172, C_MUTED, false);
        }
        if (top.size() > TOP_VISIBLE) {
            g.text(this.font, "▼", x + TOP_CENTER - 3, y + 172, C_MUTED, false);
        }
    }

    private void renderHistory(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        List<EconomySyncPayload.HistoryEntry> filtered = getFilteredAndSortedHistory();

        if (filtered.isEmpty()) {
            Component empty = tr("gui.kpkh_buyer.economy.no_operations");
            int ew = this.font.width(empty);
            g.text(this.font, empty, x + HIST_CENTER - ew / 2, y + 82, C_MUTED, true);
            return;
        }

        int rowLeft = x + HIST_PANEL_LEFT;
        int rowRight = x + HIST_PANEL_RIGHT;

        int hy = y + 80;
        int shown = 0;
        for (int i = historyScroll; i < filtered.size() && shown < HISTORY_VISIBLE; i++, shown++) {
            EconomySyncPayload.HistoryEntry h = filtered.get(i);

            String dateShort = SHORT_FMT.format(
                    Instant.ofEpochMilli(h.timestamp()).atZone(ZoneId.systemDefault()));
            g.text(this.font, dateShort, rowLeft, hy, C_MUTED, true);
            int dateW = this.font.width(dateShort);

            String otherName = h.otherName();
            if (otherName.length() > MAX_NICK) {
                otherName = otherName.substring(0, MAX_NICK) + "…";
            }

            String reasonStr = tr("gui.kpkh_buyer.economy.type." + h.type()).getString();
            if (!otherName.isEmpty()) {
                if ("sell".equals(h.type())) {
                    reasonStr += " (" + Component.translatable(h.otherName()).getString() + ")";
                } else {
                    String prep = h.positive() ? "←" : "→";
                    reasonStr += " " + prep + " " + otherName;
                }
            }

            String sign = h.positive() ? "+" : "−";
            int signW = this.font.width(sign);
            int amountW = signW + currencyWidth(h.amount());
            int amountX = rowRight - amountW;

            int reasonX = rowLeft + dateW + 5;
            int reasonMaxW = amountX - 6 - reasonX;

            String drawnText = reasonStr;
            if (this.font.width(drawnText) > reasonMaxW) {
                drawnText = ellipsize(drawnText, reasonMaxW);
            }
            g.text(this.font, drawnText, reasonX, hy, C_TEXT, true);

            int color = h.positive() ? C_INCOME : C_EXPENSE;
            g.text(this.font, sign, amountX, hy, color, true);
            drawCurrency(g, h.amount(), amountX + signW, hy, color, true);

            if (mouseX >= rowLeft && mouseX <= rowRight
                    && mouseY >= hy - 1 && mouseY <= hy + 10) {
                hoveredEntry = h;
            }

            hy += 12;
        }
    }

    /** Ручной тултип с иконкой валюты */
    private void drawHistoryTooltip(GuiGraphicsExtractor g,
                                    EconomySyncPayload.HistoryEntry h,
                                    int mouseX, int mouseY) {
        String full = FULL_FMT.format(
                Instant.ofEpochMilli(h.timestamp()).atZone(ZoneId.systemDefault()));

        String fullReason = tr("gui.kpkh_buyer.economy.type." + h.type()).getString();
        if (!h.otherName().isEmpty()) {
            if ("sell".equals(h.type())) {
                fullReason += " (" + Component.translatable(h.otherName()).getString() + ")";
            } else {
                String prep = tr("gui.kpkh_buyer.economy."
                        + (h.positive() ? "from" : "to")).getString();
                fullReason += " " + prep + " " + h.otherName();
            }
        }

        String amountNum = EconomyManager.formatNumber(h.amount());
        String sign = h.positive() ? "+" : "−";
        String commentLine = (h.comment() != null && !h.comment().isEmpty())
                ? "«" + h.comment() + "»" : null;

        int padding = 4;
        int lineH = 10;

        int w1 = this.font.width(full);
        int w2 = this.font.width(fullReason);
        int w3 = this.font.width(sign) + 1 + this.font.width(amountNum) + 3 + CURRENCY_SIZE;
        int w4 = commentLine != null ? this.font.width(commentLine) : 0;
        int maxW = Math.max(Math.max(w1, w2), Math.max(w3, w4));

        int lineCount = 3 + (commentLine != null ? 1 : 0);
        int boxW = maxW + padding * 2;
        int boxH = lineCount * lineH + padding * 2;

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

        // Дата
        g.text(this.font, full, tx + padding, lineY, C_MUTED, true);
        lineY += lineH;

        // Причина
        g.text(this.font, fullReason, tx + padding, lineY, C_HEADER, true);
        lineY += lineH;

        // Сумма: знак + число + иконка
        int amountColor = h.positive() ? C_INCOME : C_EXPENSE;
        g.text(this.font, sign, tx + padding, lineY, amountColor, true);
        int signW = this.font.width(sign);
        int numX = tx + padding + signW + 1;
        g.text(this.font, amountNum, numX, lineY, amountColor, true);
        int numW = this.font.width(amountNum);
        g.blit(RenderPipelines.GUI_TEXTURED, CURRENCY_TEX,
                numX + numW + 3, lineY - 1, 0, 0,
                CURRENCY_SIZE, CURRENCY_SIZE,
                CURRENCY_SIZE, CURRENCY_SIZE);
        lineY += lineH;

        // Комментарий
        if (commentLine != null) {
            g.text(this.font, commentLine, tx + padding, lineY, C_MUTED, true);
        }
    }

    private String ellipsize(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        int ellipsisW = this.font.width(ellipsis);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = sb.toString() + text.charAt(i);
            if (this.font.width(next) + ellipsisW > maxWidth) break;
            sb.append(text.charAt(i));
        }
        return sb + ellipsis;
    }

    private List<EconomySyncPayload.HistoryEntry> getFilteredAndSortedHistory() {
        List<EconomySyncPayload.HistoryEntry> result = new ArrayList<>(history);
        if (!"all".equals(filterType)) {
            result.removeIf(h -> !h.type().equals(filterType));
        }
        switch (sortType) {
            case "date_asc" -> result.sort((a, b) -> Long.compare(a.timestamp(), b.timestamp()));
            case "amount_desc" -> result.sort((a, b) -> Double.compare(b.amount(), a.amount()));
            case "amount_asc" -> result.sort((a, b) -> Double.compare(a.amount(), b.amount()));
            case "name" -> result.sort((a, b) -> a.otherName().compareTo(b.otherName()));
            default -> result.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        }
        return result;
    }

    private void renderTop(GuiGraphicsExtractor g, int x, int y) {
        if (top.isEmpty()) {
            Component empty = tr("gui.kpkh_buyer.economy.no_data");
            int ew = this.font.width(empty);
            g.text(this.font, empty, x + TOP_CENTER - ew / 2, y + 82, C_MUTED, true);
            return;
        }

        String selfUuid = (this.minecraft != null && this.minecraft.player != null)
                ? this.minecraft.player.getUUID().toString() : "";

        int shown = 0;
        for (int i = topScroll; i < top.size() && shown < TOP_VISIBLE; i++, shown++) {
            EconomySyncPayload.TopEntry e = top.get(i);

            int rowY = y + 68 + shown * TOP_ROW_H;
            int px = x + TOP_PANEL_LEFT;

            String rank = (i + 1) + ".";
            g.text(this.font, rank, px, rowY + 1, C_RANK, true);

            int headX = px + this.font.width(rank) + 3;
            drawPlayerHead(g, e.uuid(), headX, rowY, 10);

            boolean online = isPlayerOnline(e.uuid());
            g.fill(headX + 8, rowY + 1, headX + 10, rowY + 3,
                    online ? C_ONLINE : C_OFFLINE);

            int nickX = headX + 12;

            String nick = e.name();
            if (nick.length() > MAX_NICK) {
                nick = nick.substring(0, MAX_NICK) + "…";
            }

            boolean isSelf = e.uuid().equals(selfUuid);
            g.text(this.font, nick, nickX, rowY + 1, isSelf ? C_SELF : C_TEXT, true);

            if (isSelf) {
                int nickW = this.font.width(nick);
                g.text(this.font, " (Вы)", nickX + nickW, rowY + 1, C_SELF, false);
            }

            // ← Сдвиг влево: с 4 на 16 пикселей от правого края панели
            drawCurrencyRight(g, e.balance(), x + TOP_PANEL_RIGHT - 16, rowY + 12, C_GOLD, true);
        }
    }

    private boolean isPlayerOnline(String uuidStr) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null) return false;
            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
            return mc.getConnection().getPlayerInfo(uuid) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void drawPlayerHead(GuiGraphicsExtractor g, String uuidStr, int x, int y, int size) {
        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
            Identifier texture = null;

            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info =
                        mc.getConnection().getPlayerInfo(uuid);
                if (info != null) {
                    net.minecraft.world.entity.player.PlayerSkin skin = info.getSkin();
                    if (skin != null && skin.body() != null) {
                        texture = skin.body().texturePath();
                    }
                }
            }
            if (texture == null) {
                texture = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
            }

            g.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x, y, 8, 8, size, size, 64, 64);
            g.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x, y, 40, 8, size, size, 64, 64);
        } catch (Exception ex) {
            g.fill(x, y, x + size, y + size, 0xFF666666);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}