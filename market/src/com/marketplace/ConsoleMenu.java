package com.marketplace;

import com.marketplace.model.Product;
import com.marketplace.service.AuthService;
import com.marketplace.service.ProductService;

import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleMenu {

    private final ProductService productService;
    private final AuthService authService;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleMenu(ProductService productService, AuthService authService) {
        this.productService = productService;
        this.authService = authService;
    }

    public void start() {
        System.out.println("=== Marketplace Console ===");

        while (true) {
            System.out.println("\nВведите команду (add, update, delete, list, search, filter, exit):");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "add" -> addProduct();
                case "update" -> updateProduct();
                case "delete" -> deleteProduct();
                case "list" -> listAll();
                case "search" -> searchProducts();
                case "filter" -> filterProducts();
                case "exit" -> {
                    System.out.println("Выход...");
                    return;
                }
                default -> System.out.println("Неизвестная команда!");
            }
        }
    }

    private void addProduct() {
        System.out.println("Добавление нового товара:");
        System.out.print("Название: ");
        String name = scanner.nextLine();
        System.out.print("Категория: ");
        String category = scanner.nextLine();
        System.out.print("Бренд: ");
        String brand = scanner.nextLine();
        System.out.print("Цена: ");
        double price = Double.parseDouble(scanner.nextLine());

        Product p = new Product(name, category, brand, price);
        productService.addProduct(p);
        System.out.println("Товар добавлен: " + p.getName());
    }

    private void updateProduct() {
        System.out.print("Введите ID товара для обновления: ");
        UUID id = UUID.fromString(scanner.nextLine());

        productService.updateProduct(id, product -> {
            System.out.print("Новое название (" + product.getName() + "): ");
            String name = scanner.nextLine();
            if (!name.isEmpty()) product.setName(name);

            System.out.print("Новая категория (" + product.getCategory() + "): ");
            String category = scanner.nextLine();
            if (!category.isEmpty()) product.setCategory(category);

            System.out.print("Новый бренд (" + product.getBrand() + "): ");
            String brand = scanner.nextLine();
            if (!brand.isEmpty()) product.setBrand(brand);

            System.out.print("Новая цена (" + product.getPrice() + "): ");
            String priceInput = scanner.nextLine();
            if (!priceInput.isEmpty()) product.setPrice(Double.parseDouble(priceInput));
        });

        System.out.println("Товар обновлён.");
    }

    private void deleteProduct() {
        System.out.print("Введите ID товара для удаления: ");
        UUID id = UUID.fromString(scanner.nextLine());
        if (productService.deleteProduct(id)) {
            System.out.println("Товар удалён.");
        } else {
            System.out.println("Товар с таким ID не найден.");
        }
    }

    private void listAll() {
        System.out.println("Все товары:");
        productService.listAll().forEach(System.out::println);
        System.out.println("Всего товаров: " + productService.listAll().size());
    }

    private void searchProducts() {
        System.out.print("Поиск по полю (name/brand/category): ");
        String field = scanner.nextLine().toLowerCase();
        System.out.print("Введите значение: ");
        String value = scanner.nextLine();

        long start = System.nanoTime();
        var results = switch (field) {
            case "name" -> productService.searchByName(value);
            case "brand" -> productService.searchByBrand(value);
            case "category" -> productService.searchByCategory(value);
            default -> {
                System.out.println("Неизвестное поле!");
                yield null;
            }
        };

        if (results != null) {
            long duration = System.nanoTime() - start;
            results.forEach(System.out::println);
            System.out.printf("Найдено: %d, время: %.2f мс%n", results.size(), duration / 1_000_000.0);
        }
    }

    private void filterProducts() {
        System.out.println("Фильтрация товаров (оставьте пустое поле, если не хотите фильтровать):");
        System.out.print("Название: ");
        String name = scanner.nextLine();
        System.out.print("Категория: ");
        String category = scanner.nextLine();
        System.out.print("Бренд: ");
        String brand = scanner.nextLine();
        System.out.print("Мин. цена: ");
        String minPriceStr = scanner.nextLine();
        System.out.print("Макс. цена: ");
        String maxPriceStr = scanner.nextLine();

        Optional<String> optName = name.isEmpty() ? Optional.empty() : Optional.of(name);
        Optional<String> optCategory = category.isEmpty() ? Optional.empty() : Optional.of(category);
        Optional<String> optBrand = brand.isEmpty() ? Optional.empty() : Optional.of(brand);
        Optional<Double> optMin = minPriceStr.isEmpty() ? Optional.empty() : Optional.of(Double.parseDouble(minPriceStr));
        Optional<Double> optMax = maxPriceStr.isEmpty() ? Optional.empty() : Optional.of(Double.parseDouble(maxPriceStr));

        long start = System.nanoTime();
        var results = productService.filter(optName, optCategory, optBrand, optMin, optMax);
        long duration = System.nanoTime() - start;

        results.forEach(System.out::println);
        System.out.printf("Найдено: %d, время: %.2f мс%n", results.size(), duration / 1_000_000.0);
    }
}

