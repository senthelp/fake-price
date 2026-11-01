package com.pathetictry.pricejoke;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

record ItemEntry(Item item, String id, String name, String lname, ItemStack stack, boolean block) {
    private static List<ItemEntry> all;
    private static Map<String, ItemEntry> byId;

    static synchronized List<ItemEntry> all() {
        if (all == null) {
            List<ItemEntry> list = new ArrayList<>();
            Map<String, ItemEntry> map = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                ItemStack stack = new ItemStack(item);
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                String name = stack.getHoverName().getString();
                ItemEntry e = new ItemEntry(item, id, name, name.toLowerCase(Locale.ROOT), stack, item instanceof BlockItem);
                list.add(e);
                map.put(id, e);
            }
            list.sort(Comparator.comparing(ItemEntry::lname));
            all = list;
            byId = map;
        }
        return all;
    }

    static ItemEntry byId(String id) {
        all();
        return byId.get(id);
    }
}
