package me.iib.PintosLeiloes.database;

import me.iib.PintosLeiloes.PintosLeiloes;
import me.iib.PintosLeiloes.models.Auction;
import me.iib.PintosLeiloes.models.Claim;
import me.iib.PintosLeiloes.utils.Util;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Written by iib <iib@realmclub.com>, 15:56 27/10/2026
 */
public class Database {
    private Connection conn;


    public synchronized void connect() throws SQLException {

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {

        }

        PintosLeiloes.getInstance().getDataFolder().mkdirs();
        File a = new File(PintosLeiloes.getInstance().getDataFolder(), "leilao.db");
        conn = DriverManager.getConnection("jdbc:sqlite:" + a.getAbsolutePath());
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("CREATE TABLE IF NOT EXISTS auctions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "seller_uuid TEXT NOT NULL,"
                    + "seller_name TEXT NOT NULL,"
                    + "item TEXT NOT NULL,"
                    + "start_price REAL NOT NULL,"
                    + "current_bid REAL NOT NULL,"
                    + "bidder_uuid TEXT,"
                    + "bidder_name TEXT,"
                    + "created_at INTEGER NOT NULL,"
                    + "end_time INTEGER NOT NULL,"
                    + "status TEXT NOT NULL DEFAULT 'ACTIVE')");
            st.execute("CREATE TABLE IF NOT EXISTS claims ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "owner_uuid TEXT NOT NULL,"
                    + "item TEXT NOT NULL,"
                    + "type TEXT NOT NULL,"
                    + "price REAL NOT NULL,"
                    + "auction_id INTEGER,"
                    + "created_at INTEGER NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS notifications ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "uuid TEXT NOT NULL,"
                    + "message TEXT NOT NULL,"
                    + "created_at INTEGER NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_claims_owner ON claims(owner_uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_notif_uuid ON notifications(uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status)");
        }
    }

    public synchronized void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ignored) {
        }
    }

    public synchronized int insertAuction(UUID seller, String sellerName, ItemStack item, double startPrice, long createdAt, long endTime) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO auctions(seller_uuid,seller_name,item,start_price,current_bid,created_at,end_time,status) " + "VALUES(?,?,?,?,?,?,?,'ACTIVE')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, seller.toString());
            ps.setString(2, sellerName);
            ps.setString(3, Util.toBase64(item));
            ps.setDouble(4, startPrice);
            ps.setDouble(5, startPrice);
            ps.setLong(6, createdAt);
            ps.setLong(7, endTime);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {

        }
        return -1;
    }

    public synchronized List<Auction> loadActive() {
        List<Auction> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM auctions WHERE status='ACTIVE' ORDER BY id")) {
            while (rs.next()) {
                String bu = rs.getString("bidder_uuid");
                list.add(new Auction(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("seller_uuid")),
                        rs.getString("seller_name"),
                        Util.fromBase64(rs.getString("item")),
                        rs.getDouble("start_price"),
                        rs.getDouble("current_bid"),
                        bu == null ? null : UUID.fromString(bu),
                        rs.getString("bidder_name"),
                        rs.getLong("created_at"),
                        rs.getLong("end_time")));
            }
        } catch (SQLException e) {

        }
        return list;
    }

    public synchronized void updateBid(int id, double bid, UUID bidder, String bidderName, long endTime) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE auctions SET current_bid=?, bidder_uuid=?, bidder_name=?, end_time=? WHERE id=?")) {
            ps.setDouble(1, bid);
            ps.setString(2, bidder.toString());
            ps.setString(3, bidderName);
            ps.setLong(4, endTime);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {

        }
    }

    public synchronized void setStatus(int id, String status) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE auctions SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {

        }
    }

    public synchronized void insertClaim(UUID owner, ItemStack item, String type, double price, int auctionId) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO claims(owner_uuid,item,type,price,auction_id,created_at) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, owner.toString());
            ps.setString(2, Util.toBase64(item));
            ps.setString(3, type);
            ps.setDouble(4, price);
            ps.setInt(5, auctionId);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {

        }
    }

    private Claim readClaim(ResultSet rs) throws SQLException {
        return new Claim(rs.getInt("id"),
                UUID.fromString(rs.getString("owner_uuid")),
                Util.fromBase64(rs.getString("item")), rs.getString("type"), rs.getDouble("price"), rs.getLong("created_at"));
    }

    public synchronized List<Claim> getClaims(UUID owner) {
        List<Claim> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM claims WHERE owner_uuid=? ORDER BY id DESC")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readClaim(rs));
                }
            }
        } catch (SQLException e) {

        }
        return list;
    }

    public synchronized Claim getClaim(int id, UUID owner) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM claims WHERE id=? AND owner_uuid=?")) {
            ps.setInt(1, id);
            ps.setString(2, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readClaim(rs);
                }
            }
        } catch (SQLException e) {

        }
        return null;
    }

    public synchronized int countClaims(UUID owner) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM claims WHERE owner_uuid=?")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {

        }
        return 0;
    }

    public synchronized void deleteClaim(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM claims WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {

        }
    }


    public synchronized void insertNotification(UUID uuid, String message) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO notifications(uuid,message,created_at) VALUES(?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, message);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {

        }
    }

    public synchronized List<String> popNotifications(UUID uuid) {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT message FROM notifications WHERE uuid=? ORDER BY id")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {

        }
        if (!list.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notifications WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {

            }
        }
        return list;
    }
}
