package com.pathetictry.pricejoke;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Custom prices and favourites, saved to config/pricejoke.json. */
final class PriceStore {
    private static final Logger LOG = LoggerFactory.getLogger("pricejoke");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class Data {
        Map<String, Long> prices = new LinkedHashMap<>();
        List<String> favs = new ArrayList<>();
    }

    private static Data data = new Data();

    private PriceStore() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pricejoke.json");
    }

    static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Data d = GSON.fromJson(Files.readString(f), Data.class);
            if (d == null) return;
            if (d.prices == null) d.prices = new LinkedHashMap<>();
            if (d.favs == null) d.favs = new ArrayList<>();
            data = d;
        } catch (Exception e) {
            LOG.warn("Could not read pricejoke.json, starting empty", e);
        }
    }

    private static void save() {
        try {
            Files.writeString(file(), GSON.toJson(data));
        } catch (Exception e) {
            LOG.warn("Could not save pricejoke.json", e);
        }
    }

    static Long get(String id) {
        return data.prices.get(id);
    }

    static void set(String id, long price) {
        data.prices.put(id, price);
        save();
    }

    static void remove(String id) {
        data.prices.remove(id);
        save();
    }

    static void clear() {
        data.prices.clear();
        save();
    }

    static List<String> keys() {
        return new ArrayList<>(data.prices.keySet());
    }

    static boolean isFav(String id) {
        return data.favs.contains(id);
    }

    static void toggleFav(String id) {
        if (!data.favs.remove(id)) data.favs.add(id);
        save();
    }
}
