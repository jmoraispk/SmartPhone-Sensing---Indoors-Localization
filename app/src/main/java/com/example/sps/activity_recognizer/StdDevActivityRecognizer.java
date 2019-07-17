package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class StdDevActivityRecognizer implements ActivityRecognizer {

    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection, AtomicInteger accReadingsSinceLastUpdate) {
        List<Float> sensorDataMagnitudeList = new ArrayList<>(sensorData);
        float mean = Utils.mean(sensorDataMagnitudeList);
        float stdDev = Utils.stdDeviation(sensorDataMagnitudeList, mean);


        if(stdDev > 3)
            return SubjectActivity.RUNNING;
        else if(stdDev > 0.8)
            return SubjectActivity.WALKING;
        else
            return SubjectActivity.STANDING;
    }

    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {

        if(currentActivityState == SubjectActivity.STANDING) {
            accReadingsSinceLastUpdate.set(0);
            return 0;
        }
        if(currentActivityState == SubjectActivity.WALKING ) {
            int numSteps = accReadingsSinceLastUpdate.get() / (55 / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (55 / 2);

            accReadingsSinceLastUpdate.set(remainder);
            return numSteps;
        }
        if(currentActivityState == SubjectActivity.RUNNING) {
            int numSteps = accReadingsSinceLastUpdate.get() / (30 / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (30 / 2);

            accReadingsSinceLastUpdate.set(remainder);
            return numSteps;
        }
        return 0;

    }
}
