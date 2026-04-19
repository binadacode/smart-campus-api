package com.smartcampus.model;

public class SensorReading {

    private String id;
    private String timestamp; // ISO-8601 string; overwritten server-side on every POST
    private double value;

    public SensorReading() {
    }

    public SensorReading(String id, String timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}