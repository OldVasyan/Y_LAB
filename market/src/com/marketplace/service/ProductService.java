package com.marketplace.service;

import com.marketplace.model.Product;
import com.marketplace.repository.ProductRepository;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProductService {

    private final Map<UUID, Product> products = new HashMap<>();
    private final Map<String, List<Product>> cache = new HashMap<>();
    private final ProductRepository repository;
    private final AuditService auditService;
    private String currentUser = "unknown";
    private int cacheHits = 0;
    private int cacheMisses = 0;

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

    public void printStats() {
        System.out.println("Товаров в системе: " + products.size());
        System.out.println("Кэш: попаданий=" + cacheHits + ", промахов=" + cacheMisses);
    }

    // --------------------- CRUD ----------------------

    public synchronized UUID addProduct(Product p) {
        Objects.requireNonNull(p, "product must not be null");
        products.put(p.getId(), p);
        persist();
        invalidateCache();
        auditService.log(currentUser, "добавил товар: " + p.getName() +" ID: "+p.getId());
        return p.getId();
    }

    public synchronized boolean deleteProduct(UUID id) {
        Product removed = products.remove(id);
        if (removed != null) {
            persist();
            invalidateCache();
            auditService.log(currentUser, "удалил товар: " + removed.getName() +" ID: "+removed.getId());
            return true;
        }
        return false;
    }

    public synchronized void deleteAll() {
        products.clear();
        persist();
        invalidateCache();
        auditService.log(currentUser, "очистил каталог");
    }

    public synchronized boolean updateProduct(UUID id, ProductUpdater updater) {
        Product existing = products.get(id);
        if (existing == null) return false;
        updater.update(existing);
        persist();
        invalidateCache();
        auditService.log(currentUser, "обновил товар: " + existing.getName()  +" ID: "+existing.getId());
        return true;
    }

    // --------------------- Поиск и фильтрация ----------------------

    public synchronized List<Product> listAll() {
        return new ArrayList<>(products.values());
    }

    public List<Product> searchByName(String name) {
        String key = "name:" + safe(name);
        return getCachedOrCompute(key, () ->
                products.values().stream()
                        .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                        .collect(Collectors.toList()));
    }

    public List<Product> searchByCategory(String category) {
        String key = "category:" + safe(category);
        return getCachedOrCompute(key, () ->
                products.values().stream()
                        .filter(p -> p.getCategory().equalsIgnoreCase(category))
                        .collect(Collectors.toList()));
    }

    public List<Product> searchByBrand(String brand) {
        String key = "brand:" + safe(brand);
        return getCachedOrCompute(key, () ->
                products.values().stream()
                        .filter(p -> p.getBrand().equalsIgnoreCase(brand))
                        .collect(Collectors.toList()));
    }

    public List<Product> searchByPriceRange(double minPrice, double maxPrice) {
        String key = String.format("price:%.2f-%.2f", minPrice, maxPrice);
        return getCachedOrCompute(key, () ->
                products.values().stream()
                        .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                        .collect(Collectors.toList()));
    }

    // --------------------- Универсальный фильтр ----------------------

    public List<Product> filter(Optional<String> name,
                                Optional<String> category,
                                Optional<String> brand,
                                Optional<Double> minPrice,
                                Optional<Double> maxPrice) {

        Predicate<Product> predicate = p -> true;

        if (name.isPresent()) {
            String n = name.get().toLowerCase();
            predicate = predicate.and(p -> p.getName().toLowerCase().contains(n));
        }
        if (category.isPresent()) {
            String c = category.get().toLowerCase();
            predicate = predicate.and(p -> p.getCategory().equalsIgnoreCase(c));
        }
        if (brand.isPresent()) {
            String b = brand.get().toLowerCase();
            predicate = predicate.and(p -> p.getBrand().equalsIgnoreCase(b));
        }
        if (minPrice.isPresent()) {
            predicate = predicate.and(p -> p.getPrice() >= minPrice.get());
        }
        if (maxPrice.isPresent()) {
            predicate = predicate.and(p -> p.getPrice() <= maxPrice.get());
        }

        return products.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    // --------------------- КЭШ ----------------------

    private List<Product> getCachedOrCompute(String key, SupplierList<Product> supplier) {
        if (cache.containsKey(key)) {
            cacheHits++;
            System.out.println("(из кэша)");
            return cache.get(key);
        }
        cacheMisses++;
        List<Product> result = supplier.get();
        cache.put(key, result);
        return result;
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
            System.err.println("Ошибка при сохранении данных: " + e.getMessage());
        }
    }

    // --------------------- Функциональные интерфейсы ----------------------

    @FunctionalInterface
    public interface SupplierList<T> {
        List<T> get();
    }

    @FunctionalInterface
    public interface ProductUpdater {
        void update(Product p);
    }
}
