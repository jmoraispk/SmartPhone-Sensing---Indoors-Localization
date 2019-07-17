package com.example.sps.data_collection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.sps.AccelerometerListener;
import com.example.sps.R;
import com.example.sps.Utils;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_loader.WifiScan;
import com.example.sps.database.DatabaseService;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.ACCELEROMETER_SAMPLES_PER_SECOND;
import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;


public class DataCollectionActivity extends AppCompatActivity {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ScanInfo scanInfo;

    private Button btnScan;
    private Button btnScan10;
    private Button btnDeleteScanData;
    private Button btnDeleteScanLast;

    private Button btnRecordActivity;
    private Button btnDeleteActivityData;
    private Button btnDeleteActivityLast;

    private Button btnCellPlus;
    private Button btnCellMinus;

    private Spinner actSpin;

    private TextView txtInfoScan;
    private TextView txtInfoScanSpec;
    private TextView txtScanCount;
    private TextView txtScanStatus;

    private TextView txtInfoActivity;
    private TextView txtInfoActivitySpec;
    private TextView txtActivityCount;


    private TextView txtScanningCell;
    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private AtomicInteger scanCounter;
    private AtomicInteger actRecordCounter;

    private DatabaseService dbConnection;

    private SubjectActivity selectedActivity;

    private boolean updateGaussians;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateGaussians = false;

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        dbConnection = new DatabaseService(this);


        /////////////////////////////////BUTTONS//////////////////////////

        btnScan = (Button) findViewById(R.id.buttonScan);
        btnScan10 = (Button) findViewById(R.id.buttonScanx10);
        btnDeleteScanData = (Button) findViewById(R.id.deleteScanData);
        btnDeleteScanLast = (Button) findViewById(R.id.deleteLastScan);

        btnRecordActivity = (Button) findViewById(R.id.activityRecorderButton);
        btnDeleteActivityData = (Button) findViewById(R.id.deleteActivityData);
        btnDeleteActivityLast = (Button) findViewById(R.id.deleteLastActivity);

        btnCellPlus = (Button) findViewById(R.id.plusCell);
        btnCellMinus = (Button) findViewById(R.id.minusCell);

        ///////////////////////////////TEXTS////////////////////////////

        txtInfoScan = (TextView) findViewById(R.id.infoScan);
        txtInfoScanSpec = (TextView) findViewById(R.id.infoScanSpec);
        txtScanCount = (TextView) findViewById(R.id.textScans);
        txtScanStatus = (TextView) findViewById(R.id.textStatusScan);

        txtInfoActivity = (TextView) findViewById(R.id.infoActivity);
        txtInfoActivitySpec = (TextView) findViewById(R.id.infoActivitySpec);
        txtActivityCount = (TextView) findViewById(R.id.textActivityCount);

        ///////////////////Text box & Spinner ///////////////////////////
        txtScanningCell = (TextView) findViewById(R.id.textScanningCell);

        actSpin = (Spinner) findViewById(R.id.activity_detection_spin);

        //////////////////////////////////////////////////////////////////

        receiver = new DataCollectionBroadcastReceiver(wifiManager, this, dbConnection);
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(receiver, filter);

        scanCounter = new AtomicInteger(0);
        txtScanningCell.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                scanCounter.set(0);
                updateScanCounter();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO Fix Direction (with android magnetometer)---- worth it now?
                scanInfo = new ScanInfo(wifiManager.startScan(), Integer.parseInt(txtScanningCell.getText().toString()), Direction.EAST);

                updateTxtScanStatus(scanInfo.isScanSuccessful());
                updateGaussians = true;

