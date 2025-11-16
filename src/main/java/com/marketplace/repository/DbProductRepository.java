package com.marketplace.repository;

import com.marketplace.model.Product;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DbProductRepository implements ProductRepository {

    private final DataSource ds;
    private final String schema;

    public DbProductRepository(DataSource ds, String schema) {
        this.ds = ds;
        this.schema = (schema == null || schema.isBlank()) ? "catalog" : schema;
    }

    // ProductRepository methods:

    @Override
    public List<Product> loadAll() {
        String sql = "SELECT uuid, name, category, brand, price, created_at, updated_at FROM " + schema + ".product";
        List<Product> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load products", e);
        }
        return out;
    }

    @Override
    public void saveAll(Collection<Product> products) {
        for (Product p : products) {
            saveOrUpdate(p);
        }
    }

    // --- CRUDы ---

    public Product findByUuid(UUID uuid) {
        String sql = "SELECT uuid, name, category, brand, price, created_at, updated_at FROM " + schema + ".product WHERE uuid = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product by uuid", e);
        }
    }

    public void saveOrUpdate(Product p) {
        if (p == null) return;

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            Instant now = Instant.now();

            try (PreparedStatement upd = c.prepareStatement(
                    "UPDATE " + schema + ".product " +
                            "SET name=?, category=?, brand=?, price=?, updated_at=? " +
                            "WHERE uuid=?"
            )) {
                upd.setString(1, p.getName());
                upd.setString(2, p.getCategory());
                upd.setString(3, p.getBrand());
                upd.setDouble(4, p.getPrice());
                upd.setTimestamp(5, Timestamp.from(now));   // updated_at = now
                upd.setObject(6, p.getId());

                int updated = upd.executeUpdate();

                if (updated == 0) {

                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT INTO " + schema + ".product " +
                                    "(uuid, name, category, brand, price, created_at, updated_at) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?)"
                    )) {
                        ins.setObject(1, p.getId());
                        ins.setString(2, p.getName());
                        ins.setString(3, p.getCategory());
                        ins.setString(4, p.getBrand());
                        ins.setDouble(5, p.getPrice());
                        ins.setTimestamp(6, Timestamp.from(now));
                        ins.setTimestamp(7, Timestamp.from(now));
                        ins.executeUpdate();
                    }
                }
            }

            c.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to saveOrUpdate product", e);
        }
    }

    public boolean deleteByUuid(UUID uuid) {
        String sql = "DELETE FROM " + schema + ".product WHERE uuid = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, uuid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete product", e);
        }
    }

    // -- mapping --
    private Product mapRow(ResultSet rs) throws SQLException {
        UUID uuid = (UUID) rs.getObject("uuid");
        String name = rs.getString("name");
        String category = rs.getString("category");
        String brand = rs.getString("brand");
        double price = rs.getDouble("price");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Instant created = createdTs != null ? createdTs.toInstant() : Instant.now();
        Instant updated = updatedTs != null ? updatedTs.toInstant() : created;

        return new Product(uuid, name, category, brand, price, created, updated);
    }

    @Override
    public String getStorageInfo() {
        return "postgres (schema=" + schema + ")";
    }
}

