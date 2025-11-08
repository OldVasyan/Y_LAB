package com.marketplace.repository;

import com.marketplace.model.Product;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ProductRepository сохраняет список товаров в файл через Java-serialization.
 * По умолчанию файл: data/products.dat
 */
public class FileProductRepository implements ProductRepository {

    private final Path filePath;

    public FileProductRepository() {
        this("data/products.dat");
    }

    public FileProductRepository(String path) {
        this.filePath = Paths.get(path);
        try {
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("Не удалось инициализировать репозиторий файлов: " + e.getMessage());
        }
    }

    @Override
    public List<Product> loadAll() {
        if (!Files.exists(filePath) || filePath.toFile().length() == 0) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(filePath, StandardOpenOption.READ)))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                //noinspection unchecked
                return (List<Product>) obj;
            } else {
                System.err.println("Непредвиденный формат файла: ожидался List<Product>");
            }
        } catch (EOFException eof) {
            return new ArrayList<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка при загрузке продуктов из файла: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public void saveAll(Collection<Product> products) {
        Path tmp = filePath.resolveSibling(filePath.getFileName() + ".tmp");
        // запись во временный файл tmp
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)))) {
            List<Product> list = new ArrayList<>(products);
            oos.writeObject(list);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Ошибка при записи временного файла: " + e.getMessage());
            e.printStackTrace();
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
            return;
        }

        // попытка переместить/заменить основной файл
        boolean success = false;
        int attempts = 0;
        while (!success && attempts < 3) {
            attempts++;

            try {
                Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                success = true;
            }
            catch (IOException atomicEx) {
                try {
                    Files.copy(tmp, filePath, StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(tmp);
                    success = true;

                } catch (IOException fallbackEx) {
                    System.err.println("Попытка " + attempts + " не удалась: " + fallbackEx.getMessage());

                    try { TimeUnit.MILLISECONDS.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }

        if (!success) {
            System.err.println("Ошибка при сохранении продуктов в файл: не удалось обновить " + filePath +
                    " после " + attempts + " попыток");
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
        }
    }

    @Override
    public String getStorageInfo() {
        return filePath.toAbsolutePath().toString();
    }
}
