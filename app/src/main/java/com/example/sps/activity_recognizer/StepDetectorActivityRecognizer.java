package com.example.sps.activity_recognizer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.sps.database.DatabaseService;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class StepDetectorActivityRecognizer implements ActivityRecognizer, SensorEventListener {

    private AtomicInteger steps = new AtomicInteger(0);

    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData,Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, AtomicInteger accReadingsSinceLastUpdate) {
        if(steps.get() > 0)
            return SubjectActivity.WALKING;
        return SubjectActivity.STANDING;
    }

    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        return steps.getAndSet(0);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        steps.incrementAndGet();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
