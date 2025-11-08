package com.marketplace.service;

import com.marketplace.model.Product;
import com.marketplace.repository.ProductRepository;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * ProductService — хранит товары в памяти (Map<UUID,Product>)
 * Использует ProductRepository для персистентности
 * Поддерживает CRUD, поиск/фильтрацию, кэш и аудит действий пользователя
 */
public class ProductService {

    private final Map<UUID, Product> products = new HashMap<>();
    private final Map<String, List<Product>> cache = new HashMap<>();
    private final ProductRepository repository;
    private final AuditService auditService;
    private String currentUser = "unknown";

    public ProductService(ProductRepository repository, AuditService auditService) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");

        List<Product> loaded = repository.loadAll();
        if (loaded != null) {
            for (Product p : loaded) {
                products.put(p.getId(), p);
            }
        }
    }

    public void setCurrentUser(String username) {
        if (username != null) {
            this.currentUser = username;
        }
    }

    public synchronized UUID addProduct(Product p) {
        Objects.requireNonNull(p, "product must not be null");
        products.put(p.getId(), p);
        persist();
        invalidateCache();

        auditService.log(currentUser, "добавил товар: " + p.getName());
        return p.getId();
    }

    public synchronized void deleteAll() {
        products.clear();
        invalidateCache();
        persist();
    }

    public synchronized boolean deleteProduct(UUID id) {
        Product removed = products.remove(id);
        if (removed != null) {
            persist();
            invalidateCache();

            auditService.log(currentUser, "удалил товар: " + removed.getName());
            return true;
        }
        return false;
    }

    public synchronized boolean updateProduct(UUID id, ProductUpdater updater) {
        Product existing = products.get(id);
        if (existing == null) return false;
        updater.update(existing);
        persist();
        invalidateCache();

        auditService.log(currentUser, "обновил товар: " + existing.getName());
        return true;
    }

    public synchronized Optional<Product> getById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }

    public synchronized List<Product> listAll() {
        return new ArrayList<>(products.values());
    }

    public List<Product> searchByName(String name) {
        String key = "name:" + safe(name);
        return cacheComputeIfAbsent(key, () ->
                products.values().stream()
                        .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                        .collect(Collectors.toList()));
    }

    public List<Product> searchByCategory(String category) {
        String key = "category:" + safe(category);
        return cacheComputeIfAbsent(key, () ->
                products.values().stream()
                        .filter(p -> p.getCategory().equalsIgnoreCase(category))
                        .collect(Collectors.toList()));
    }

    public List<Product> searchByBrand(String brand) {
        String key = "brand:" + safe(brand);
        return cacheComputeIfAbsent(key, () ->
                products.values().stream()
                        .filter(p -> p.getBrand().equalsIgnoreCase(brand))
                        .collect(Collectors.toList()));
    }

    public List<Product> searchByPriceRange(double minPrice, double maxPrice) {
        String key = String.format("price:%.2f:%.2f", minPrice, maxPrice);
        return cacheComputeIfAbsent(key, () ->
                products.values().stream()
                        .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                        .collect(Collectors.toList()));
    }

    public List<Product> filter(Optional<String> maybeName,
                                Optional<String> maybeCategory,
                                Optional<String> maybeBrand,
                                Optional<Double> minPrice,
                                Optional<Double> maxPrice) {

        Predicate<Product> predicate = p -> true;

        if (maybeName.isPresent()) {
            String name = maybeName.get().toLowerCase();
            predicate = predicate.and(p -> p.getName().toLowerCase().contains(name));
        }
        if (maybeCategory.isPresent()) {
            String cat = maybeCategory.get().toLowerCase();
            predicate = predicate.and(p -> p.getCategory().equalsIgnoreCase(cat));
        }
        if (maybeBrand.isPresent()) {
            String brand = maybeBrand.get().toLowerCase();
            predicate = predicate.and(p -> p.getBrand().equalsIgnoreCase(brand));
        }
        if (minPrice.isPresent()) {
            double min = minPrice.get();
            predicate = predicate.and(p -> p.getPrice() >= min);
        }
        if (maxPrice.isPresent()) {
            double max = maxPrice.get();
            predicate = predicate.and(p -> p.getPrice() <= max);
        }

        return products.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private synchronized <T> List<T> cacheComputeIfAbsent(String key, SupplierList<T> supplier) {
        @SuppressWarnings("unchecked")
        List<T> cached = (List<T>) cache.get(key);
        if (cached != null) {
            return new ArrayList<>(cached);
        }
        List<T> result = supplier.get();
        cache.put(key, (List<Product>) result.stream().map(p -> (Product) p).collect(Collectors.toList()));
        return new ArrayList<>(result);
    }

    private void invalidateCache() {
        cache.clear();
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private void persist() {
        try {
            repository.saveAll(products.values());
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении данных репозитория: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    public interface SupplierList<T> {
        List<T> get();
    }

    @FunctionalInterface
    public interface ProductUpdater {
        void update(Product p);
    }
}
