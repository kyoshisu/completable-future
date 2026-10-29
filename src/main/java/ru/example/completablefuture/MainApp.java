package ru.example.completablefuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MainApp {
    
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r);
                t.setName("AsyncWorker-" + t.getId());
                t.setDaemon(true);
                return t;
            }
    );

    private static final Random random = new Random();
    private static final List<String> ids = List.of("ID-1", "ID-2", "ID-3", "ID-4", "ID-5");

    public static void main(String[] args) {
        System.out.println("1. СИНХРОННОЕ выполнение (для сравнения)");
        long syncStart = System.currentTimeMillis();
        List<FinalResult> syncResults = fetchAllDataSync(ids);
        long syncEnd = System.currentTimeMillis();
        syncResults.forEach(System.out::println);
        System.out.println("Синхронное время: " + (syncEnd - syncStart) + " мс\n");

        System.out.println("2. АСИНХРОННОЕ выполнение (CompletableFuture)");
        long asyncStart = System.currentTimeMillis();
        List<FinalResult> asyncResults = fetchAllDataAsync(ids);
        long asyncEnd = System.currentTimeMillis();
        asyncResults.forEach(System.out::println);
        System.out.println("Асинхронное время: " + (asyncEnd - asyncStart) + " мс");

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public static List<FinalResult> fetchAllDataAsync(List<String> ids) {
        List<CompletableFuture<FinalResult>> futures = ids.stream()
                .map(MainApp::processSingleIdAsync)
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(res -> res != null)
                .collect(Collectors.toList());
    }

    private static CompletableFuture<FinalResult> processSingleIdAsync(String id) {
        System.out.println("[" + Thread.currentThread().getName() + "] Запуск обработки для " + id);

        CompletableFuture<String> nameFuture = CompletableFuture.supplyAsync(() -> fetchName(id), executor);

        CompletableFuture<Double> priceFuture = CompletableFuture.supplyAsync(() -> fetchPrice(id), executor);

        return nameFuture.thenCombine(priceFuture, (name, price) -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] Объединение данных для " + id);
                    return new FinalResult(id, name, price, "RUB");
                })

                .handle((result, ex) -> {
                    if (ex != null) {
                        System.out.println("[" + Thread.currentThread().getName() + "] ОШИБКА для " + id + ": " + ex.getCause().getMessage());
                        return null;
                    }
                    return result;
                });
    }

    private static String fetchName(String id) {
        simulateDelay(500, 1000);
        if (id.equals("ID-3")) throw new RuntimeException("Сбой БД при запросе имени для " + id);
        return "Товар_" + id;
    }

    private static Double fetchPrice(String id) {
        simulateDelay(700, 1200);
        if (id.equals("ID-4")) throw new RuntimeException("Тайм-аут API цен для " + id);
        return 100.0 + random.nextInt(900);
    }

    public static List<FinalResult> fetchAllDataSync(List<String> ids) {
        List<FinalResult> results = new ArrayList<>();
        for (String id : ids) {
            try {
                String name = fetchName(id);
                Double price = fetchPrice(id);
                results.add(new FinalResult(id, name, price, "RUB"));
            } catch (Exception e) {
                System.out.println("[Sync] ОШИБКА для " + id + ": " + e.getMessage());
            }
        }
        return results;
    }

    private static void simulateDelay(int min, int max) {
        try {
            Thread.sleep(min + random.nextInt(max - min));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}