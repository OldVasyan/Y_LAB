package com.marketplace.repository;

import com.marketplace.model.Product;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductRepository {

    List<Product> loadAll();

    void saveAll(Collection<Product> products);

    void saveOrUpdate(Product p);
    Product findById(Long id);
    boolean deleteById(Long id);
    void deleteAll();

    default String getStorageInfo() {
        return "unknown";
    }
}
