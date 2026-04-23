package com.westminster.smartcampus.store;

import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central singleton in-memory data store for the application.
 * Uses thread-safe collections to handle concurrent HTTP requests safely.
 */
public class InMemoryStore {

    private static final InMemoryStore INSTANCE = new InMemoryStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readingsBySensorId = new ConcurrentHashMap<>();

    private InMemoryStore() {
        seedData();
    }

    public static InMemoryStore getInstance() {
        return INSTANCE;
    }

    private void seedData() {
        // Seed rooms
        Room lib = new Room("LIB-301", "Library Quiet Study", 60);
        Room lab = new Room("LAB-101", "Computer Lab 1", 30);
        Room hall = new Room("HALL-A", "Main Hall A", 200);
        rooms.put(lib.getId(), lib);
        rooms.put(lab.getId(), lab);
        rooms.put(hall.getId(), hall);

        // Seed sensors
        Sensor co2Lib   = new Sensor("CO2-001",  "CO2",         "ACTIVE",      420.0, "LIB-301");
        Sensor tempLib  = new Sensor("TEMP-001", "TEMPERATURE", "ACTIVE",       22.5, "LIB-301");
        Sensor co2Lab   = new Sensor("CO2-002",  "CO2",         "MAINTENANCE",   0.0, "LAB-101");
        Sensor humHall  = new Sensor("HUM-001",  "HUMIDITY",    "ACTIVE",       55.0, "HALL-A");

        sensors.put(co2Lib.getId(),  co2Lib);
        sensors.put(tempLib.getId(), tempLib);
        sensors.put(co2Lab.getId(),  co2Lab);
        sensors.put(humHall.getId(), humHall);

        lib.getSensorIds().add("CO2-001");
        lib.getSensorIds().add("TEMP-001");
        lab.getSensorIds().add("CO2-002");
        hall.getSensorIds().add("HUM-001");

        // Seed readings
        long now = System.currentTimeMillis();
        List<SensorReading> co2LibReadings = new CopyOnWriteArrayList<>();
        co2LibReadings.add(new SensorReading("READ-001", now - 7_200_000L, 410.0));
        co2LibReadings.add(new SensorReading("READ-002", now - 3_600_000L, 415.0));
        co2LibReadings.add(new SensorReading("READ-003", now - 1_800_000L, 420.0));

        List<SensorReading> tempLibReadings = new CopyOnWriteArrayList<>();
        tempLibReadings.add(new SensorReading("READ-004", now - 3_600_000L, 21.8));
        tempLibReadings.add(new SensorReading("READ-005", now - 1_800_000L, 22.5));

        List<SensorReading> humHallReadings = new CopyOnWriteArrayList<>();
        humHallReadings.add(new SensorReading("READ-006", now - 3_600_000L, 54.2));

        readingsBySensorId.put("CO2-001",  co2LibReadings);
        readingsBySensorId.put("TEMP-001", tempLibReadings);
        readingsBySensorId.put("CO2-002",  new CopyOnWriteArrayList<>());
        readingsBySensorId.put("HUM-001",  humHallReadings);
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Map<String, List<SensorReading>> getReadingsBySensorId() {
        return readingsBySensorId;
    }
}
