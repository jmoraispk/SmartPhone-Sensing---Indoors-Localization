package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.ACCELEROMETER_SAMPLES_PER_SECOND;
import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;


/* This activity is the same as AutocorrActivityRecognizer but it will correlate with many more
activities apart from just Walking.
RUNNING
STAIRS
ELEVATOR

It should return the one that autocorrelates the best or that correlates to a min threshold,
whatever happens first.
 */

class CrossCorrActivityRecognizer implements ActivityRecognizer {

    SubjectActivity lastState = SubjectActivity.STANDING;

    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection, AtomicInteger accReadingsSinceLastUpdate) {


        int minDelay = 10;
        int maxDelay = 90;

        List<Float> sensorDataMagnitudeList = new ArrayList<>(sensorData);

        List<Float> mostRecent = sensorDataMagnitudeList.subList(sensorDataMagnitudeList.size()/4 * 3, sensorDataMagnitudeList.size());

        float mean = Utils.mean(mostRecent);
        float stdDev = Utils.stdDeviation(mostRecent, mean);



        //Get the magnitude of the acceleration sensors
        List<Float> sensorDataX = new ArrayList<>();
        List<Float> sensorDataY = new ArrayList<>();
        List<Float> sensorDataZ = new ArrayList<>();

        for (FloatTriplet f : sensorDataRaw){
            sensorDataX.add(f.getX());
            sensorDataY.add(f.getY());
            sensorDataZ.add(f.getZ());
        }

        Map<SubjectActivity, Float> maxCorrelationPerActivity = new HashMap<>();



        List<List<FloatTriplet>> sensorDataListFromDatabase;

        List<SubjectActivity> activitiesToIdentify = new ArrayList<>();
        activitiesToIdentify.add(SubjectActivity.STANDING);
        activitiesToIdentify.add(SubjectActivity.WALKING);
        activitiesToIdentify.add(SubjectActivity.RUNNING);
        activitiesToIdentify.add(SubjectActivity.STAIRS);
        activitiesToIdentify.add(SubjectActivity.SIDE_STEPPING_RIGHT);
        activitiesToIdentify.add(SubjectActivity.SIDE_STEPPING_LEFT);

        if (stdDev < 0.5) {lastState = SubjectActivity.STANDING;return SubjectActivity.STANDING;}

        for(SubjectActivity activityToIdentify: activitiesToIdentify) {
            //get the recordings for each activity (if there are any, else continue)
            sensorDataListFromDatabase = dbconnection.getActivityRecordings(activityToIdentify);
            if (sensorDataListFromDatabase == null) continue;


            for(List<FloatTriplet> recording : sensorDataListFromDatabase) {


                //Separate each recording into its components
                List<Float> recordedDataX = new ArrayList<>();
                List<Float> recordedDataY = new ArrayList<>();
                List<Float> recordedDataZ = new ArrayList<>();

                for (FloatTriplet f : recording){
                    recordedDataX.add(f.getX());
                    recordedDataY.add(f.getY());
                    recordedDataZ.add(f.getZ());
                }


                //Correlate them with sensor data
                List<Float> correlationsForEachDelayX = Utils.correlation(sensorDataX, recordedDataX, minDelay, maxDelay);
                List<Float> correlationsForEachDelayY = Utils.correlation(sensorDataY, recordedDataY, minDelay, maxDelay);
                List<Float> correlationsForEachDelayZ = Utils.correlation(sensorDataZ, recordedDataZ, minDelay, maxDelay);

                //Get overall correlation
                List<Float> correlationsForEachDelayTotal = new ArrayList<>();
                for(int i = 0; i < correlationsForEachDelayX.size(); i++){
                    correlationsForEachDelayTotal.add((correlationsForEachDelayX.get(i) + correlationsForEachDelayY.get(i) + correlationsForEachDelayZ.get(i))/3);
                }

                //Find where there is the best correlation
                int largestCorrelationIndex = Utils.argMax(correlationsForEachDelayTotal);
                float correlationMax = correlationsForEachDelayTotal.get(largestCorrelationIndex);

                if(maxCorrelationPerActivity.containsKey(activityToIdentify)){
                    float currentMaxCorr = maxCorrelationPerActivity.get(activityToIdentify);
                    if(currentMaxCorr < correlationMax)
                        maxCorrelationPerActivity.put(activityToIdentify,  correlationMax);
                }else{
                    maxCorrelationPerActivity.put(activityToIdentify, correlationMax);
                }

            }

        }


        SubjectActivity maxCorrelated = SubjectActivity.STANDING;
        float maxCorrelationUntilNow = 0;
        for(SubjectActivity key: maxCorrelationPerActivity.keySet()){
            System.out.println(key.name() + ": " + maxCorrelationPerActivity.get(key));
            if(maxCorrelationPerActivity.get(key) > maxCorrelationUntilNow){
                maxCorrelationUntilNow = maxCorrelationPerActivity.get(key);
                maxCorrelated = key;
            }
        }
        if(maxCorrelationPerActivity.get(maxCorrelated) > 0.7) {

            lastState = maxCorrelated;
            return maxCorrelated;
        }

        return lastState;
    }




    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if (currentActivityState == SubjectActivity.WALKING || currentActivityState == SubjectActivity.RUNNING) {
            int numSteps = accReadingsSinceLastUpdate.get() / (60 / 2);
            accReadingsSinceLastUpdate.addAndGet(-numSteps * (60/2));

            if (currentActivityState == SubjectActivity.RUNNING) {
                numSteps *= 2; // "Thomas Running invented running when he tried to walk twice at the same time" -> running = 2 * walk
            }
            return numSteps;
        }
        accReadingsSinceLastUpdate.set(0);
        return 0;
    }
}
