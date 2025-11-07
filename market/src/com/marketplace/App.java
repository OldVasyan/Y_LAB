package com.marketplace;

import com.marketplace.model.Product;
import com.marketplace.service.AuthService;
import com.marketplace.service.ProductService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class App {
    public static void main(String[] args) {
        AuthService auth = new AuthService();

        // Попробуем войти
        auth.login("admin", "admin123");
        System.out.println("Is logged in: " + auth.isLoggedIn());
        System.out.println("Current user: " + auth.getCurrentUser().orElse("none"));

        // Выйти
        auth.logout();
        System.out.println("Is logged in: " + auth.isLoggedIn());

        ProductService service = new ProductService();

        Product p1 = new Product("iPhone 16", "Smartphone", "Apple", 130000);
        Product p2 = new Product("Galaxy S24", "Smartphone", "Samsung", 90000);
        Product p3 = new Product("MacBook Pro", "Laptop", "Apple", 250000);
        Product p4 = new Product("ThinkPad X1", "Laptop", "Lenovo", 150000);
        Product p5 = new Product("iPhone 15", "Smartphone", "Apple", 70000);

        service.addProduct(p1);
        service.addProduct(p2);
        service.addProduct(p3);
        service.addProduct(p4);
        service.addProduct(p5);

        System.out.println("\nAll products:");
        service.listAll().forEach(System.out::println);

        System.out.println("\nSearch by brand 'Apple':");
        List<Product> apples = service.searchByBrand("Apple");
        apples.forEach(System.out::println);

        System.out.println("\nSearch by price 80000..200000:");
        service.searchByPriceRange(80000, 200000).forEach(System.out::println);


        System.out.println("\nUpdate price of Galaxy:");
        Optional<Product> maybe = service.searchByName("Galaxy").stream().findFirst();
        if (maybe.isPresent()) {
            UUID id = maybe.get().getId();
            service.updateProduct(id, p -> p.setPrice(75000));
            System.out.println("Updated: " + service.getById(id).get());
        }


        System.out.println("\nDelete MacBook:");
        UUID macId = service.searchByName("MacBook").stream().findFirst().map(Product::getId).orElse(null);
        if (macId != null) {
            service.deleteProduct(macId);
            System.out.println("MacBook deleted.");
        }

        System.out.println("\nFinal product list:");
        service.listAll().forEach(System.out::println);

        System.out.println("\nFilter test:");
        service.filter(
                Optional.of("iphone"),
                Optional.empty(),
                Optional.of("apple"),
                Optional.of(90000.0),
                Optional.of(130000.0)
        ).forEach(System.out::println);



    }
}

