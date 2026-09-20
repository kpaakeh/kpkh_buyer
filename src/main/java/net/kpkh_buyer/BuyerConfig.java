package net.kpkh_buyer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class BuyerConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("buyer_prices.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Одна группа предметов с ценой и режимом.
     *  mode = "buy"  → скупщик ПОКУПАЕТ (игрок продаёт, получает деньги)
     *  mode = "sell" → скупщик ПРОДАЁТ (игрок покупает, платит деньги)
     */
    public static class ItemEntry {
        public List<String> items = new ArrayList<>();
        public double price;
        public String mode = "buy";
    }

    /** Категория с иконкой, названием и предметами (макс. 18). */
    public static class Category {
        public String id;
        public String display_name;
        public String description;     // ← новое: описание для тултипа
        public String icon;            // fallback-иконка (item)
        public String icon_texture;    // ← новое: путь к PNG 32×16 (две иконки)
        public List<ItemEntry> items = new ArrayList<>();
    }

    public static class Root {
        public List<Category> categories = new ArrayList<>();
    }

    private static Root root = new Root();

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                root = GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), Root.class);
                if (root == null) root = new Root();
                if (root.categories == null) root.categories = new ArrayList<>();
                if (root.categories.isEmpty()) {
                    root = createDefault();
                    save();
                }
            } else {
                root = createDefault();
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            root = createDefault();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Root createDefault() {
        Root r = new Root();

        Category ores = new Category();
        ores.id = "ores";
        ores.display_name = "Руды";
        ores.icon = "minecraft:iron_ore";

        ItemEntry gold = new ItemEntry();
        gold.items = List.of(
                "minecraft:gold_ore",
                "minecraft:deepslate_gold_ore",
                "minecraft:nether_gold_ore"
        );
        gold.price = 25.0;
        gold.mode = "buy";
        ores.items.add(gold);

        ItemEntry diamond = new ItemEntry();
        diamond.items = List.of("minecraft:diamond");
        diamond.price = 200.0;
        diamond.mode = "sell";
        ores.items.add(diamond);

        r.categories.add(ores);

        Category misc = new Category();
        misc.id = "misc";
        misc.display_name = "Разное";
        misc.icon = "minecraft:stick";

        ItemEntry stick = new ItemEntry();
        stick.items = List.of("minecraft:stick");
        stick.price = 5.0;
        stick.mode = "buy";
        misc.items.add(stick);

        r.categories.add(misc);

        return r;
    }

    public static List<Category> getCategories() {
        return root.categories;
    }

    /** Находит запись по itemId, ищет во всех категориях. */
    public static ItemEntry findEntry(String itemId) {
        for (Category c : root.categories) {
            if (c.items == null) continue;
            for (ItemEntry e : c.items) {
                if (e.items != null && e.items.contains(itemId)) return e;
            }
        }
        return null;
    }

    /** Обратная совместимость — возвращает цену или -1. */
    public static double getPrice(String itemId) {
        ItemEntry e = findEntry(itemId);
        return e != null ? e.price : -1;
    }
}