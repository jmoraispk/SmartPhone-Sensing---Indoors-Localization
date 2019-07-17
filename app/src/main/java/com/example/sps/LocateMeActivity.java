package com.example.sps;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.sps.activity_recognizer.ActivityAlgorithm;
import com.example.sps.activity_recognizer.ActivityRecognizer;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.FourierTransformActivityRecognizer;
import com.example.sps.activity_recognizer.StepDetectorActivityRecognizer;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_collection.DataCollectionActivity;
import com.example.sps.database.DatabaseService;
import com.example.sps.localization_method.CountParticleWeightsThread;
import com.example.sps.localization_method.ContinuousLocalization;
import com.example.sps.localization_method.LocalizationMethod;
import com.example.sps.localization_method.LocalizationAlgorithm;
import com.example.sps.localization_method.ParallelBayesianLocalizationMethod;
import com.example.sps.localization_method.Particle;
import com.example.sps.map.Cell;
import com.example.sps.map.WallPositions;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;



public class LocateMeActivity extends AppCompatActivity {

    public static final int NUM_ACC_READINGS = 200;
    public static final int NUM_CELLS = 16;

    public static final int ACCELEROMETER_SAMPLES_PER_SECOND = 50;

    public static final int DRAW_FRAMES_PER_SECOND = 10;
    public static final int SKIP_TICKS_DRAW = Math.round(1000.0f / DRAW_FRAMES_PER_SECOND);

    public static final int UPDATE_FRAMES_PER_SECOND = 30;
    public static final int SKIP_TICKS_UPDATE = Math.round(1000.0f / UPDATE_FRAMES_PER_SECOND);

    private static final int XOFFSET1 = 700;
    private static final int XOFFSET2 = 1000;
    private static final int YOFFSET = 5;


    private Canvas canvas;
    private int xOffSet = XOFFSET1;
    private int yOffSet = YOFFSET;
    int particleRadius = 4;

    private Button initialBeliefButton;
    private Button locateMeButton;
    private Button collectDataButton;

    private CheckBox fourierDetailsCheckBox;
    private CheckBox stickyAnglesCheckBox;

    private TextView cellText;
    private TextView actMiscText;

    private Spinner locSpin;
    private Spinner actSpin;

    private EditText currCellText;

    private ActivityRecognizer activityRecognizer;
    private LocalizationMethod localizationMethod;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private LinkedBlockingQueue<Float> accelerometerDataMagnitude;
    private LinkedBlockingQueue<FloatTriplet> accelerometerDataRaw;

    private List<ScanResult> scanData;

    private AccelerometerListener accelerometerListener;

    private IntentFilter wifiIntentFilter;
    private BroadcastReceiver wifiBroadcastReceiver;

    private WifiManager wifiManager;

    private float[] cellProbabilities;

    private DatabaseService databaseService;

    private Sensor rotationSensor;

    private float mAzimuth = 0;

    private LinkedBlockingQueue<Float> azimuthList;

    private boolean stickyAngles;

    private int totalSteps = 0;

    private AtomicInteger accReadingsSinceLastUpdate;

    private boolean update = true;

    private boolean plotDetailedSensor = false;

    private CopyOnWriteArrayList<Particle> particles;

    private WallPositions walls = new WallPositions();

