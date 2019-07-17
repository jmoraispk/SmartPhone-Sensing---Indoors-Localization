package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiScan;
import com.example.sps.database.DatabaseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSSIFingerprintKnnLocalizationMethod extends KnnLocalizationMethod {
    private static final int MAX_DIST = 45;


    @Override
    public int calculateDistance(List<ScanResult> scan, WifiScan sample) {
        int diff = 0;
        Map<String, Integer> scanRSSs = new HashMap<>();
        Map<String, Integer> sampleRSSs= new HashMap<>();

        for(ScanResult s : scan) scanRSSs.put(s.BSSID, s.level);
        for(WifiReading r : sample.getReadings()) sampleRSSs.put(r.getBSSID(), r.getRSS());

        for(String BSSID : scanRSSs.keySet()){
            if(sampleRSSs.containsKey(BSSID))
                diff += Math.abs(scanRSSs.get(BSSID) - sampleRSSs.get(BSSID));
            else
                diff += MAX_DIST;
        }

        for(String BSSID : sampleRSSs.keySet()){
            if(scanRSSs.containsKey(BSSID))
                diff += Math.abs(sampleRSSs.get(BSSID) - scanRSSs.get(BSSID));
            else
                diff += MAX_DIST;
        }

        return diff;

    }
}
