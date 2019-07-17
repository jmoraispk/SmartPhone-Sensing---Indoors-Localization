package com.example.sps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.sps.activity_recognizer.FloatTriplet;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

public class AccelerometerListener implements SensorEventListener {

    private static final float ALPHA = 0.25f;

    Queue<Float> toPopulateMagnitude;
    Queue<FloatTriplet> toPopulateRaw;
    AtomicInteger accReadingsSinceLastUpdate;
    Float previousSampleMagnitude;
    FloatTriplet previousSampleRaw;


    public AccelerometerListener(Queue<Float> toPopulateMagnitude, Queue<FloatTriplet> toPopulateRaw, AtomicInteger accReadingsSinceLastUpdate) {
        this.toPopulateMagnitude = toPopulateMagnitude;
        this.toPopulateRaw = toPopulateRaw;
        this.accReadingsSinceLastUpdate = accReadingsSinceLastUpdate;
        this.previousSampleMagnitude = null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (toPopulateMagnitude.size() >= NUM_ACC_READINGS) {
            toPopulateMagnitude.poll();
            toPopulateRaw.poll();
        }


        Float magnitude = (float) Math.sqrt(Math.pow(sensorEvent.values[0],2) + Math.pow(sensorEvent.values[1],2) + Math.pow(sensorEvent.values[2],2));
        FloatTriplet raw = new FloatTriplet(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]);
        if (previousSampleMagnitude != null) {
            magnitude = previousSampleMagnitude + ALPHA * (magnitude - previousSampleMagnitude);

            raw.setX(previousSampleRaw.getX() + ALPHA * (raw.getX() - previousSampleRaw.getX()));
            raw.setY(previousSampleRaw.getY() + ALPHA * (raw.getY() - previousSampleRaw.getY()));
            raw.setZ(previousSampleRaw.getZ() + ALPHA * (raw.getZ() - previousSampleRaw.getZ()));
        }


        toPopulateMagnitude.add(magnitude);
        toPopulateRaw.add(raw);

        previousSampleMagnitude = magnitude;
        previousSampleRaw = raw;
        if (accReadingsSinceLastUpdate != null)
            accReadingsSinceLastUpdate.incrementAndGet();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
