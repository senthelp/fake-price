package com.pathetictry.pricejoke;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Flat black/gray item picker: set a fake price per item. Everything is drawn by hand. */
public class PriceScreen extends Screen {
    private static final int PW = 336, PH = 204, COLS = 9, ROWS = 6, SLOT = 18, LROWS = 5;
    private static final int BLACK = 0xFF000000, PANEL = 0xFF2B2B2B, SLOTBG = 0xFF3A3A3A, BORDER = 0xFF777777;
    private static final int BTN = 0xFF1A1A1A, BTN_ON = 0xFF555555, WHITE = 0xFFFFFFFF, GRAY = 0xFFAAAAAA, GREEN = 0xFF55FF55;

    private enum Tab {
        ALL("All"), BLOCKS("Blocks"), ITEMS("Items"), FAVS("Favs");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private Tab tab = Tab.ALL;
    private EditBox search, priceBox;
    private List<ItemEntry> view = List.of();
    private ItemEntry selected;
    private int gridScroll, listScroll;
    private String status = "Click an item, type a price, press SET";
    private int x0, y0, gx, gy, rx, rw, by;

    public PriceScreen() {
        super(Component.literal("Fake Price"));
    }

    @Override
    protected void init() {
        String oldSearch = search == null ? "" : search.getValue();
        String oldPrice = priceBox == null ? "" : priceBox.getValue();

        x0 = (width - PW) / 2;
        y0 = (height - PH) / 2;
        gx = x0 + 8;
        gy = y0 + 56;
        rx = gx + COLS * SLOT + 8;
        rw = x0 + PW - 8 - rx;
        by = y0 + 172;

        search = new EditBox(font, gx + 4, y0 + 22, PW - 24, 8, Component.literal("Search"));
        search.setBordered(false);
        search.setMaxLength(40);
        search.setHint(Component.literal("Search items..."));
        search.setValue(oldSearch);
        search.setResponder(s -> {
            gridScroll = 0;
            refilter();
        });

        priceBox = new EditBox(font, gx + 100, by + 4, 56, 8, Component.literal("Price"));
        priceBox.setBordered(false);
        priceBox.setMaxLength(18);
        priceBox.setHint(Component.literal("5m, 250k"));
        priceBox.setFilter(s -> s.matches("[0-9.,kmbtKMBT]*"));
        priceBox.setValue(oldPrice);

        addRenderableWidget(search);
        addRenderableWidget(priceBox);
        refilter();
    }

    private void refilter() {
        String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        view = ItemEntry.all().stream()
                .filter(e -> switch (tab) {
                    case ALL -> true;
                    case BLOCKS -> e.block();
                    case ITEMS -> !e.block();
                    case FAVS -> PriceStore.isFav(e.id());
                })
                .filter(e -> q.isEmpty() || e.lname().contains(q) || e.id().contains(q))
                .toList();
        gridScroll = Math.max(0, Math.min(gridScroll, gridRows() - ROWS));
    }

    private int gridRows() {
        return (view.size() + COLS - 1) / COLS;
    }

    private void select(ItemEntry e) {
        selected = e;
        Long p = PriceStore.get(e.id());
        priceBox.setValue(p == null ? "" : PriceFormat.format(p));
        status = "";
    }

    private void set() {
        if (selected == null) {
            status = "Pick an item first";
            return;
        }
        long v = PriceFormat.parse(priceBox.getValue());
        if (v < 0) {
            status = "Invalid price. Try 5m, 250k, 1b or 1,000";
            return;
        }
        PriceStore.set(selected.id(), v);
        status = selected.name() + " now shows $" + PriceFormat.format(v);
    }

    private void box(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void btn(GuiGraphics g, int x, int y, int w, int h, String t, boolean on) {
        box(g, x, y, w, h, on ? BTN_ON : BTN);
        g.drawString(font, t, x + (w - font.width(t)) / 2, y + (h - 8) / 2, WHITE, false);
    }

    private void bar(GuiGraphics g, int x, int y, int h, int total, int vis, int scroll) {
        if (total <= vis) return;
        int th = Math.max(8, h * vis / total);
        int ty = y + (h - th) * scroll / (total - vis);
        g.fill(x, ty, x + 2, ty + th, GRAY);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xA0000000);
        box(g, x0, y0, PW, PH, PANEL);
        g.drawString(font, "Fake Price", x0 + 8, y0 + 6, WHITE, false);
        String note = "screen only";
        g.drawString(font, note, x0 + PW - 8 - font.width(note), y0 + 6, GRAY, false);

        box(g, gx, y0 + 18, PW - 16, 16, BLACK);

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            btn(g, gx + i * 42, y0 + 38, 38, 14, tabs[i].label, tabs[i] == tab);
        }

