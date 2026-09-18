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
        PayloadTypeRegistry.clientboundPlay().register(EconomySyncPayload.ID, EconomySyncPayload.CODEC);
    }

    public static void sendToPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        double balance = EconomyManager.getBalance(uuid);
    
        List<EconomyManager.TransactionRecord> all = EconomyManager.getHistory(uuid, 100);
        List<EconomySyncPayload.HistoryEntry> history = new ArrayList<>();
        int pos = 0, neg = 0;
        for (EconomyManager.TransactionRecord tx : all) {
            boolean positive = tx.to() != null && tx.to().equals(uuid);
    
            String type;
            String otherName = "";
            String reason = tx.reason();
    
            if ("sell".equals(reason)) {
                type = "sell";
            } else if (positive && tx.from() == null) {
                type = "deposit";
            } else if (!positive && tx.to() == null) {
                type = "withdraw";
            } else {
                type = "transfer";
                if (positive && tx.from() != null) {
                    otherName = EconomyManager.getName(tx.from());
                } else if (!positive && tx.to() != null) {
                    otherName = EconomyManager.getName(tx.to());
                }
            }
    
            if (positive && pos < 5) {
                history.add(new EconomySyncPayload.HistoryEntry(type, otherName, tx.amount(), true, tx.timestamp()));
                pos++;
            } else if (!positive && neg < 5) {
                history.add(new EconomySyncPayload.HistoryEntry(type, otherName, tx.amount(), false, tx.timestamp()));
                neg++;
            }
            if (pos >= 5 && neg >= 5) break;
        }
    
        List<EconomyManager.TopEntry> topRaw = EconomyManager.getTopBalances(50);
        List<EconomySyncPayload.TopEntry> top = new ArrayList<>();
        for (EconomyManager.TopEntry t : topRaw) {
            top.add(new EconomySyncPayload.TopEntry(t.uuid(), t.name(), t.balance()));
        }
    
        ServerPlayNetworking.send(player, new EconomySyncPayload(balance, history, top));
    }
}