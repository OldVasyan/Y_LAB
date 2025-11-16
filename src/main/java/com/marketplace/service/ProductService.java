package com.marketplace.service;

import com.marketplace.model.Product;
import com.marketplace.repository.ProductRepository;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProductService {

    private final ProductRepository repository;
    private final AuditService auditService;
    private String currentUser = "unknown";


    public ProductService(ProductRepository repository, AuditService auditService) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");

    }

    public void setCurrentUser(String username) {
        if (username != null) {
            this.currentUser = username;
        }
    }

    public void printStats() {
        System.out.println("Товаров в системе: " + repository.loadAll().size());
    }

    // --------------------- CRUD ----------------------

    public synchronized Long addProduct(Product p) {
        Objects.requireNonNull(p, "product must not be null");
        repository.saveOrUpdate(p);
        auditService.log(currentUser, "добавил товар: " + p.getName() +" ID: "+p.getId());
        return p.getId();
    }

    public synchronized boolean deleteProduct(Long id) {
        Product removed = repository.findById(id);
        boolean deleted = repository.deleteById(id);

        if (deleted && removed != null) {
            auditService.log(currentUser, "удалил товар: " + removed.getName() +" ID: "+removed.getId());
            return true;
        }
        return false;
    }

    public synchronized void deleteAll() {
        repository.deleteAll();
        auditService.log(currentUser, "очистил каталог");
    }

    public synchronized boolean updateProduct(Long id, ProductUpdater updater) {
        Product existing = repository.findById(id);
        if (existing == null) return false;
        updater.update(existing);
        repository.saveOrUpdate(existing);
        auditService.log(currentUser, "обновил товар: " + existing.getName()  +" ID: "+existing.getId());
        return true;
    }

    // --------------------- Поиск и фильтрация ----------------------

    public synchronized List<Product> listAll() {
        return repository.loadAll();
    }

    public List<Product> searchByName(String name) {
        return listAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Product> searchByCategory(String category) {
        return listAll().stream()
                .filter(p -> p.getCategory().toLowerCase().contains(category.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Product> searchByBrand(String brand) {
        return listAll().stream()
                .filter(p -> p.getBrand().toLowerCase().contains(brand.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Product> searchByPriceRange(double minPrice, double maxPrice) {
        return listAll().stream()
                .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                .collect(Collectors.toList());
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

        return listAll().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    // --------------------- Функциональные интерфейсы ----------------------


    @FunctionalInterface
    public interface ProductUpdater {
        void update(Product p);
    }
}
