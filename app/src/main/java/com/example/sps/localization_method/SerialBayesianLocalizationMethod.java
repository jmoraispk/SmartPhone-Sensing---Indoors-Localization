package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.database.DatabaseService;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.List;

public class SerialBayesianLocalizationMethod implements LocalizationMethod {


    private static final int MIN_NUM_RES_TO_TAKE_INTO_ACCOUNT = 5;
    private static final float PROB_PER_READING = 0.1f / 5;

    @Override
    public float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities, DatabaseService databaseService) {


        int numResultsTakenIntoAccount = 0;
        for (ScanResult scanResult : scan) { //iterate over scans
            priorProbabilities = computeProbWithReading(scanResult, priorProbabilities, databaseService);

            numResultsTakenIntoAccount++;

            //check if we have a strong guess
            if (canStopEarly(priorProbabilities, numResultsTakenIntoAccount))
                return priorProbabilities;
        }
        return priorProbabilities;

    }

    private float[] computeProbWithReading(ScanResult scanResult, float[] priorProbabilities, DatabaseService databaseService) {
        double normalizer = 0;
        for (int i = 0; i < priorProbabilities.length; i++) { // iterate over cells

            NormalDistribution dist = databaseService.getGaussian(i, scanResult.BSSID);
            double probObservingRssInCellI;
            if (dist == null)
                probObservingRssInCellI = 0.05;
            else
                probObservingRssInCellI = dist.cumulativeProbability(scanResult.level + 0.5) - dist.cumulativeProbability(scanResult.level - 0.5);

            normalizer += probObservingRssInCellI * priorProbabilities[i];
            priorProbabilities[i] = (float) (probObservingRssInCellI * priorProbabilities[i]);

        }
        for (int i = 0; i < priorProbabilities.length; i++) { // iterate over cells to normalize
            priorProbabilities[i] /= normalizer;
        }
        return priorProbabilities;
    }

    private boolean canStopEarly(float[] priorProbabilities, int numResultsTakenIntoAccount) {
        if (numResultsTakenIntoAccount < MIN_NUM_RES_TO_TAKE_INTO_ACCOUNT) return false;

        for (int i = 0; i < priorProbabilities.length; i++) {
            if (priorProbabilities[i] > PROB_PER_READING * numResultsTakenIntoAccount) return true;
        }
        return false;
    }

    @Override
    public String getMiscInfo() {
        return null;
    }
}
