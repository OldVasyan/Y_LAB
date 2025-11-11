package com.marketplace;

import com.marketplace.model.Product;
import com.marketplace.repository.FileProductRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.service.AuditService;
import com.marketplace.service.AuthService;
import com.marketplace.service.ProductService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class App {
    public static void main(String[] args) {
        AuditService auditService = new AuditService();
        AuthService auth = new AuthService(auditService);
        ProductRepository repository = new FileProductRepository("data/products.dat");
        ProductService productService = new ProductService(repository, auditService);

        auth.login("admin", "admin123");
        productService.setCurrentUser(auth.getCurrentUser().orElse("unknown"));



        // вызов консольного меню
        ConsoleMenu menu = new ConsoleMenu(productService, auth);
        menu.start();

        // --блок для демонстрации товаров, очищаем бд, добавляем демотовары, выводим на экран
        //productService.deleteAll();
        //addDemoProducts(productService);
        //System.out.println("\nВсе продукты:");
        //productService.listAll().forEach(System.out::println);

        auth.logout();

    }
    private static void addDemoProducts(ProductService productService) {
        Product p1 = new Product("iPhone 16", "Smartphone", "Apple", 130000);
        Product p2 = new Product("Galaxy S24", "Smartphone", "Samsung", 90000);
        Product p3 = new Product("MacBook Pro", "Laptop", "Apple", 250000);
        Product p4 = new Product("ThinkPad X1", "Laptop", "Lenovo", 150000);
        Product p5 = new Product("iPhone 15", "Smartphone", "Apple", 70000);

        productService.addProduct(p1);
        productService.addProduct(p2);
        productService.addProduct(p3);
        productService.addProduct(p4);
        productService.addProduct(p5);
    }
}
