package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

class AutocorrActivityRecognizer implements ActivityRecognizer {


    public static final int MIN_DELAY = 40;
    public static final int MAX_DELAY = 100;


    int minDelay = MIN_DELAY;
    int maxDelay = MAX_DELAY;

    int optDelay = 0;


    private SubjectActivity lastState = SubjectActivity.STANDING;

    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection, AtomicInteger accReadingsSinceLastUpdate) {

        List<Float> sensorDataMagnitudeList = new ArrayList<>(sensorData);


        float mean = Utils.mean(sensorDataMagnitudeList);
        float stdDev = Utils.stdDeviation(sensorDataMagnitudeList, mean);

        if (stdDev < 0.3) { lastState = SubjectActivity.STANDING; return SubjectActivity.STANDING;};
        if (stdDev > 3) {  return SubjectActivity.JERKY_MOTION;}

        if (optDelay != 0) {
            minDelay = Math.max(optDelay - 12, MIN_DELAY);
            maxDelay = Math.min(optDelay + 12, MAX_DELAY);
        }
        List<Float> correlationsForEachDelay = Utils.correlation(sensorDataMagnitudeList, sensorDataMagnitudeList, minDelay, maxDelay);


        int largestCorrelationIndex = Utils.argMax(correlationsForEachDelay);
        float correlation = correlationsForEachDelay.get(largestCorrelationIndex);

        if (correlation > 0.775 ) {
                optDelay = largestCorrelationIndex + minDelay;

            lastState = SubjectActivity.WALKING;
            return SubjectActivity.WALKING;
        }
        return lastState;
    }

    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if (currentActivityState == SubjectActivity.WALKING || (currentActivityState == SubjectActivity.JERKY_MOTION  && lastState == SubjectActivity.WALKING)) {
            int numUpdates = accReadingsSinceLastUpdate.get();
            int numSteps = numUpdates / (optDelay / 2);
            accReadingsSinceLastUpdate.addAndGet(-(optDelay / 2)*numSteps);
            return numSteps;
        }
        accReadingsSinceLastUpdate.set(0);
        return 0;
    }

}