package com.example.sps.localization_method;

import android.net.wifi.ScanResult;
import com.example.sps.database.DatabaseService;

import java.util.List;

public interface LocalizationMethod {

    float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities, DatabaseService databaseService);

    String getMiscInfo();
}
