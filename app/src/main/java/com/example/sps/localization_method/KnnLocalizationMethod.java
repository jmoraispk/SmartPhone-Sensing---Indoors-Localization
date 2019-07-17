package com.example.sps.localization_method;

import android.app.Activity;
import android.net.wifi.ScanResult;
import android.provider.ContactsContract;

import com.example.sps.data_loader.WifiScan;
import com.example.sps.database.DatabaseService;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


public abstract class KnnLocalizationMethod implements LocalizationMethod {
    private static int NUM_NEIGHBOURS;





    @Override
    public float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities,  DatabaseService databaseService) {

        List<List<WifiScan>> data = databaseService.getRawReadings();
        if(data.size() == 0) return priorProbabilities;
        System.out.println("AAAAAAAAAAAAAAAAAAAA" + data.size());
        int numSamples = 0;
        for(List<WifiScan> list : data) numSamples += list.size();

        NUM_NEIGHBOURS = (int) Math.sqrt(numSamples);
        if (NUM_NEIGHBOURS % 2 == 0)
            NUM_NEIGHBOURS ++;


        List<Distance> distances = new LinkedList<>();
        for(int i = 0; i < data.size(); i++){

            for(WifiScan sample : data.get(i))
                distances.add(new Distance(i,calculateDistance(scan, sample)));

        }

        Collections.sort(distances, new Comparator<Distance>() {
            @Override
            public int compare(Distance distance, Distance t1) {
                return distance.getDistance() - t1.getDistance();
            }
        });

        List<Distance> closest = distances.subList(0, NUM_NEIGHBOURS);


        return votesDistribution(closest, databaseService);
    }

    // Returns the probability of being in each cell based on the cells of the closest k neighbours
    private float[] votesDistribution(List<Distance> closest, DatabaseService databaseService) {
        float[] voteDistribution = new float[databaseService.getNumberOfCells()];

        for(Distance d : closest)
            voteDistribution[d.getCell()]++;

        for (int i = 0; i < databaseService.getNumberOfCells(); i++)
            voteDistribution[i] = voteDistribution[i] / NUM_NEIGHBOURS;

        return voteDistribution;
    }

    public abstract int calculateDistance(List<ScanResult> scan, WifiScan sample);

    public int getNumNeighbours() { return NUM_NEIGHBOURS; }


    public class Distance {
        private int cell;
        private int distance;

        public Distance(int cell, int distance) {
            this.cell = cell;
            this.distance = distance;
        }

        public int getCell() {
            return cell;
        }

        public void setCell(int cell) {
            this.cell = cell;
        }

        public int getDistance() {
            return distance;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }

    }

    @Override
    public String getMiscInfo() {return "Num. Neighbours: " + NUM_NEIGHBOURS;}
}
