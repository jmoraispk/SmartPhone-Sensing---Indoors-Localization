package com.example.sps.data_collection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.example.sps.database.DatabaseService;

import java.util.LinkedList;
import java.util.List;

public class DataCollectionBroadcastReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;
    private DataCollectionActivity src;
    private DatabaseService db;

    private String[] bannedStrings = new String[]{"AP", "Android", "iPhone", "HotSpot", "Hotspot"};

    public DataCollectionBroadcastReceiver(WifiManager wifiManager, DataCollectionActivity src, DatabaseService dbservice) {
        this.wifiManager = wifiManager;
        this.src = src;
        this.db = dbservice;
    }

    public boolean containsBannedWord(String toTest) {
        for(String banned : bannedStrings) {
            if (toTest.contains(banned)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        ScanInfo scanInfo = src.getScanInfo();
        if(scanInfo != null) {
            if (scanInfo.isScanSuccessful()) {
                List<ScanResult> filteredScanResults = new LinkedList<>();
                for (ScanResult s : scanResults)
                    if (!containsBannedWord(s.BSSID)) filteredScanResults.add(s);

                db.insertTableScan(scanInfo.getCellId(), scanInfo.getDirection(), filteredScanResults);
            }

            src.incScanCounter();
            src.updateScanCounter();
            src.setScanInfoToNull();
            src.updateTxtInfoScan();
            src.updateTxtInfoScanSpec();
        }
    }
}
