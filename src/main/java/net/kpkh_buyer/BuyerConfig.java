package net.kpkh_buyer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BuyerConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("buyer_prices.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static List<PriceEntry> prices = new ArrayList<>();

    public static class PriceEntry {
        public String item;
        public double price;

        public PriceEntry() {}
        public PriceEntry(String item, double price) {
            this.item = item;
            this.price = price;
        }
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                Type listType = new TypeToken<List<PriceEntry>>(){}.getType();
                prices = GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), listType);
                if (prices == null) prices = new ArrayList<>();
            } else {
                // Создаём файл с примером
                prices = List.of(
                        new PriceEntry("minecraft:diamond", 150.0),
                        new PriceEntry("minecraft:emerald", 100.0),
                        new PriceEntry("minecraft:iron_ingot", 25.0),
                        new PriceEntry("minecraft:gold_ingot", 40.0)
                );
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(prices));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static List<PriceEntry> getAllPrices() {
        return prices;
    }
    /** Возвращает цену предмета или -1, если он не покупается */
    public static double getPrice(String itemId) {
        for (PriceEntry entry : prices) {
            if (entry.item.equals(itemId)) {
                return entry.price;
            }
        }
        return -1;
    }
}