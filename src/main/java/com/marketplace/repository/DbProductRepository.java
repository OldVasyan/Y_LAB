package com.marketplace.repository;

import com.marketplace.model.Product;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        String sql = "SELECT id, name, category, brand, price, created_at, updated_at FROM " + schema + ".product";
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

    public Product findById(Long id) {
        String sql = "SELECT id, name, category, brand, price, created_at, updated_at FROM " + schema + ".product WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product by id", e);
        }
    }

    public void saveOrUpdate(Product p) {
        if (p == null) return;

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            Instant now = Instant.now();

            // 1. UPDATE: Если ID уже существует (p.getId() != null), пытаемся обновить по числовому ID
            if (p.getId() != null) {
                try (PreparedStatement upd = c.prepareStatement(
                        "UPDATE " + schema + ".product " +
                                "SET name=?, category=?, brand=?, price=?, updated_at=? " +
                                "WHERE id=?"
                )) {
                    upd.setString(1, p.getName());
                    upd.setString(2, p.getCategory());
                    upd.setString(3, p.getBrand());
                    upd.setDouble(4, p.getPrice());
                    upd.setTimestamp(5, Timestamp.from(now));
                    upd.setLong(6, p.getId());

                    if (upd.executeUpdate() > 0) {
                        c.commit();
                        return;
                    }
                }
            }

            // 2. INSERT: Если ID==null или UPDATE не прошел, выполняем вставку
            String INSERT_SQL = "INSERT INTO " + schema + ".product " +
                    "(name, category, brand, price, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ins = c.prepareStatement(
                    INSERT_SQL,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                ins.setString(1, p.getName());
                ins.setString(2, p.getCategory());
                ins.setString(3, p.getBrand());
                ins.setDouble(4, p.getPrice());
                ins.setTimestamp(5, Timestamp.from(now));
                ins.setTimestamp(6, Timestamp.from(now));

                ins.executeUpdate();

                // 3. Получаем сгенерированный ID
                try (ResultSet generatedKeys = ins.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Long generatedId = generatedKeys.getLong(1);
                        p.setId(generatedId);
                    }
                }
            }

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to saveOrUpdate product", e);
        }
    }

    public boolean deleteById(Long id) {
        String sql = "DELETE FROM " + schema + ".product WHERE id = ?";
        try (Connection c = ds.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete product", e);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + schema + ".product";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int deletedCount = ps.executeUpdate();
            System.out.println("Удалено товаров: " + deletedCount);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete all products", e);
        }
    }

    // -- mapping --
    private Product mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String name = rs.getString("name");
        String category = rs.getString("category");
        String brand = rs.getString("brand");
        double price = rs.getDouble("price");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Instant created = createdTs != null ? createdTs.toInstant() : Instant.now();
        Instant updated = updatedTs != null ? updatedTs.toInstant() : created;

        return new Product(id, name, category, brand, price, created, updated);
    }

    @Override
    public String getStorageInfo() {
        return "postgres (schema=" + schema + ")";
    }
}