        // Item grid
        box(g, gx - 1, gy - 1, COLS * SLOT + 2, ROWS * SLOT + 2, BLACK);
        ItemEntry hover = null;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int idx = (gridScroll + r) * COLS + c;
                if (idx >= view.size()) break;
                ItemEntry e = view.get(idx);
                int sx = gx + c * SLOT, sy = gy + r * SLOT;
                boolean h = in(mx, my, sx, sy, SLOT - 1, SLOT - 1);
                if (h) hover = e;
                g.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, h ? BTN_ON : SLOTBG);
                if (e == selected) {
                    g.fill(sx, sy, sx + SLOT - 1, sy + 1, WHITE);
                    g.fill(sx, sy + SLOT - 2, sx + SLOT - 1, sy + SLOT - 1, WHITE);
                    g.fill(sx, sy, sx + 1, sy + SLOT - 1, WHITE);
                    g.fill(sx + SLOT - 2, sy, sx + SLOT - 1, sy + SLOT - 1, WHITE);
                }
                g.renderItem(e.stack(), sx + 1, sy + 1);
                if (PriceStore.isFav(e.id())) g.drawString(font, "*", sx + SLOT - 8, sy, WHITE, false);
            }
        }
        bar(g, gx + COLS * SLOT + 3, gy, ROWS * SLOT, gridRows(), ROWS, gridScroll);

        // Changed items list
        List<String> keys = PriceStore.keys();
        listScroll = Math.max(0, Math.min(listScroll, keys.size() - LROWS));
        g.drawString(font, "Changed items (" + keys.size() + ")", rx, y0 + 44, GRAY, false);
        box(g, rx - 1, gy - 1, rw + 2, LROWS * SLOT + 2, BLACK);
        for (int i = 0; i < LROWS; i++) {
            int idx = listScroll + i;
            if (idx >= keys.size()) break;
            String id = keys.get(idx);
            ItemEntry e = ItemEntry.byId(id);
            int ry = gy + i * SLOT;
            g.fill(rx, ry, rx + rw - 4, ry + SLOT - 1, SLOTBG);
            if (e != null) g.renderItem(e.stack(), rx + 1, ry + 1);
            String name = e != null ? e.name() : id;
            g.drawString(font, font.plainSubstrByWidth(name, rw - 72), rx + 20, ry + 1, WHITE, false);
            g.drawString(font, "$", rx + 20, ry + 9, GREEN, false);
            g.drawString(font, PriceFormat.format(PriceStore.get(id)), rx + 26, ry + 9, WHITE, false);
            btn(g, rx + rw - 50, ry + 2, 44, 14, "REMOVE", false);
        }
        bar(g, rx + rw - 2, gy, LROWS * SLOT, keys.size(), LROWS, listScroll);
        btn(g, rx, gy + 94, rw, 14, "RESET ALL", false);

        // Editor row
        g.fill(gx, by, gx + SLOT - 1, by + SLOT - 1, SLOTBG);
        if (selected != null) g.renderItem(selected.stack(), gx + 1, by + 1);
        String sel = selected == null ? "Pick an item" : selected.name();
        g.drawString(font, font.plainSubstrByWidth(sel, 70), gx + 22, by + 4, selected == null ? GRAY : WHITE, false);
        box(g, gx + 96, by, 64, 16, BLACK);
        btn(g, gx + 164, by, 30, 16, "SET", false);
        btn(g, gx + 198, by, 38, 16, selected != null && PriceStore.isFav(selected.id()) ? "UNFAV" : "FAV", false);

        // Status line (hovered item name wins)
        if (hover != null) {
            g.drawString(font, hover.name() + "  " + hover.id(), gx, y0 + PH - 12, GRAY, false);
        } else {
            g.drawString(font, status, gx, y0 + PH - 12, WHITE, false);
        }

        search.render(g, mx, my, pt);
        priceBox.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        double mx = event.x(), my = event.y();
        boolean right = event.button() == 1;

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (in(mx, my, gx + i * 42, y0 + 38, 38, 14)) {
                tab = tabs[i];
                gridScroll = 0;
                refilter();
                return true;
            }
        }

        if (in(mx, my, gx, gy, COLS * SLOT, ROWS * SLOT)) {
            int idx = (gridScroll + (int) (my - gy) / SLOT) * COLS + (int) (mx - gx) / SLOT;
            if (idx < view.size()) {
                ItemEntry e = view.get(idx);
                if (right) {
                    PriceStore.toggleFav(e.id());
                    if (tab == Tab.FAVS) refilter();
                } else {
                    select(e);
                }
            }
            return true;
        }

        List<String> keys = PriceStore.keys();
        for (int i = 0; i < LROWS; i++) {
            int idx = listScroll + i;
            if (idx >= keys.size()) break;
            int ry = gy + i * SLOT;
            if (in(mx, my, rx + rw - 50, ry + 2, 44, 14)) {
                PriceStore.remove(keys.get(idx));
                status = "Removed";
                return true;
            }
            if (in(mx, my, rx, ry, rw - 4, SLOT - 1)) {
                ItemEntry e = ItemEntry.byId(keys.get(idx));
                if (e != null) select(e);
                return true;
            }
        }

        if (in(mx, my, rx, gy + 94, rw, 14)) {
            PriceStore.clear();
            status = "All prices reset";
            return true;
        }
        if (in(mx, my, gx + 164, by, 30, 16)) {
            set();
            return true;
        }
        if (in(mx, my, gx + 198, by, 38, 16)) {
            if (selected != null) {
                PriceStore.toggleFav(selected.id());
                if (tab == Tab.FAVS) refilter();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int d = (int) -Math.signum(scrollY);
        if (in(mx, my, gx, gy, COLS * SLOT, ROWS * SLOT)) {
            gridScroll = Math.max(0, Math.min(gridScroll + d, gridRows() - ROWS));
            return true;
        }
        if (in(mx, my, rx, gy, rw, LROWS * SLOT)) {
            listScroll = Math.max(0, Math.min(listScroll + d, PriceStore.keys().size() - LROWS));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
