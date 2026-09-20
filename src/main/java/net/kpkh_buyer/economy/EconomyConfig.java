package net.kpkh_buyer.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class EconomyConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("economy.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String currencySymbol = "$";
    public static String currencyTexture = "kpkh_buyer:textures/gui/currency.png";

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                Map<String, Object> map = GSON.fromJson(
                        Files.newBufferedReader(CONFIG_PATH),
                        new TypeToken<Map<String, Object>>(){}.getType());
                if (map != null) {
                    if (map.get("currency_symbol") != null)
                        currencySymbol = map.get("currency_symbol").toString();
                    if (map.get("currency_texture") != null)
                        currencyTexture = map.get("currency_texture").toString();
                }
            } else {
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("currency_symbol", currencySymbol);
            map.put("currency_texture", currencyTexture);
            Files.writeString(CONFIG_PATH, GSON.toJson(map));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}