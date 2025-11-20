package com.marketplace.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

//Легаси код, потом перепишу

public class AuditService {
    private static final String LOGS_DIR = "logs";
    private static final String LOG_FILE = LOGS_DIR + "/audit.log";

    public AuditService() {
        try {
            Files.createDirectories(Path.of(LOGS_DIR));
        } catch (IOException e) {
            System.err.println("Не удалось создать каталог логов: " + e.getMessage());
        }
    }

    // synchronized - чтобы избежать коллизий
    public synchronized void log(String username, String action) {
        String record = String.format("[%s] User: %s -> %s",
                LocalDateTime.now(), username, action);

        // вывод в консоль для наглядности, потом удалю
        System.out.println(record);

        // запись в файл, append = true - добавляем в конец
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(record);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Ошибка при записи лога аудита: " + e.getMessage());
        }
    }
}
