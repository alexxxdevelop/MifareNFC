package com.mifare.mifare;

public class JournalItem {
    public int sector;
    public int value;
    public String battery;

    // Конструктор
    public JournalItem(int sector, int value, String battery) {
        this.sector = sector;
        this.value = value;
        this.battery = battery;
    }
}
