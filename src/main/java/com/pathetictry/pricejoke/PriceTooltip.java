package com.pathetictry.pricejoke;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

/** Rewrites the number after "$" in a tooltip line. Only edits the client-side tooltip list. */
final class PriceTooltip {
    private static final Pattern NUM = Pattern.compile("^(\\s*)\\d[\\d,]*(?:\\.\\d+)?[KkMmBbTt]?");

    private static final class Part {
        final Style style;
        String text;

        Part(Style style, String text) {
            this.style = style;
            this.text = text;
        }
    }

    private PriceTooltip() {}

    static void apply(ItemStack stack, List<Component> lines) {
        Long price = PriceStore.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (price == null) return;
        String repl = PriceFormat.format(price);

        for (int i = 0; i < lines.size(); i++) {
            List<Part> parts = new ArrayList<>();
            lines.get(i).visit((style, text) -> {
                parts.add(new Part(style, text));
                return Optional.empty();
            }, Style.EMPTY);

            if (!rewrite(parts, repl)) continue;

            // Each piece keeps its own style, so the green $ and white number stay as they were.
            MutableComponent out = Component.empty();
            for (Part p : parts) out.append(Component.literal(p.text).withStyle(p.style));
            lines.set(i, out);
        }
    }

    private static boolean rewrite(List<Part> parts, String repl) {
        boolean seenDollar = false;
        for (Part p : parts) {
            int from = 0;
            if (!seenDollar) {
                int d = p.text.indexOf('$');
                if (d < 0) continue;
                seenDollar = true;
                from = d + 1;
            }
            Matcher m = NUM.matcher(p.text.substring(from));
            if (m.find()) {
                p.text = p.text.substring(0, from) + m.group(1) + repl + p.text.substring(from + m.end());
                return true;
            }
            if (!p.text.substring(from).isBlank()) return false;
        }
        return false;
    }
}
