package net.kpkh_buyer.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("kpkh_economy.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<UUID, Double> balances = new HashMap<>();
    private static final Map<UUID, String> names = new HashMap<>();

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                Type type = new TypeToken<Map<String, Double>>(){}.getType();
                Map<String, Double> raw = GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), type);
                balances.clear();
                if (raw != null) {
                    raw.forEach((k, v) -> balances.put(UUID.fromString(k), v));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Map<String, Double> raw = new HashMap<>();
            balances.forEach((k, v) -> raw.put(k.toString(), v));
            Files.writeString(CONFIG_PATH, GSON.toJson(raw));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }

    public static void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
        save();
    }

    public static void deposit(UUID uuid, double amount) {
        if (amount <= 0) return;
        balances.merge(uuid, amount, Double::sum);
        save();
    }

    public static boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;
        double current = getBalance(uuid);
        if (current < amount) return false;
        balances.put(uuid, current - amount);
        save();
        return true;
    }

    public static String format(double amount) {
        return String.format("$%,.2f", amount);
    }

    /** Запоминаем имя игрока при входе, чтобы /pay работал по нику и для офлайн-игроков */
    public static void registerName(UUID uuid, String name) {
        if (name != null && !name.isEmpty()) {
            names.put(uuid, name);
        }
    }

    /** Ищет UUID по нику: сначала среди зарегистрированных, потом создаёт offline-UUID */
    public static UUID resolveByName(String name) {
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        }
        return getOfflineUUID(name);
    }

    /** Стандартный Minecraft-способ создания UUID для офлайн-игрока */
    public static UUID getOfflineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
}