    private Thread activeLocalizationRunnable;
    private CountParticleWeightsThread activeCountParticlesThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_me);


        databaseService = new DatabaseService(this);
        initialBeliefButton = findViewById(R.id.btn_initial_belief);
        locateMeButton = findViewById(R.id.btn_locate_me);
        collectDataButton = findViewById(R.id.btn_collect_data);
        fourierDetailsCheckBox = findViewById(R.id.checkbox_detailed_sensor);
         stickyAnglesCheckBox = findViewById(R.id.checkbox_sticky_angles);
         stickyAngles = false;


        cellText = findViewById(R.id.cell_guess);
        actMiscText = findViewById(R.id.act_guess);
        currCellText = findViewById(R.id.currCell);

        locSpin = findViewById(R.id.localization_algorithm_spin);
        actSpin = findViewById(R.id.activity_detection_spin);
        setInitialBelief();
        azimuthList = new LinkedBlockingQueue<>();


        //Set Adapter for the Localization Spinner
        ArrayAdapter<LocalizationAlgorithm> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LocalizationAlgorithm.values());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        locSpin.setAdapter(adapter);
        //Set Listener for Localization Spinner changes
        locSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                localizationMethod = ((LocalizationAlgorithm) adapterView.getItemAtPosition(i)).getMethod();
                plotDetailedSensor = false;
                fourierDetailsCheckBox.setChecked(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });

        //Set Adapter for the Activity Spinner
        ArrayAdapter<ActivityAlgorithm> adapterAct = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, ActivityAlgorithm.values());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        actSpin.setAdapter(adapterAct);
        //Set Listener for Activity Spinner changes
        actSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ActivityRecognizer previous = activityRecognizer;
                activityRecognizer = ((ActivityAlgorithm) adapterView.getItemAtPosition(i)).getMethod();

                if (previous instanceof StepDetectorActivityRecognizer)
                    sensorManager.unregisterListener(((StepDetectorActivityRecognizer) previous));
                if (activityRecognizer instanceof StepDetectorActivityRecognizer)
                    sensorManager.registerListener((SensorEventListener) activityRecognizer, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_FASTEST);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                activityRecognizer = activityRecognizer; //do nothing
            }
        });


        activityRecognizer = ActivityAlgorithm.NORMAL_STD.getMethod();
        localizationMethod = LocalizationAlgorithm.KNN_RSSI.getMethod();

        accReadingsSinceLastUpdate = new AtomicInteger(0);
        accelerometerDataMagnitude = new LinkedBlockingQueue<>();
        accelerometerDataRaw = new LinkedBlockingQueue<>();
        accelerometerListener = new AccelerometerListener(accelerometerDataMagnitude, accelerometerDataRaw, accReadingsSinceLastUpdate);


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiBroadcastReceiver = new simpleScanBroadcastReceiver();
        wifiIntentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);

        initialBeliefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialBelief();
            }
        });

        // Set the sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        locateMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // while computing the location
                cellText.setText("Loading...");
                actMiscText.setText("");
                if (scanData != null)
                    scanData.removeAll(scanData);

                if (!(localizationMethod instanceof ContinuousLocalization))
                    new Thread(new singleLocalizationRunnable()).start();
                else {
                    resetCellProbabilities();
                    //If there is a localization method running already, stop it
                    if(activeLocalizationRunnable != null && activeLocalizationRunnable instanceof continuousLocalizationRunnable) {
                        ((continuousLocalizationRunnable) activeLocalizationRunnable).setRunning(false);
                        activeCountParticlesThread.setRunning(false);
                        try {
                            activeCountParticlesThread.join();
                            activeLocalizationRunnable.join();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }

                    // And start a new one.
                    activeLocalizationRunnable = new continuousLocalizationRunnable();
                    activeLocalizationRunnable.start();
                }

            }
        });

        //Go to collect Data Activity if that button is pressed
        collectDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent((Activity) view.getContext(), DataCollectionActivity.class);
                startActivity(intent);
            }
        });

        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(new RotationListener(), rotationSensor, 1000000 / ACCELEROMETER_SAMPLES_PER_SECOND);


        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        new Thread(new Runnable() {
            @Override
            public void run() {
                long nextTick = System.currentTimeMillis();
                long sleepTime;
                while (true) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (update) { //"update" is used to control refresh time of the drawings

                                if (plotDetailedSensor) {
                                    xOffSet = XOFFSET2; //move map to the side
                                } else xOffSet = XOFFSET1;

                                drawMap();

                                if (plotDetailedSensor)
                                    drawDetailedSensorData();

                                if (localizationMethod instanceof ContinuousLocalization) {
                                    drawArrow();
                                    //Display steps walked so far:
                                    Paint p = new Paint();
                                    p.setTextSize(50);
                                    canvas.drawText("Steps: " + totalSteps, 20, 40, p);
                                    if (particles != null) {
                                        drawParticles();
                                    }

                                }
                                update = false;
                            }


                        }
                    });
                    nextTick += SKIP_TICKS_DRAW;
                    sleepTime = nextTick - System.currentTimeMillis();
                    if(sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }).start();
    }


    public void onStickyCheckboxClicked(View view) {
        stickyAngles = !stickyAngles;
    }

    public class singleLocalizationRunnable extends Thread {
        public void run() {
            update = true;

            accelerometerDataMagnitude.removeAll(accelerometerDataMagnitude);
            accelerometerDataRaw.removeAll(accelerometerDataRaw);

            sensorManager.registerListener(accelerometerListener, accelerometer, 1000000 / ACCELEROMETER_SAMPLES_PER_SECOND);

            int numTimesToRepeat = 1;
            int weightingScheme = 1;    //0 - normal consecutive scans (same as repeatedly pressing the button)
                                        //1 - average cell probabilities

            if(localizationMethod instanceof ParallelBayesianLocalizationMethod)
                numTimesToRepeat = 5;


            float[] accumulatedProbabilities = new float[NUM_CELLS];
            float[] currentProbabilities;
            for(int i = 0; i<numTimesToRepeat; i++) {
                //Start wifi scan
                wifiManager.startScan();

                while (scanData == null || scanData.size() == 0 || accelerometerDataMagnitude.size() < NUM_ACC_READINGS) { //spin while data not ready
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (weightingScheme == 0)
                    cellProbabilities = localizationMethod.computeLocation(scanData, cellProbabilities, databaseService);

                if (weightingScheme == 1) {
                    currentProbabilities = localizationMethod.computeLocation(scanData, cellProbabilities, databaseService);
                    //Sum cell probabilities

                    for (int j = 0; j < currentProbabilities.length; j++) {
                        accumulatedProbabilities[j] += currentProbabilities[j];
                    }


                    scanData = new LinkedList<>();
                }
            }

            //Average them and make them the new cellProbabilities
            if (weightingScheme == 1) {
                for (int i = 0; i < cellProbabilities.length; i++) {
                    cellProbabilities[i] = accumulatedProbabilities[i] / numTimesToRepeat;

                }
            }

            sensorManager.unregisterListener(accelerometerListener);

            final SubjectActivity activity = activityRecognizer.recognizeActivity(accelerometerDataMagnitude, accelerometerDataRaw, databaseService, accReadingsSinceLastUpdate);


            final int cell = Utils.getIndexOfLargestOnArray(cellProbabilities) + 1;


            //There is an option to write to a final the measurement result and the current cell we are actually it, for statistics purposes
            if (!currCellText.getText().toString().equals("CurrentCell (for stats)")) {
                int txtCell = Integer.parseInt(currCellText.getText().toString());

                try {
                    FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sps/stats.txt", true);
                    fw.append(txtCell + "," + cell + "," + Math.round(cellProbabilities[cell - 1] * 100) + "," + localizationMethod.getClass().getName() + "," + localizationMethod.getMiscInfo() + "\n");
                    fw.flush();
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            final float confidence = cellProbabilities[cell - 1];
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    // Stuff that updates the UI
                    setLocalizationText(activity, cell, confidence);

                    highlightLocation(cell);
                }
            });
        }
    }


    public class continuousLocalizationRunnable extends Thread {

        private boolean running = true;


        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            accReadingsSinceLastUpdate.set(0);
            update = false;

            sensorManager.registerListener(accelerometerListener, accelerometer, (int) (1000000.0f / (ACCELEROMETER_SAMPLES_PER_SECOND)));

            // Spread particles
            particles = ((ContinuousLocalization) localizationMethod).spreadParticles(cellProbabilities);

            //Begin a thread to Continuously count the particles weights and update the text accordingly
            activeCountParticlesThread = new CountParticleWeightsThread(particles, walls, getLocateMeActivity());
            activeCountParticlesThread.start();

            long nextTick = System.currentTimeMillis();
            long sleepTime = 0;

            SubjectActivity currentActivityState = SubjectActivity.LOADING;

            int steps;
            while (localizationMethod instanceof ContinuousLocalization && running) {
                steps = 0;

                //Amount of samples that come into the buffer between localizations
                if(currentActivityState == SubjectActivity.LOADING)
                    accReadingsSinceLastUpdate.set(0);

                if (accelerometerDataMagnitude.size() == NUM_ACC_READINGS) {

                    SubjectActivity newActivity = activityRecognizer.recognizeActivity(accelerometerDataMagnitude, accelerometerDataRaw, databaseService, accReadingsSinceLastUpdate);
                    currentActivityState = newActivity;
                    steps = activityRecognizer.getSteps(accelerometerDataMagnitude, accelerometerDataRaw, databaseService, currentActivityState, accReadingsSinceLastUpdate);
                }
                updateParticles(steps);


                final SubjectActivity currentActivityStatefinal = currentActivityState;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        actMiscText.setText("Activity: " + currentActivityStatefinal.name() + ". " + localizationMethod.getMiscInfo());
                    }
                });


                nextTick += SKIP_TICKS_UPDATE;
                sleepTime = nextTick - System.currentTimeMillis();
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

            sensorManager.unregisterListener(accelerometerListener);
            System.out.println("Unregistered");

        }
    }

    private void updateParticles(int steps) {

        float distance = steps * 0.70f;
        totalSteps += steps;

        if (activityRecognizer instanceof FourierTransformActivityRecognizer) {
            List<Float> azimuths = new ArrayList<Float>(azimuthList);
            ((ContinuousLocalization) localizationMethod).updateParticles(azimuths.get((int) (azimuths.size()*5.0f/6.0f)), distance, particles); //Fourier transform is very fast, should use recent rotation
        }
        else
            ((ContinuousLocalization) localizationMethod).updateParticles(azimuthList.peek(), distance, particles);


        ((ContinuousLocalization) localizationMethod).collideAndResample(particles, walls);

        update = true;
        return;
    }

    //Draw particles in the positions they are in
    private void drawParticles() {

        int width = this.canvas.getWidth();

        float xcale = width / walls.getMaxWidth();


        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
        drawable.getPaint().setColor(Color.RED);

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            drawable.setAlpha((int) (p.getWeight()*1500*255));
            drawable.setBounds(xOffSet - Math.round((p.getY()) * xcale) - particleRadius,
                    yOffSet + Math.round((p.getX()) * xcale) - particleRadius,
                    xOffSet - Math.round((p.getY()) * xcale) + particleRadius,
                    yOffSet + Math.round((p.getX()) * xcale) + particleRadius);
            drawable.draw(canvas);
        }
        return;
    }

    private class RotationListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float[] orientation = new float[3];
            float[] rMat = new float[9];

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // calculate th rotation matrix
                SensorManager.getRotationMatrixFromVector(rMat, sensorEvent.values);
                mAzimuth = (float) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 200) % 360;

                if(stickyAngles)
                    mAzimuth = Math.round(mAzimuth /90) * 90;

                if (azimuthList.size() >= NUM_ACC_READINGS) {
                    azimuthList.poll();
                }

                //save the orientation angles to choose the correct one when steps are taken
                azimuthList.add(mAzimuth);


            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            return;
        }
    }

    //Draw an "arrow" that moves according to the phones orientation relative to the map
    private void drawArrow() {

        double stopX = 200 * Math.cos((mAzimuth + 90) / 180 * Math.PI); // + 90 to compensate for rotation
        double stopY = 200 * Math.sin((mAzimuth + 90) / 180 * Math.PI);
        Paint p = new Paint();
        p.setStrokeWidth(15);
        int x_offset = 200;
        int y_offset = 300;
        canvas.drawLine(x_offset, y_offset, x_offset + (int) Math.round(stopX), y_offset + (int) Math.round(stopY), p);
    }

    //Highlight location on Bayes Localization
    private void highlightLocation(int current_cell) {

        float xcale = canvas.getWidth() / walls.getMaxWidth();

        ShapeDrawable rectangle = new ShapeDrawable(new RectShape());

        //Highlight current cell
        Cell c = walls.getCells().get(current_cell - 1);

        rectangle.getPaint().setColor(Color.GREEN);
        rectangle.getPaint().setStrokeWidth(10);

        rectangle.setBounds(xOffSet - Math.round(c.getBottomWall() * xcale), yOffSet + Math.round(c.getLeftWall() * xcale),
                xOffSet - Math.round(c.getTopWall() * xcale), yOffSet + Math.round(c.getRightWall() * xcale));
        rectangle.draw(canvas);
    }


    private void setLocalizationText(SubjectActivity activity, int cell, float confidence) {
        this.actMiscText.setText("Activity: " + activity.name() + ". " + localizationMethod.getMiscInfo());
        this.cellText.setText("You are at cell " + cell + " with confidence " + Math.round((confidence * 100) * 100) / 100 + "%");
    }


    public void setLocationTextForParticleFilter(int cell) {
        this.cellText.setText("You are most likely at cell " + cell);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.unregisterReceiver(wifiBroadcastReceiver);
        if (activeLocalizationRunnable != null && activeLocalizationRunnable instanceof continuousLocalizationRunnable) {
            ((continuousLocalizationRunnable) activeLocalizationRunnable).setRunning(false);
            activeCountParticlesThread.setRunning(false);
            try {
                activeCountParticlesThread.join();
                activeLocalizationRunnable.join();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);


    }

    //Sets all previous cell probabilities to uniform and resets the steps taken
    protected void setInitialBelief() {
        resetCellProbabilities();
        totalSteps = 0;
    }

    private void resetCellProbabilities() {
        int numCells = databaseService.getNumberOfCells();
        cellProbabilities = new float[numCells];
        for (int i = 0; i < numCells; i++)
            cellProbabilities[i] = 1.0f / numCells;
    }

    public class simpleScanBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            scanData = wifiManager.getScanResults();
        }

    }


    private void drawMap() {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        ImageView canvasView = findViewById(R.id.canvas);

        Bitmap blankBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);

        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);


        int width = this.canvas.getWidth();


        float xcale = width / walls.getMaxWidth();


        ShapeDrawable rectangle = new ShapeDrawable(new RectShape());

        rectangle.getPaint().setColor(Color.BLACK);
        rectangle.getPaint().setStyle(Paint.Style.STROKE);
        rectangle.getPaint().setStrokeWidth(10);


        // draw the objects

        int rot = 2;

        /*normalp.setWeight(((float) p.getTimeAlive()) / totalTimeAlive);
        if (rot == 0)
            for (Cell c : walls.getCells()) {
                rectangle.setBounds(Math.round(c.getLeftWall() * xcale) + xOffSet, Math.round(c.getTopWall() * xcale) + yOffSet,
                        Math.round(c.getRightWall() * xcale) + xOffSet, Math.round(c.getBottomWall() * xcale) + yOffSet);
                rectangle.draw(canvas);
            }


        if (rot == 1)
            for (Cell c : walls.getCells()) {
                rectangle.setBounds(Math.round(c.getTopWall() * xcale) + xOffSet, Math.round(c.getLeftWall() * xcale) + yOffSet,
                        Math.round(c.getBottomWall() * xcale) + xOffSet, Math.round(c.getRightWall() * xcale) + yOffSet);
                rectangle.draw(canvas);
            }
        */
        //PERFECT
        if (rot == 2)
            for (Cell c : walls.getDrawable()) {
                rectangle.setBounds(xOffSet - Math.round(c.getBottomWall() * xcale), yOffSet + Math.round(c.getLeftWall() * xcale),
                        xOffSet - Math.round(c.getTopWall() * xcale), yOffSet + Math.round(c.getRightWall() * xcale));
                rectangle.draw(canvas);
            }


    }

    public LocalizationMethod getLocalizationMethod() {
        return localizationMethod;
    }

    public LocateMeActivity getLocateMeActivity() {
        return this;
    }

    //CheckBox click handler for Detailed Sensor data
    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        if (localizationMethod instanceof ContinuousLocalization)
            plotDetailedSensor = checked;
    }

    //A function that draws the graphs of the Sensor Data and its FFT
    public void drawDetailedSensorData() {
        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());


        float xOff = 3;//canvas.getWidth() / ((float) NUM_ACC_READINGS);

        int x = 0;
        Iterator<Float> accDataIt = accelerometerDataMagnitude.iterator();

        List<Float> accelerometerDataMagnitudeFFT = Utils.fourierTransform(new ArrayList<>(accelerometerDataMagnitude));
        List<Float> except0 = accelerometerDataMagnitudeFFT.subList(1, accelerometerDataMagnitudeFFT.size());
        int highestHz = Utils.argMax(except0);

        while (accDataIt.hasNext()) {
            int mag = (int) (5 * accDataIt.next());
            drawable.getPaint().setColor(Color.GREEN);
            drawable.setBounds((Math.round(xOff * x) - particleRadius),
                    (600 + mag) - particleRadius,
                    Math.round(xOff * x) + particleRadius,
                    (600 + mag) + particleRadius);
            drawable.draw(canvas);

            x++;
        }

        xOff = 5;
        for (x = 1; x < accelerometerDataMagnitudeFFT.size() / 3; x++) {
            int magFFT = (int) (accelerometerDataMagnitudeFFT.get(x) / 2);
            drawable.getPaint().setColor(Color.BLUE);
            drawable.setBounds((Math.round(xOff * x) - particleRadius),
                    (1000 - magFFT) - particleRadius,
                    Math.round(xOff * x) + particleRadius,
                    (1000 - magFFT) + particleRadius);
            drawable.draw(canvas);

            //Draw 0 line
            drawable.getPaint().setColor(Color.BLACK);
            drawable.setBounds((Math.round(xOff * x) - particleRadius + 1),
                    (1000 + 5) - particleRadius + 1,
                    Math.round(xOff * x) + particleRadius - 1,
                    (1000 + 5) + particleRadius - 1);
            drawable.draw(canvas);
        }
    }
}
