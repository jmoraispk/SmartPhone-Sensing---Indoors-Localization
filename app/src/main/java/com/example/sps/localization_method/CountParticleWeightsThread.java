package com.example.sps.localization_method;

import android.app.Activity;

import com.example.sps.LocateMeActivity;
import com.example.sps.Utils;
import com.example.sps.map.WallPositions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountParticleWeightsThread extends Thread {


    private List<Particle> particles;
    private WallPositions walls;
    private LocateMeActivity src;
    private boolean running;

    public CountParticleWeightsThread(List<Particle> particles, WallPositions walls, LocateMeActivity src) {
        this.particles = particles;
        this.walls = walls;
        this.src = src;
        this.running = true;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {


        while (src.getLocalizationMethod() instanceof ContinuousLocalization && running) {
            List<Float> weightSumPerCell = new ArrayList<>(16);

            for (int i = 0; i < walls.getCells().size(); i++) {
                weightSumPerCell.add(0.0f);
            }

            int index;

            //Equate the weight of each particle
            for (Particle p : particles) {
                index = p.getCell();
                //to skip fake cells:
                if (index > 15) continue;

                weightSumPerCell.set(index, weightSumPerCell.get(index) + p.getWeight());
            }

            //Get cell with highest weight
            final int decision = Utils.argMax(weightSumPerCell);

            //Update text on UI
            src.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    src.setLocationTextForParticleFilter(decision+ 1);
                }
            });

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
