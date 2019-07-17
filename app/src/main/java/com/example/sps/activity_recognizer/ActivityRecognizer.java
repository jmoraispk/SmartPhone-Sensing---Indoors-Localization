package com.example.sps.activity_recognizer;

import android.app.Activity;

import com.example.sps.database.DatabaseService;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public interface ActivityRecognizer {

    SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection,AtomicInteger accReadingsSinceLastUpdate);

    int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate);

}
