package net.kpkh_buyer.economy.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kpkh_buyer.economy.EconomyManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EconomyNetworking {

    public static void registerCommon() {
        PayloadTypeRegistry.clientboundPlay().register(
                EconomySyncPayload.ID, EconomySyncPayload.CODEC);
    }

    public static void sendToPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        double balance = EconomyManager.getBalance(uuid);

        // ─── История ───
        List<EconomyManager.TransactionRecord> all = EconomyManager.getHistory(uuid, 500);
        List<EconomySyncPayload.HistoryEntry> history = new ArrayList<>();

        for (EconomyManager.TransactionRecord tx : all) {
            boolean positive = tx.to() != null && tx.to().equals(uuid);
        
            String type;
            String otherName = "";
            String reason = tx.reason();
        
            if (reason != null && reason.startsWith("sell")) {
                type = "sell";
                if (reason.contains(":")) {
                    otherName = reason.substring(reason.indexOf(":") + 1); // descId блока
                }
            } else if (positive && tx.from() == null) {
                type = "deposit";
            } else if (!positive && tx.to() == null) {
                type = "withdraw";
            } else {
                type = "transfer";
                if (positive && tx.from() != null) otherName = EconomyManager.getName(tx.from());
                else if (!positive && tx.to() != null) otherName = EconomyManager.getName(tx.to());
            }
        
            history.add(new EconomySyncPayload.HistoryEntry(
                    type, otherName, tx.amount(), positive, tx.timestamp(),
                    tx.comment() != null ? tx.comment() : ""));
        }

        // ─── Топ ───
        List<EconomyManager.TopEntry> topRaw = EconomyManager.getTopBalances(50);
        List<EconomySyncPayload.TopEntry> top = new ArrayList<>();
        for (EconomyManager.TopEntry t : topRaw) {
            top.add(new EconomySyncPayload.TopEntry(t.uuid(), t.name(), t.balance()));
        }

        ServerPlayNetworking.send(player, new EconomySyncPayload(balance, history, top));
    }
}