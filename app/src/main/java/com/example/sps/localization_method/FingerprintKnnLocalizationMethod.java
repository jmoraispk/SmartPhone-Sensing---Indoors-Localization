package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiScan;
import com.example.sps.database.DatabaseService;

import java.util.LinkedList;
import java.util.List;

public class FingerprintKnnLocalizationMethod extends KnnLocalizationMethod {


    @Override
    public int calculateDistance(List<ScanResult> scan, WifiScan sample) {
        int differences = 0;

        List<String> scannedBSSID = new LinkedList<>();
        List<String> trainedBSSID = new LinkedList<>();

        for(ScanResult result : scan) {
            scannedBSSID.add(result.BSSID);
        }

        for(WifiReading reading : sample.getReadings()) {
            trainedBSSID.add(reading.getBSSID());
        }


        for(String a : scannedBSSID)
            if (! trainedBSSID.contains(a) )
                differences ++;

        for(String b : trainedBSSID)
            if (! scannedBSSID.contains(b))
                differences ++;


        return differences;
    }
}
