package ru.example.completablefuture;

public class FinalResult {
    private final String id;
    private final String name;
    private final double price;
    private final String currency;

    public FinalResult(String id, String name, double price, String currency) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.currency = currency;
    }

    @Override
    public String toString() {
        return String.format("Товар [%s]: %s, Цена: %.2f %s", id, name, price, currency);
    }
}