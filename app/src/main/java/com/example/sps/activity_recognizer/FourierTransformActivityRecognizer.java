package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.ACCELEROMETER_SAMPLES_PER_SECOND;
import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

public class FourierTransformActivityRecognizer implements ActivityRecognizer {


    private int dominantFrequencyIndex;
    private float dominantFrequencyHz;
    private float dominantFrequencyMagnitude;

    private int samplesSinceTransition = 0;
    private SubjectActivity lastState = SubjectActivity.STANDING;


    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, AtomicInteger accReadingsSinceLastUpdate) {

        List<Float> mostRecent = new ArrayList<>(sensorData).subList(sensorData.size()/8*6, sensorData.size()-1);

        float rawmean = Utils.mean(mostRecent);
        float rawstdDev = Utils.stdDeviation(mostRecent, rawmean);

        if (rawstdDev < 0.5) {
            if(lastState == SubjectActivity.WALKING) samplesSinceTransition = accReadingsSinceLastUpdate.get();
            lastState = SubjectActivity.STANDING; return SubjectActivity.STANDING;};

        List<Float> accelerometerDataMagnitudeFFT = Utils.fourierTransform(new ArrayList<>(sensorData));



        List<Float> ofInterest = accelerometerDataMagnitudeFFT.subList(1, accelerometerDataMagnitudeFFT.size() / 5);

        float mean = Utils.mean(ofInterest);
        float stddev = Utils.stdDeviation(ofInterest, mean);



        int dominantFrequencyIndex = Utils.argMax(ofInterest) ;
        float dominantFrequencyHz = dominantFrequencyIndex*(ACCELEROMETER_SAMPLES_PER_SECOND)/ ((float)NUM_ACC_READINGS);
        float dominantFrequencyMagnitude = accelerometerDataMagnitudeFFT.get(dominantFrequencyIndex);

        if(dominantFrequencyMagnitude > mean +2.5*stddev && dominantFrequencyHz > 0.5 && dominantFrequencyHz < 2.5) {
            this.dominantFrequencyIndex = dominantFrequencyIndex;
            this.dominantFrequencyHz = dominantFrequencyHz;
            this.dominantFrequencyMagnitude = dominantFrequencyMagnitude;
            lastState = SubjectActivity.WALKING;
            return SubjectActivity.WALKING;}


        return lastState;
    }



    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if(currentActivityState == SubjectActivity.WALKING){
            int numUpdates = accReadingsSinceLastUpdate.get();
            int numSteps = (int) (numUpdates / ((ACCELEROMETER_SAMPLES_PER_SECOND)/  dominantFrequencyHz));

            int numUpdatesDecrease = (int) (- numSteps * ( ((ACCELEROMETER_SAMPLES_PER_SECOND)/  dominantFrequencyHz)));

            accReadingsSinceLastUpdate.addAndGet(numUpdatesDecrease);
            return numSteps;

        }
        int numSteps = 0;
        if(samplesSinceTransition != 0) {
            numSteps = Math.round(samplesSinceTransition / ((ACCELEROMETER_SAMPLES_PER_SECOND)/  dominantFrequencyHz));
            samplesSinceTransition = 0;

        }

        accReadingsSinceLastUpdate.set(0);

        return numSteps;
    }



}