                //update info texts
                updateTxtInfoScan();
                updateTxtInfoScanSpec();
            }
        });

        btnScan10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 10; i++) {
                            scanInfo = new ScanInfo(wifiManager.startScan(), Integer.parseInt(txtScanningCell.getText().toString()), Direction.EAST);

                            updateTxtScanStatus(scanInfo.isScanSuccessful());
                            while(scanInfo != null){
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            }
                        }
                    }
                }).start();
                updateGaussians = true;

                //update info texts
                updateTxtInfoScan();
                updateTxtInfoScanSpec();

            }
        });

        btnDeleteScanData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbConnection.deleteScanData();
                updateTxtInfoScan();
                updateTxtInfoScanSpec();
            }
        });

        btnDeleteActivityData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbConnection.deleteActivityData();
                updateTxtInfoAct();
                updateTxtInfoActSpec();
            }
        });

        btnDeleteActivityLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbConnection.deleteLastActivity();
                updateTxtInfoAct();
                updateTxtInfoActSpec();
            }
        });

        btnDeleteScanLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbConnection.deleteLastScan();
                updateTxtInfoScan();
                updateTxtInfoScanSpec();
                updateGaussians = true;
            }
        });

        btnCellPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txt = txtScanningCell.getText().toString();

                if (txt.equals("scanningCell"))
                    txtScanningCell.setText("1");
                else{
                    Integer i = Integer.parseInt(txt);
                    Integer a = dbConnection.getNumberOfCells();
                    i ++;
                    if (i > a)
                        txtScanningCell.setText("1");
                    else
                        txtScanningCell.setText(i.toString());
                }
                updateTxtInfoScanSpec();
            }
        });

        btnCellMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txt = txtScanningCell.getText().toString();

                if (txt.equals("scanningCell")) {
                    Integer a = dbConnection.getNumberOfCells();
                    txtScanningCell.setText(a.toString());
                } else {
                    Integer i = Integer.parseInt(txt);
                    if (i > 1) {
                        i --;
                        txtScanningCell.setText(i.toString());
                    } else {
                        Integer a = dbConnection.getNumberOfCells();
                        txtScanningCell.setText(a.toString());
                    }
                }
                updateTxtInfoScanSpec();
            }
        });

        ArrayAdapter<SubjectActivity> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, SubjectActivity.values());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        actSpin.setAdapter(adapter);
        selectedActivity = SubjectActivity.STANDING;
        updateTxtInfoActSpec();
        actRecordCounter = new AtomicInteger(0);
        //Set Listener for Localization Spinner changes
        actSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                actRecordCounter.set(0);


                selectedActivity = ((SubjectActivity) adapterView.getItemAtPosition(i));

                updateTxtInfoActSpec();
                updateActRecCounter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        btnRecordActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Queue < Float > data = new LinkedList<>();
                        Queue < FloatTriplet > dataRaw = new LinkedList<>();
                        SensorEventListener listener = new AccelerometerListener(data, dataRaw,null);
                        sensorManager.registerListener(listener, accelerometer, 1000000/ACCELEROMETER_SAMPLES_PER_SECOND);
                        while(data.size() < NUM_ACC_READINGS ){
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        sensorManager.unregisterListener(listener);

                        dbConnection.insertRecording( ((LinkedList<FloatTriplet>) dataRaw).subList(0, NUM_ACC_READINGS),selectedActivity);


                        incActRecCounter();
                        updateActRecCounter();
                        updateTxtInfoAct();
                        updateTxtInfoActSpec();
                    }
                }).start();
            }
        });
    }

    //If any scan was made or removed
    private void updateGaussians(){

        dbConnection.clearGaussianTable();
        for(int cellId = 1; cellId <= dbConnection.getNumberOfCells(); cellId++){
            List<WifiScan> scansOfCell = dbConnection.getScansOfCell(cellId);

            Map<String, Float> means = Utils.calculateMeans(scansOfCell);
            Map<String, Float> stddevs = Utils.calculateStdDevs(scansOfCell, means);

            for(String bssid : means.keySet())
                dbConnection.insertTableGaussian(cellId, bssid, means.get(bssid), stddevs.get(bssid));

        }
    }

    @Override
    public void onPause(){
        super.onPause();
        this.unregisterReceiver(receiver);
        if (updateGaussians)
            updateGaussians();
    }

    @Override
    public void onResume(){
        super.onResume();
        this.registerReceiver(receiver, filter);
        updateGaussians = false;
        updateTxtInfoScan();
        updateTxtInfoAct();
    }


    public ScanInfo getScanInfo() {
        return scanInfo;
    }

    public void setScanInfoToNull() {
        this.scanInfo = null;
    }


    public void incScanCounter () {
        scanCounter.set(scanCounter.get() + 1);
    }

    public void incActRecCounter () {
        actRecordCounter.set(actRecordCounter.get() + 1);
    }

    public void updateScanCounter(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtScanCount.setText(getString(R.string.scanCount) + scanCounter.get());
            }
        });
    }

    public void updateActRecCounter(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtActivityCount.setText(getString(R.string.actCount) + actRecordCounter.get());
            }
        });
    }


    private void updateTxtScanStatus(boolean scanSuccessful) {
        final String update;
        if (scanSuccessful)
            update = getString(R.string.scanStat) + getString(R.string.statusOk);
        else
            update = getString(R.string.scanStat) + getString(R.string.statusNotOk);
            runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    txtScanStatus.setText(update);
            }
        });
    }


    public void updateTxtInfoScan() {
        final Integer aux = dbConnection.getNumberOfScans();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtInfoScan.setText(getString(R.string.totNumScans) + aux.toString());
            }
        });
    }

    public void updateTxtInfoScanSpec() {
        final Integer aux = dbConnection.getNumberOfScansOnCell(Integer.parseInt(txtScanningCell.getText().toString()));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtInfoScanSpec.setText(getString(R.string.scanSpec) + aux.toString());
            }
        });
    }

    public void updateTxtInfoAct(){
        final Integer aux = dbConnection.getNumberOfActivityRecordings();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtInfoActivity.setText(getString(R.string.totNumActRec)+ aux.toString());
            }
        });
    }

    public void updateTxtInfoActSpec() {
        final Integer aux = dbConnection.getNumberOfActivityRecordingsOfActivity(selectedActivity);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtInfoActivitySpec.setText(getString(R.string.actSpec) + aux.toString());
            }
        });
    }

}
