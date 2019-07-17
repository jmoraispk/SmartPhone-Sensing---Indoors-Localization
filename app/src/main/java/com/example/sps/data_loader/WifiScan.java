package com.example.sps.data_loader;

import java.util.LinkedList;
import java.util.List;

public class WifiScan {

    private List<WifiReading> readings;

    public WifiScan() {
        readings = new LinkedList<>();
    }

    public WifiScan(List<WifiReading> sample) {
        this.readings = sample;
    }

    public List<WifiReading> getReadings() {
        return readings;
    }

    public void setReadings(List<WifiReading> readings) {
        this.readings = readings;
    }
}
