package net.kpkh_buyer.economy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EconomyManager {

    private static Connection connection;
    private static final Object LOCK = new Object();

    public static void initialize(Path economyDir) {
        synchronized (LOCK) {
            try {
                Files.createDirectories(economyDir);
                Path dbPath = economyDir.resolve("economy.db");
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
                createTables();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize economy DB", e);
            }
        }
    }

    public static void shutdown() {
        synchronized (LOCK) {
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            connection = null;
        }
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS balances (
                    uuid TEXT PRIMARY KEY,
                    balance REAL NOT NULL DEFAULT 0
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_uuid TEXT,
                    to_uuid TEXT,
                    amount REAL NOT NULL,
                    type TEXT NOT NULL,
                    reason TEXT,
                    comment TEXT,
                    timestamp INTEGER NOT NULL
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_names (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL
                )
            """);

            // Миграция: если БД уже была — добавить колонку comment
            try { stmt.execute("ALTER TABLE transactions ADD COLUMN comment TEXT"); }
            catch (SQLException ignored) {}

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_from ON transactions(from_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_to ON transactions(to_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_time ON transactions(timestamp)");
        }
    }

    // ---------- Базовые операции ----------

    public static double getBalance(UUID uuid) {
        synchronized (LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT balance FROM balances WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble(1);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return 0.0;
        }
    }

    public static void setBalance(UUID uuid, double amount) {
        synchronized (LOCK) {
            double clamped = Math.max(0, amount);
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO balances(uuid, balance) VALUES(?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance")) {
                ps.setString(1, uuid.toString());
                ps.setDouble(2, clamped);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static void deposit(UUID uuid, double amount) {
        deposit(uuid, amount, null);
    }

    public static void deposit(UUID uuid, double amount, String reason) {
        if (amount <= 0) return;
        synchronized (LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO balances(uuid, balance) VALUES(?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET balance = balance + excluded.balance")) {
                ps.setString(1, uuid.toString());
                ps.setDouble(2, amount);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
            logTransaction(null, uuid, amount, "deposit", reason, null);
        }
    }

    public static boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;
        synchronized (LOCK) {
            if (getBalance(uuid) < amount) return false;
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE balances SET balance = balance - ? WHERE uuid = ?")) {
                ps.setDouble(1, amount);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); return false; }
            logTransaction(uuid, null, amount, "withdraw", null, null);
            return true;
        }
    }

    // ---------- Переводы ----------

    public static boolean transfer(UUID from, UUID to, double amount, String reason) {
        return transfer(from, to, amount, reason, null);
    }

    public static boolean transfer(UUID from, UUID to, double amount, String reason, String comment) {
        if (amount <= 0 || from.equals(to)) return false;
        synchronized (LOCK) {
            if (getBalance(from) < amount) return false;
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE balances SET balance = balance - ? WHERE uuid = ?")) {
                    ps.setDouble(1, amount);
                    ps.setString(2, from.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO balances(uuid, balance) VALUES(?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET balance = balance + excluded.balance")) {
                    ps.setString(1, to.toString());
                    ps.setDouble(2, amount);
                    ps.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ignored) {}
                e.printStackTrace();
                return false;
            } finally {
                try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            logTransaction(from, to, amount, "transfer", reason, comment);
            return true;
        }
    }

    // ---------- Логирование ----------

    private static void logTransaction(UUID from, UUID to, double amount,
                                       String type, String reason, String comment) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO transactions(from_uuid, to_uuid, amount, type, reason, comment, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, from != null ? from.toString() : null);
            ps.setString(2, to != null ? to.toString() : null);
            ps.setDouble(3, amount);
            ps.setString(4, type);
            ps.setString(5, reason);
            ps.setString(6, comment);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ---------- История ----------

    public static List<TransactionRecord> getHistory(UUID uuid, int limit) {
        List<TransactionRecord> list = new ArrayList<>();
        synchronized (LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, from_uuid, to_uuid, amount, type, reason, comment, timestamp " +
                    "FROM transactions WHERE from_uuid = ? OR to_uuid = ? " +
                    "ORDER BY timestamp DESC LIMIT ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, uuid.toString());
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String f = rs.getString("from_uuid");
                        String t = rs.getString("to_uuid");
                        list.add(new TransactionRecord(
                                rs.getLong("id"),
                                f != null ? UUID.fromString(f) : null,
                                t != null ? UUID.fromString(t) : null,
                                rs.getDouble("amount"),
                                rs.getString("type"),
                                rs.getString("reason"),
                                rs.getString("comment"),
                                rs.getLong("timestamp")
                        ));
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return list;
    }

    public record TransactionRecord(long id, UUID from, UUID to, double amount,
                                    String type, String reason, String comment, long timestamp) {}

    // ---------- Утилиты ----------

    public static String format(double amount) {
        return String.format("%s%,.2f", EconomyConfig.currencySymbol, amount);
    }

    public static void registerName(UUID uuid, String name) {
        if (name == null || name.isEmpty()) return;
        synchronized (LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO player_names(uuid, name) VALUES(?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static String getName(UUID uuid) {
        synchronized (LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT name FROM player_names WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString(1);
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        // Полный UUID вместо обрезки
        return uuid.toString();
    }

    public static UUID resolveByName(String name) {
        synchronized (LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid FROM player_names WHERE LOWER(name) = LOWER(?) LIMIT 1")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return UUID.fromString(rs.getString(1));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return getOfflineUUID(name);
    }

    public static UUID getOfflineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    // ---------- Топ ----------

    public record TopEntry(String uuid, String name, double balance) {}

    public static List<TopEntry> getTopBalances(int limit) {
        List<TopEntry> list = new ArrayList<>();
        synchronized (LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT b.uuid, b.balance, p.name FROM balances b " +
                    "LEFT JOIN player_names p ON p.uuid = b.uuid " +
                    "ORDER BY b.balance DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String uuid = rs.getString("uuid");
                        String name = rs.getString("name");
                        if (name == null) {
                            name = uuid;   // полный UUID
                        }
                        list.add(new TopEntry(uuid, name, rs.getDouble("balance")));
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return list;
    }
}