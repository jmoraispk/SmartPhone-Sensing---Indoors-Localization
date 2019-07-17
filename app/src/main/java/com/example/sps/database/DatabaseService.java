package com.example.sps.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.wifi.ScanResult;
import android.view.View;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.example.sps.Utils;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_collection.Direction;
import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiScan;

import java.util.LinkedList;
import java.util.List;

import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;
import static com.example.sps.LocateMeActivity.NUM_CELLS;

public class DatabaseService extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "SPSDataBase.db";
    public static final int DATABASE_VERSION = 8;


    public static final String SCAN_TABLE_NAME = "scan";
    public static final String SCAN_COLUMN_SCAN_ID = "scan_id";
    public static final String SCAN_COLUMN_CELL_ID = "cell_id";
    public static final String SCAN_COLUMN_DIR = "dir";
    public static final String SCAN_COLUMN_TIME = "time";


    public static final String SCAN_ITEM_TABLE_NAME = "scan_item";
    public static final String SCAN_ITEM_COLUMN_SCAN_ID = "scan_id";
    public static final String SCAN_ITEM_COLUMN_BSSID = "bssid";
    public static final String SCAN_ITEM_COLUMN_SSID = "ssid";
    public static final String SCAN_ITEM_COLUMN_RSSI = "rssi";

    public static final String GAUSSIANS_TABLE_NAME = "gaussians";
    public static final String GAUSSIANS_COLUMN_CELL_ID = "cell_id";
    public static final String GAUSSIANS_COLUMN_BSSID = "bssid";
    public static final String GAUSSIANS_COLUMN_MEAN = "mean";
    public static final String GAUSSIANS_COLUMN_STDDEV = "stddev";

    public static final String ACTIVITY_RECORDINGS_TABLE_NAME = "activity_recordings";
    public static final String ACTIVITY_RECORDINGS_COLUMN_ID = "record_id";
    public static final String ACTIVITY_RECORDINGS_COLUMN_ACTIVITY = "activity";
    public static final String ACTIVITY_RECORDINGS_COLUMN_SAMPLE_INDEX = "sample_index";
    public static final String ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_X = "sample_value_x";
    public static final String ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_Y = "sample_value_y";
    public static final String ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_Z = "sample_value_z";


    private static final String SQL_CREATE_TABLE_SCAN =
            "CREATE TABLE " + SCAN_TABLE_NAME + " (" +
                    SCAN_COLUMN_SCAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SCAN_COLUMN_CELL_ID + " INTEGER," +
                    SCAN_COLUMN_DIR + " TEXT," +
                    SCAN_COLUMN_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    private static final String SQL_DELETE_TABLE_SCAN =
            "DROP TABLE IF EXISTS " + SCAN_TABLE_NAME;




    private static final String SQL_CREATE_TABLE_SCAN_ITEM =
            "CREATE TABLE " + SCAN_ITEM_TABLE_NAME + " (" +
                    SCAN_ITEM_COLUMN_SCAN_ID + " INTEGER," +
                    SCAN_ITEM_COLUMN_BSSID + " TEXT," +
                    SCAN_ITEM_COLUMN_RSSI + " INTEGER," +
                    SCAN_ITEM_COLUMN_SSID + " TEXT," +
                    "FOREIGN KEY (" + SCAN_ITEM_COLUMN_SCAN_ID + ") REFERENCES " + SCAN_TABLE_NAME + " (" + SCAN_COLUMN_SCAN_ID + "))";

    private static final String SQL_DELETE_TABLE_SCAN_ITEM =
            "DROP TABLE IF EXISTS " + SCAN_ITEM_TABLE_NAME;



    private static final String SQL_CREATE_TABLE_GAUSSIANS =
            "CREATE TABLE " + GAUSSIANS_TABLE_NAME + " (" +
                    GAUSSIANS_COLUMN_BSSID + " TEXT NOT NULL," +
                    GAUSSIANS_COLUMN_CELL_ID + " INTEGER NOT NULL," +
                    GAUSSIANS_COLUMN_MEAN + " DECIMAL(3,2)," +
                    GAUSSIANS_COLUMN_STDDEV + " DECIMAL(3,2)," +
                    "PRIMARY KEY (" + GAUSSIANS_COLUMN_BSSID + ", " + GAUSSIANS_COLUMN_CELL_ID + ")"  + ")";

    private static final String SQL_DELETE_TABLE_GAUSSIANS =
            "DROP TABLE IF EXISTS " + GAUSSIANS_TABLE_NAME;


    private static final String SQL_CREATE_TABLE_ACTIVITY =
            "CREATE TABLE " + ACTIVITY_RECORDINGS_TABLE_NAME + " (" +
                    ACTIVITY_RECORDINGS_COLUMN_ID + " INTEGER NOT NULL," +
                    ACTIVITY_RECORDINGS_COLUMN_ACTIVITY + " TEXT NOT NULL," +
                    ACTIVITY_RECORDINGS_COLUMN_SAMPLE_INDEX + " INTEGER NOT NULL," +
                    ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_X + " REAL NOT NULL," +
                    ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_Y + " REAL NOT NULL," +
                    ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_Z + " REAL NOT NULL," +
                    "PRIMARY KEY (" + ACTIVITY_RECORDINGS_COLUMN_ID + ", " + ACTIVITY_RECORDINGS_COLUMN_SAMPLE_INDEX + ")"  + ")";

    private static final String SQL_DELETE_TABLE_ACTIVITY_RECORDINGS =
            "DROP TABLE IF EXISTS " + ACTIVITY_RECORDINGS_TABLE_NAME;

    SQLiteDatabase dbconnection;

    public DatabaseService(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.dbconnection = this.getWritableDatabase();

    }
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(SQL_CREATE_TABLE_SCAN_ITEM);
            db.execSQL(SQL_CREATE_TABLE_GAUSSIANS);
            db.execSQL(SQL_CREATE_TABLE_SCAN);
            db.execSQL(SQL_CREATE_TABLE_ACTIVITY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_TABLE_SCAN);
        db.execSQL(SQL_DELETE_TABLE_SCAN_ITEM);
        db.execSQL(SQL_DELETE_TABLE_GAUSSIANS);
        db.execSQL(SQL_DELETE_TABLE_ACTIVITY_RECORDINGS);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void insertTableScan(int cellID, Direction dir, List<ScanResult> scanResults) {
        ContentValues rowScanTable = new ContentValues();
        rowScanTable.put(SCAN_COLUMN_CELL_ID, cellID);
        rowScanTable.put(SCAN_COLUMN_DIR, dir.getDir());
        long id=  dbconnection.insert(SCAN_TABLE_NAME, null, rowScanTable);

        ContentValues rowScanItem = new ContentValues();

        for (ScanResult result : scanResults) {
            rowScanItem.clear();
            rowScanItem.put(SCAN_ITEM_COLUMN_SCAN_ID, id );
            rowScanItem.put(SCAN_ITEM_COLUMN_BSSID, result.BSSID);
            rowScanItem.put(SCAN_ITEM_COLUMN_SSID, result.SSID);
            rowScanItem.put(SCAN_ITEM_COLUMN_RSSI, result.level);
            dbconnection.insert(SCAN_ITEM_TABLE_NAME, null, rowScanItem);


        }
    }

    public List<List<WifiScan>> getRawReadings() {
        List<List<WifiScan>> data = new LinkedList<>();

        Cursor cellCursor = dbconnection.rawQuery("SELECT DISTINCT " + SCAN_COLUMN_CELL_ID + " FROM " + SCAN_TABLE_NAME, new String[]{});
        while(cellCursor.moveToNext()) {
            data.add(new LinkedList<WifiScan>());
        }
        if(data.size() == 0) return data;

        Cursor scanCursor;
        cellCursor.moveToFirst();
        do{
            int cellId = cellCursor.getInt(cellCursor.getColumnIndex(SCAN_COLUMN_CELL_ID));
            scanCursor = dbconnection.rawQuery("SELECT * FROM " + SCAN_TABLE_NAME + " WHERE " + SCAN_COLUMN_CELL_ID +" = " + cellId, new String[]{});
            while(scanCursor.moveToNext()) {
                int scanId = scanCursor.getInt(scanCursor.getColumnIndex(SCAN_COLUMN_SCAN_ID));
                Cursor resultsCursor = dbconnection.rawQuery("SELECT * FROM " + SCAN_ITEM_TABLE_NAME + " WHERE " + SCAN_ITEM_COLUMN_SCAN_ID + " = " + scanId, new String[]{});
                List<WifiReading> readings = new LinkedList<>();
                while (resultsCursor.moveToNext()) {
                    readings.add(new WifiReading(resultsCursor.getString(resultsCursor.getColumnIndex(SCAN_ITEM_COLUMN_BSSID)), resultsCursor.getInt(resultsCursor.getColumnIndex(SCAN_ITEM_COLUMN_RSSI))));
                }
                resultsCursor.close();
                data.get(cellId-1).add(new WifiScan(readings));
            }
            scanCursor.close();

        } while(cellCursor.moveToNext());
        cellCursor.close();

        return data;
    }

    //Updates a cell gaussians (probability mass functions of each AP)
    public void insertTableGaussian(int cellID, String bssid, float mean, float stddev) {
        ContentValues rowGaussian = new ContentValues();

        rowGaussian.put(GAUSSIANS_COLUMN_CELL_ID, cellID);
        rowGaussian.put(GAUSSIANS_COLUMN_BSSID, bssid);
        rowGaussian.put(GAUSSIANS_COLUMN_MEAN, mean);
        rowGaussian.put(GAUSSIANS_COLUMN_STDDEV, stddev);

        dbconnection.insert(GAUSSIANS_TABLE_NAME, null, rowGaussian);
    }

    //Get a gaussian from the Gaussians table, for a given cell and bssid
    public NormalDistribution getGaussian(int cellID, String bssid) {
        Cursor gaussianCursor = null;
        float mean = 0, stddev = 0;
        try {
            gaussianCursor = dbconnection.rawQuery("SELECT * FROM " + GAUSSIANS_TABLE_NAME + " WHERE " + GAUSSIANS_COLUMN_CELL_ID + " = " + cellID + " AND " + GAUSSIANS_COLUMN_BSSID + " = '" + bssid + "'", new String[]{});

            if (gaussianCursor.getCount() == 0) return null;

            gaussianCursor.moveToNext();
            mean = gaussianCursor.getFloat(gaussianCursor.getColumnIndex(GAUSSIANS_COLUMN_MEAN));
            stddev = gaussianCursor.getFloat(gaussianCursor.getColumnIndex(GAUSSIANS_COLUMN_STDDEV));
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            gaussianCursor.close();
        }
        return new NormalDistribution(mean, stddev + 0.01);
    }


    public List<WifiScan> getScansOfCell(int cellId) {
        List<WifiScan> data = new LinkedList<>();

        Cursor scanCursor = dbconnection.rawQuery("SELECT * FROM " + SCAN_TABLE_NAME + " WHERE " + SCAN_COLUMN_CELL_ID +" = " + cellId, new String[]{});
        while(scanCursor.moveToNext()) {
            int scanId = scanCursor.getInt(scanCursor.getColumnIndex(SCAN_COLUMN_SCAN_ID));
            Cursor resultsCursor = dbconnection.rawQuery("SELECT * FROM " + SCAN_ITEM_TABLE_NAME + " WHERE " + SCAN_ITEM_COLUMN_SCAN_ID + " = " + scanId, new String[]{});
            List<WifiReading> readings = new LinkedList<>();
            while (resultsCursor.moveToNext()) {
                readings.add(new WifiReading(resultsCursor.getString(resultsCursor.getColumnIndex(SCAN_ITEM_COLUMN_BSSID)), resultsCursor.getInt(resultsCursor.getColumnIndex(SCAN_ITEM_COLUMN_RSSI))));
            }
            resultsCursor.close();

            data.add(new WifiScan(readings));
        }
        scanCursor.close();

        return data;
    }

    public int getNumberOfCells() {

        return NUM_CELLS;

    }

    public void clearGaussianTable() {
        dbconnection.execSQL("DELETE FROM " + GAUSSIANS_TABLE_NAME);
    }

    public void insertRecording(List<FloatTriplet> recording, SubjectActivity activity) {

        int numActivityRecordings = getNumberOfActivityRecordings();

        for ( int i = 0; i < recording.size(); i++) {

            ContentValues sample = new ContentValues();

            sample.put(ACTIVITY_RECORDINGS_COLUMN_ID, numActivityRecordings + 1);
            sample.put(ACTIVITY_RECORDINGS_COLUMN_SAMPLE_INDEX, i);
            sample.put(ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_X, recording.get(i).getX());
            sample.put(ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_Y, recording.get(i).getY());
            sample.put(ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_Z, recording.get(i).getZ());
            sample.put(ACTIVITY_RECORDINGS_COLUMN_ACTIVITY, activity.name());

            dbconnection.insert(ACTIVITY_RECORDINGS_TABLE_NAME, null, sample);
        }
    }



    public List<List<FloatTriplet>> getActivityRecordings(SubjectActivity activity) {

        List<List<FloatTriplet>> listOfRecordings = new LinkedList<>();

        Cursor recordingCursor = null;
        Cursor sampleCursor = null;

        try {
            recordingCursor = dbconnection.rawQuery("SELECT DISTINCT " + ACTIVITY_RECORDINGS_COLUMN_ID + " FROM " + ACTIVITY_RECORDINGS_TABLE_NAME + " WHERE " + ACTIVITY_RECORDINGS_COLUMN_ACTIVITY + " = '" + activity.name() + "'", new String[]{});

            if (recordingCursor.getCount() == 0) return null;

            while (recordingCursor.moveToNext()) {
                List<FloatTriplet> recording = new LinkedList<>();

                sampleCursor = dbconnection.rawQuery("SELECT * FROM " + ACTIVITY_RECORDINGS_TABLE_NAME + " WHERE " + ACTIVITY_RECORDINGS_COLUMN_ID + " = " + recordingCursor.getInt(recordingCursor.getColumnIndex(ACTIVITY_RECORDINGS_COLUMN_ID)), new String[]{});

                while (sampleCursor.moveToNext()) {
                    recording.add(new FloatTriplet((float) sampleCursor.getDouble(sampleCursor.getColumnIndex(ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_X)), (float) sampleCursor.getDouble(sampleCursor.getColumnIndex(ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_Y)), (float) sampleCursor.getDouble(sampleCursor.getColumnIndex(ACTIVITY_RECORDINGS_COLUMN_SAMPLE_VALUE_Z))));
                }
                sampleCursor.close();
                listOfRecordings.add(recording);
            }


        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            recordingCursor.close();
            if (sampleCursor != null)
                sampleCursor.close();
        }
        return listOfRecordings;
    }


    public void deleteScanData() {
        dbconnection.execSQL("DELETE FROM " + SCAN_TABLE_NAME);
        dbconnection.execSQL("DELETE FROM " + SCAN_ITEM_TABLE_NAME);
        dbconnection.execSQL("DELETE FROM " + GAUSSIANS_TABLE_NAME);
    }

    public void deleteActivityData() {
        dbconnection.execSQL("DELETE FROM " + ACTIVITY_RECORDINGS_TABLE_NAME);
    }

    public void deleteLastActivity() {
        int toDeleteID = getNumberOfActivityRecordings();

        dbconnection.execSQL("DELETE FROM " + ACTIVITY_RECORDINGS_TABLE_NAME + " WHERE " + ACTIVITY_RECORDINGS_COLUMN_ID + " = " + toDeleteID, new String[]{});

    }

    public void deleteLastScan() {
        Cursor cursor = dbconnection.rawQuery("SELECT " + SCAN_COLUMN_SCAN_ID + " FROM " + SCAN_TABLE_NAME, new String[]{});

        if (cursor == null || cursor.getCount() == 0) {
            cursor.close();
            return ;
        }

        cursor.moveToLast();

        int ToDelete = cursor.getInt(cursor.getColumnIndex(SCAN_COLUMN_SCAN_ID));

        //deletes the scan with that ID
        dbconnection.execSQL("DELETE FROM " + SCAN_TABLE_NAME + " WHERE " + SCAN_COLUMN_SCAN_ID + " = " + ToDelete, new String[]{});

        //deletes all items from that scan
        dbconnection.execSQL("DELETE FROM " + SCAN_ITEM_TABLE_NAME + " WHERE " + SCAN_ITEM_COLUMN_SCAN_ID + " = " + ToDelete, new String[]{});

        cursor.close();
    }


    public int getNumberOfScans() {
        Cursor cursor = dbconnection.rawQuery("SELECT * FROM " + SCAN_TABLE_NAME, new String[]{});

        if (cursor == null || cursor.getCount() == 0) {
            cursor.close();
            return 0;
        }

        if (cursor.isBeforeFirst())
            cursor.moveToFirst();

        int count = 1;

        while (! cursor.isLast()) {
            cursor.moveToNext();
            count++;
        }
        cursor.close();
        return count;
    }

    public int getNumberOfScansOnCell(int cell) {
        Cursor cursor = dbconnection.rawQuery("SELECT * FROM " + SCAN_TABLE_NAME + " WHERE " + SCAN_COLUMN_CELL_ID + " = " + cell, new String[]{});

        if (cursor == null || cursor.getCount() == 0) {
            cursor.close();
            return 0;
        }

        if (cursor.isBeforeFirst())
            cursor.moveToFirst();

        int count = 1;

        while (! cursor.isLast()) {
            cursor.moveToNext();
            count++;
        }
        cursor.close();
        return count;
    }

    public int getNumberOfActivityRecordings() {
        Cursor cursor = dbconnection.rawQuery("SELECT DISTINCT " + ACTIVITY_RECORDINGS_COLUMN_ID + " FROM " + ACTIVITY_RECORDINGS_TABLE_NAME, new String[]{});

        if (cursor == null || cursor.getCount() == 0) {
            cursor.close();
            return 0;
        }

        if (cursor.isBeforeFirst())
            cursor.moveToFirst();

        int count = 1;

        while (! cursor.isLast()) {
            cursor.moveToNext();
            count++;
        }
        cursor.close();
        return count;
    }

    public int getNumberOfActivityRecordingsOfActivity(SubjectActivity activity) {
        Cursor cursor = dbconnection.rawQuery("SELECT COUNT(" + ACTIVITY_RECORDINGS_COLUMN_ID + "), " + ACTIVITY_RECORDINGS_COLUMN_ACTIVITY + " FROM " + ACTIVITY_RECORDINGS_TABLE_NAME + " WHERE " + ACTIVITY_RECORDINGS_COLUMN_ACTIVITY + " = '" + activity.name() + "' GROUP BY " + ACTIVITY_RECORDINGS_COLUMN_ID, new String[]{});

        if (cursor == null || cursor.getCount() == 0) {
            cursor.close();
            return 0;
        }

        int count = cursor.getCount();

        cursor.close();
        return count;
    }
}
