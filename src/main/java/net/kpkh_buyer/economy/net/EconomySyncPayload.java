package net.kpkh_buyer.economy.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record EconomySyncPayload(
        double balance,
        List<HistoryEntry> history,
        List<TopEntry> top
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EconomySyncPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("kpkh_buyer", "economy_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EconomySyncPayload> CODEC =
            StreamCodec.of(
                    // ─── Запись ───
                    (buf, payload) -> {
                        buf.writeDouble(payload.balance);
                        buf.writeInt(payload.history.size());
                        for (HistoryEntry e : payload.history) {
                            buf.writeUtf(e.type);
                            buf.writeUtf(e.otherName);
                            buf.writeDouble(e.amount);
                            buf.writeBoolean(e.positive);
                            buf.writeLong(e.timestamp);
                            buf.writeUtf(e.comment != null ? e.comment : "");
                        }
                        buf.writeInt(payload.top.size());
                        for (TopEntry e : payload.top) {
                            buf.writeUtf(e.uuid);
                            buf.writeUtf(e.name);
                            buf.writeDouble(e.balance);
                        }
                    },
                    // ─── Чтение ───
                    buf -> {
                        double balance = buf.readDouble();
                        int hsize = buf.readInt();
                        List<HistoryEntry> history = new ArrayList<>();
                        for (int i = 0; i < hsize; i++) {
                            String type = buf.readUtf();
                            String otherName = buf.readUtf();
                            double amount = buf.readDouble();
                            boolean positive = buf.readBoolean();
                            long timestamp = buf.readLong();
                            String comment = buf.readUtf();
                            history.add(new HistoryEntry(type, otherName, amount, positive, timestamp, comment));
                        }
                        int tsize = buf.readInt();
                        List<TopEntry> top = new ArrayList<>();
                        for (int i = 0; i < tsize; i++) {
                            top.add(new TopEntry(buf.readUtf(), buf.readUtf(), buf.readDouble()));
                        }
                        return new EconomySyncPayload(balance, history, top);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }

    public record HistoryEntry(String type, String otherName, double amount,
                               boolean positive, long timestamp, String comment) {}
    public record TopEntry(String uuid, String name, double balance) {}
}