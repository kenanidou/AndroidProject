package com.example.lulufindy;

public class ParkingSession {

    private String vehicleNumber;
    private long startTimeMillis;
    private long endTimeMillis = -1;
    private boolean active;

    public ParkingSession(String vehicleNumber, long startTimeMillis) {
        this.vehicleNumber = vehicleNumber;
        this.startTimeMillis = startTimeMillis;
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public void endSession() {
        active = false;
        endTimeMillis = System.currentTimeMillis();
    }

    public String getFormattedElapsedTime() {
        long elapsedMillis = getDurationMillis();
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = (elapsedMillis / (1000 * 60 * 60));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public double calculateCost(double costPerMinute) {
        long totalMinutes = getDurationMillis() / (1000 * 60);
        return totalMinutes * costPerMinute;
    }

    public long getDurationMillis() {
        if (endTimeMillis != -1) {
            return endTimeMillis - startTimeMillis;
        } else {
            return System.currentTimeMillis() - startTimeMillis;
        }
    }

    public void stopSession() {
        this.active = false;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public long getElapsedTime() {
        return getDurationMillis();
    }

}